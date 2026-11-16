import test from 'node:test';
import assert from 'node:assert/strict';
import { SessionClient } from '../../main/resources/static/visor/session.mjs';

test('inicio de sesión con CSRF y permisos actualizados sin renovar el token', async () => {
  let role = 'ALUMNO';
  const calls = [];
  const client = new SessionClient(async (path, options) => {
    calls.push([path, options]);
    return { ok: true, json: async () => path.endsWith('csrf-token') ? { csrfToken: 'csrf' }
      : { data: path.endsWith('login') ? { accessToken: 'jwt' } : { role } } };
  });
  await client.login('alumno@test.es', 'password');
  assert.equal(calls[1][1].headers['X-XSRF-TOKEN'], 'csrf');
  assert.equal(await client.role(), 'ALUMNO');
  role = 'PROFESOR';
  assert.equal(await client.role(), 'PROFESOR');
  assert.equal(calls.at(-1)[1].headers.Authorization, 'Bearer jwt');
  client.clear();
  assert.equal(await client.role(), null);
});

test('errores de login y desactivación borran el acceso local', async () => {
  const client = new SessionClient(async path => ({ ok: path.endsWith('csrf-token'),
    json: async () => ({ csrfToken: 'csrf' }) }));
  await assert.rejects(client.login('a', 'b'), /Credenciales/);
  client.token = 'old';
  assert.equal(await client.role(), null);
  assert.equal(client.token, null);
});

test('una respuesta antigua no revoca una sesión nueva ni sustituye un rol reciente', async () => {
  const pending = [];
  const client = new SessionClient(() => new Promise(resolve => pending.push(resolve)));
  client.token = 'old';
  const oldRequest = client.role();
  client.token = 'new';
  pending.shift()({ ok: false });
  assert.equal(await oldRequest, undefined);
  assert.equal(client.token, 'new');
  const first = client.role();
  const second = client.role();
  pending.pop()({ ok: true, json: async () => ({ data: { role: 'PROFESOR' } }) });
  assert.equal(await second, 'PROFESOR');
  pending.pop()({ ok: true, json: async () => ({ data: { role: 'ALUMNO' } }) });
  assert.equal(await first, undefined);
});
