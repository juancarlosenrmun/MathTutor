import assert from 'node:assert/strict';
import { test } from 'node:test';
import { SessionClient } from '../../main/resources/static/visor/session.mjs';
import { mountAdministration } from '../../main/resources/static/visor/administration.mjs';
import { mountTeacherStatistics } from '../../main/resources/static/visor/teacher-statistics.mjs';
import { mountInterface } from '../../main/resources/static/visor/interface.mjs';
import { element, fixture } from './ui-fixture.mjs';

test('la solicitud de supresión incluye CSRF y JWT, y la exportación es una lectura', async () => {
  const calls = [];
  const client = new SessionClient(async (path, options) => {
    calls.push([path, options]);
    return path.endsWith('csrf-token') ? Response.json({ csrfToken: 'csrf' }) : new Response(null, { status: 204 });
  });
  client.token = 'jwt';
  await client.request('/api/privacy/export');
  await client.request('/api/privacy/account', { method: 'DELETE' });
  assert.deepEqual(calls.map(([path]) => path), ['/api/privacy/export', '/api/csrf-token', '/api/privacy/account']);
  assert.deepEqual(calls[0][1].headers, { Authorization: 'Bearer jwt' });
  assert.deepEqual(calls[2][1].headers, { Authorization: 'Bearer jwt', 'X-XSRF-TOKEN': 'csrf' });
  assert.equal(calls[2][1].method, 'DELETE');
});

test('si falla CSRF o el servidor rechaza la acción, el cliente devuelve un mensaje controlado', async () => {
  const withoutCsrf = new SessionClient(async path => {
    assert.equal(path, '/api/csrf-token');
    return new Response(null, { status: 403 });
  });
  await assert.rejects(withoutCsrf.request('/api/admin/users/2/deactivate', { method: 'PUT' }), /autorizar/);
  const conflict = new SessionClient(async path => path.endsWith('csrf-token')
    ? Response.json({ csrfToken: 'csrf' }) : new Response('detalle interno', { status: 409 }));
  await assert.rejects(conflict.request('/api/privacy/account', { method: 'DELETE' }),
    error => error.status === 409 && !error.message.includes('detalle interno'));
});

test('una descarga de datos pendiente se cancela si se cierra la sesión', async () => {
  const f = fixture();
  f.state.restore = true;
  f.host.location.hash = '#/cuenta';
  const button = f.nodes['#export-account'] = element();
  const fetch = f.host.fetch.bind(f.host);
  let finishExport, downloaded = false;
  f.host.fetch = (path, options) => path === '/api/privacy/export'
    ? new Promise(resolve => { finishExport = resolve; }) : fetch(path, options);
  f.host.URL = { createObjectURL() { downloaded = true; } };
  await mountInterface(f.doc, f.host, () => {}).ready;
  const download = button.listeners.click({ currentTarget: button });
  await f.nodes['#logout'].listeners.click();
  finishExport(Response.json({ profile: { email: 'a@test.es' } }));
  await download;
  assert.equal(downloaded, false);
  assert.equal(button.disabled, false);
});

function panel(ids, actions = []) {
  const nodes = Object.fromEntries(ids.map(id => [id, element()]));
  return { nodes, document: {
    querySelector: selector => nodes[selector],
    querySelectorAll: () => actions,
    createElement: tag => element({ tag }),
  } };
}

test('la administración excluye la cuenta propia y cancela la mutación cuando no hay confirmación', async () => {
  const change = element({ dataset: { adminAction: 'role' } });
  const { nodes, document } = panel(['#admin-status', '#admin-user', '#admin-role', '#admin-actions',
    '#admin-load', '#admin-previous', '#admin-next'], [change]);
  let requests = 0;
  const client = { profile: { id: 1, role: 'ADMIN', email: 'a@example.test' }, async request() {
    requests++;
    return Response.json([{ id: 1, role: 'ADMIN', active: true, email: 'a@example.test' },
      { id: 2, role: 'ALUMNO', active: false, email: 'b@example.test' }]);
  } };
  const administration = mountAdministration(document, { confirm: () => false }, client);
  administration.sync('espacio');
  await nodes['#admin-load'].listeners.click();
  assert.deepEqual(nodes['#admin-user'].children.map(item => item.textContent), ['b@example.test (desactivado)']);
  nodes['#admin-user'].value = '2';
  nodes['#admin-user'].listeners.change();
  assert.equal(change.disabled, true);
  await change.listeners.click();
  assert.equal(requests, 1);
  client.profile.role = 'ALUMNO';
  administration.sync('espacio');
  assert.deepEqual(nodes['#admin-user'].children, []);
});

test('el panel docente descarta respuestas que llegan tras salir del espacio', async () => {
  const { nodes, document } = panel(['#teacher-load', '#teacher-status', '#teacher-results', '.statistics-scroll']);
  let deliver;
  const client = { profile: { role: 'PROFESOR', email: 'p@example.test' }, request: () => new Promise(resolve => { deliver = resolve; }) };
  const statistics = mountTeacherStatistics(document, client);
  statistics.sync('espacio');
  const pending = nodes['#teacher-load'].listeners.click();
  statistics.sync('laboratorio');
  deliver(Response.json([{ module: 'funciones', started: 5 }]));
  await pending;
  assert.deepEqual(nodes['#teacher-results'].children, []);
  assert.equal(nodes['.statistics-scroll'].hidden, true);
});
