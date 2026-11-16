import test from 'node:test';
import assert from 'node:assert/strict';
import { viewFor, permissionDescription, workspaceFor } from '../../main/resources/static/visor/interface.mjs';

test('rutas públicas, enlaces directos, desconocidos y cuenta protegida', () => {
  for (const view of ['inicio', 'acceso', 'registro', 'laboratorio', 'restringido']) {
    assert.equal(viewFor(`#/${view}`, false), view);
  }
  assert.equal(viewFor('#/cuenta', false), 'acceso');
  assert.equal(viewFor('#/cuenta', true), 'cuenta');
  assert.equal(viewFor('#/espacio', false), 'acceso');
  assert.equal(viewFor('#/espacio', true), 'espacio');
  assert.equal(viewFor('#/acceso', true), 'espacio');
  assert.equal(viewFor('#/registro', true), 'espacio');
  assert.equal(viewFor('#/desconocida', false), 'inicio');
  assert.equal(viewFor('', false), 'inicio');
});

test('el espacio de cada rol ofrece únicamente las capacidades actuales de S1', () => {
  for (const [role, label] of [['ALUMNO', 'Alumno'], ['PROFESOR', 'Profesor'], ['ADMIN', 'Administrador']]) {
    const workspace = workspaceFor(role);
    assert.equal(workspace.label, label);
    assert.match(workspace.description, /laboratorio/);
    assert.doesNotMatch(workspace.description, /chat|historial|estadísticas|gestionar usuarios/i);
  }
  assert.equal(workspaceFor('DESCONOCIDO').label, 'Sin rol asignado');
});

test('permisos por rol sin ofrecer capacidades futuras ni elevar privilegios', () => {
  assert.match(permissionDescription('ALUMNO'), /propios/);
  assert.match(permissionDescription('PROFESOR'), /agregadas/);
  assert.match(permissionDescription('ADMIN'), /administración/);
  assert.match(permissionDescription('DESCONOCIDO'), /Sin permisos/);
});
