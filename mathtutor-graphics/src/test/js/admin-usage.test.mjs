import assert from 'node:assert/strict';
import { test } from 'node:test';
import { mountAdministration } from '../../main/resources/static/visor/administration.mjs';
import { element } from './ui-fixture.mjs';

const ids = ['status', 'user', 'role', 'actions', 'load', 'previous', 'next',
  ...['sessions', 'topics'].flatMap(panel =>
    ['status', 'results', 'table', 'load', 'previous', 'next'].map(id => panel + '-' + id))];

function administration(request) {
  const nodes = Object.fromEntries(ids.map(id => [id, element()]));
  const document = {
    querySelector: selector => nodes[selector.replace('#admin-', '')],
    querySelectorAll: () => [],
    createElement: () => element(),
  };
  const client = { profile: { id: 1, role: 'ADMIN', email: 'admin@example.test' }, request };
  const admin = mountAdministration(document, {}, client);
  admin.sync('espacio');
  return { nodes, admin };
}

test('consulta y pagina el uso por tema sin seleccionar un usuario', async () => {
  const paths = [];
  const { nodes } = administration(async path => {
    paths.push(path);
    return Response.json(paths.length === 1
      ? { items: [{ module: 'funciones', topic: 'límites', sessions: 2, turns: 3 }], hasNext: true }
      : { items: [], hasNext: false });
  });
  await nodes['topics-load'].listeners.click();
  assert.deepEqual(nodes['topics-results'].children[0].children.map(cell => cell.textContent),
    ['funciones', 'límites', '2', '3']);
  assert.equal(nodes['topics-previous'].disabled, true);
  assert.equal(nodes['topics-next'].disabled, false);
  await nodes['topics-next'].listeners.click();
  assert.deepEqual(paths, ['/api/admin/usage/topics?page=0&size=20', '/api/admin/usage/topics?page=1&size=20']);
  assert.equal(nodes['topics-table'].hidden, true);
  await nodes['topics-previous'].listeners.click();
  assert.equal(paths.at(-1), paths[0]);
});

test('consulta solo el perfil elegido y descarta su respuesta al cambiar de perfil', async () => {
  const pending = new Map();
  const { nodes, admin } = administration(async path => {
    if (path === '/api/admin/users?page=0&size=20') {
      return Response.json([{ id: 2, role: 'ALUMNO', active: false, email: 'a@example.test' },
        { id: 3, role: 'ALUMNO', active: true, email: 'b@example.test' }]);
    }
    assert.match(path, /^\/api\/admin\/users\/[23]\/sessions\?page=0&size=20$/);
    return new Promise(resolve => { pending.set(path, resolve); });
  });
  assert.equal(nodes['sessions-load'].disabled, true);
  await nodes.load.listeners.click();
  nodes.user.value = '2';
  nodes.user.listeners.change();
  const previous = nodes['sessions-load'].listeners.click();
  nodes.user.value = '3';
  nodes.user.listeners.change();
  const current = nodes['sessions-load'].listeners.click();
  pending.get('/api/admin/users/3/sessions?page=0&size=20')(Response.json({ items: [{ id: 5, module: null, status: 'CERRADA',
    startedAt: '2026-01-01T12:00:00Z', lastActivityAt: '2026-01-02T12:00:00Z' }], hasNext: false }));
  await current;
  assert.deepEqual(nodes['sessions-results'].children[0].children.slice(0, 3).map(cell => cell.textContent),
    ['5', 'Sin módulo', 'Cerrada']);
  pending.get('/api/admin/users/2/sessions?page=0&size=20')(Response.json({ items: [], hasNext: false }));
  await previous;
  assert.equal(nodes['sessions-results'].children[0].children[0].textContent, '5');
  admin.sync('laboratorio');
  assert.deepEqual(nodes['sessions-results'].children, []);
  assert.equal(nodes['sessions-table'].hidden, true);
});

test('informa de errores de consulta y permite reintentar', async () => {
  const { nodes } = administration(async () => { throw new Error('Error de consulta'); });
  await nodes['topics-load'].listeners.click();
  assert.equal(nodes['topics-status'].textContent, 'Error de consulta');
  assert.equal(nodes['topics-load'].disabled, false);
});
