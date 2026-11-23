import test from 'node:test';
import assert from 'node:assert/strict';
import { viewFor } from '../../main/resources/static/visor/interface.mjs';
import { SessionClient } from '../../main/resources/static/visor/session.mjs';

const token = 'A'.repeat(43);

test('UC-02 abre el formulario con un enlace válido y permite pedir otro si falta el token', () => {
  assert.equal(viewFor('#/recuperar', false), 'recuperar');
  assert.equal(viewFor(`#/restablecer/${token}`, false), 'restablecer');
  assert.equal(viewFor(`#/restablecer/${token}`, true), 'restablecer');
  for (const link of ['#/restablecer', '#/restablecer/', '#/restablecer/corto']) {
    assert.equal(viewFor(link, false), 'recuperar');
  }
});

test('UC-02 protege ambas peticiones y descarta la sesión local solo al confirmar el cambio', async () => {
  const requests = [];
  const client = new SessionClient(async (url, options) => {
    if (url === '/api/csrf-token') return Response.json({csrfToken: 'csrf'});
    requests.push([url, options.headers['X-XSRF-TOKEN'], JSON.parse(options.body)]);
    return Response.json({success: true});
  });
  client.token = 'sesión';
  await client.requestPasswordReset('alumna@example.org');
  assert.equal(client.token, 'sesión');
  await client.confirmPasswordReset(token, 'ClaveNueva123!');
  assert.equal(client.token, null);
  assert.deepEqual(requests, [
    ['/api/auth/password-reset/request', 'csrf', {email: 'alumna@example.org'}],
    ['/api/auth/password-reset/confirm', 'csrf', {token, password: 'ClaveNueva123!'}],
  ]);
});

test('UC-02 muestra la política conocida y oculta detalles internos de los errores', async () => {
  let reply;
  const client = new SessionClient(async url => url === '/api/csrf-token' ? Response.json({csrfToken: 'csrf'}) : reply);
  reply = Response.json({error: 'detalle interno'}, {status: 503});
  await assert.rejects(client.requestPasswordReset('alumna@example.org'), /No se pudo contactar/);
  reply = Response.json({error: 'La contraseña debe contener al menos un dígito'}, {status: 400});
  await assert.rejects(client.confirmPasswordReset(token, 'SinDigitos!!'), /al menos un dígito/);
  reply = Response.json({error: 'detalle interno'}, {status: 400});
  await assert.rejects(client.confirmPasswordReset(token, 'ClaveNueva123!'), /enlace no es válido/);
});
