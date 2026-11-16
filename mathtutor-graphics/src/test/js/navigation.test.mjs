import test from 'node:test';
import assert from 'node:assert/strict';
import { viewFor, permissionDescription } from '../../main/resources/static/visor/interface.mjs';

test('rutas públicas, enlaces directos, desconocidos y cuenta protegida', () => {
  for (const view of ['inicio', 'acceso', 'registro', 'laboratorio', 'restringido']) {
    assert.equal(viewFor(`#/${view}`, false), view);
  }
  assert.equal(viewFor('#/cuenta', false), 'acceso');
  assert.equal(viewFor('#/cuenta', true), 'cuenta');
  assert.equal(viewFor('#/acceso', true), 'cuenta');
  assert.equal(viewFor('#/registro', true), 'cuenta');
  assert.equal(viewFor('#/desconocida', false), 'inicio');
  assert.equal(viewFor('', false), 'inicio');
});

test('permisos por rol sin ofrecer capacidades futuras ni elevar privilegios', () => {
  assert.match(permissionDescription('ALUMNO'), /propios/);
  assert.match(permissionDescription('PROFESOR'), /agregadas/);
  assert.match(permissionDescription('ADMIN'), /administración/);
  assert.match(permissionDescription('DESCONOCIDO'), /Sin permisos/);
});
