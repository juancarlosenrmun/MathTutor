import test from 'node:test';
import assert from 'node:assert/strict';
import { SessionClient } from '../../main/resources/static/visor/session.mjs';

function fixture(status = 200) {
  const calls = [];
  const client = new SessionClient(async (path, options) => {
    calls.push([path, options]);
    return { ok: path.endsWith('csrf-token') || status < 400, status, json: async () => path.endsWith('csrf-token')
      ? { csrfToken: 'csrf' } : { data: { accessToken: 'nuevo', role: 'ALUMNO', email: 'a@test.es' } } };
  });
  return { client, calls };
}

test('registro público envía solo ALUMNO y conserva el token en memoria', async () => {
  const { client, calls } = fixture();
  await client.register('a@test.es', 'UnaClaveSegura12!');
  assert.deepEqual(JSON.parse(calls[1][1].body), { email: 'a@test.es', password: 'UnaClaveSegura12!', role: 'ALUMNO' });
  assert.equal(calls[1][0], '/api/auth/register');
  assert.equal(client.token, 'nuevo');
});

test('restaurar renueva con cookie y CSRF; salir revoca y borra el token', async () => {
  const { client, calls } = fixture();
  await client.restore();
  assert.equal(client.token, 'nuevo');
  assert.equal(calls[1][0], '/api/auth/refresh');
  assert.equal(calls[1][1].credentials, 'same-origin');
  await client.logout();
  assert.equal(calls[3][0], '/api/auth/logout');
  assert.equal(calls[3][1].headers.Authorization, 'Bearer nuevo');
  assert.equal(client.token, null);
});

test('registro inválido, límite y servidor no muestran mensajes internos', async () => {
  for (const [status, message] of [[400, /registro/], [429, /Demasiados/], [500, /servidor/]]) {
    const { client } = fixture(status);
    await assert.rejects(client.register('a', 'b'), message);
    assert.equal(client.token, null);
  }
});

test('refresh sin cookie es anónimo; error de red no se confunde con sesión caducada', async () => {
  const { client } = fixture(401);
  assert.equal(await client.restore(), null);
  const offline = new SessionClient(async () => { throw new Error('offline'); });
  await assert.rejects(offline.restore(), /offline/);
});

test('403 es acceso restringido; 5xx preserva el token; 401 permite renovar', async () => {
  for (const status of [403, 500]) {
    const { client } = fixture(status);
    client.token = 'jwt';
    await assert.rejects(client.role(), status === 403 ? /permiso/ : /servidor/);
    assert.equal(client.token, status === 403 ? null : 'jwt');
  }
  let reads = 0;
  const client = new SessionClient(async path => ({ ok: !path.endsWith('/me') || ++reads > 1,
    status: path.endsWith('/me') && reads === 1 ? 401 : 200,
    json: async () => path.endsWith('csrf-token') ? { csrfToken: 'csrf' }
      : { data: { accessToken: 'renovado', role: 'ALUMNO' } } }));
  client.token = 'viejo';
  assert.equal(await client.role(), 'ALUMNO');
  assert.equal(client.token, 'renovado');
});

test('salir invalida una renovación pendiente', async () => {
  let resolve;
  const client = new SessionClient(async path => path.endsWith('csrf-token')
    ? { ok: true, json: async () => ({ csrfToken: 'csrf' }) }
    : new Promise(done => { resolve = done; }));
  const pending = client.restore();
  await new Promise(done => setImmediate(done));
  client.clear();
  resolve({ ok: true, json: async () => ({ data: { accessToken: 'obsoleto' } }) });
  await pending;
  assert.equal(client.token, null);
});
