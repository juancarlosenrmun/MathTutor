import test from 'node:test';
import assert from 'node:assert/strict';
import { surface, mountSurfaceControls } from '../../main/resources/static/visor/surfaces.mjs';

test('UC-29 triangula un plano y calcula normales unitarias orientadas hacia arriba', () => {
  const mesh = surface('x+y', -1, 1, -1, 1, 2);
  assert.equal(mesh.triangles.length, 8);
  for (const triangle of mesh.triangles) {
    assert.ok(Math.abs(Math.hypot(...triangle.normal) - 1) < 1e-12);
    assert.ok(triangle.normal[2] > 0);
  }
});

test('UC-29 no crea triángulos sobre singularidades ni coordenadas no finitas', () => {
  const mesh = surface('1/x', -1, 1, -1, 1, 4);
  assert.ok(mesh.triangles.length < 32);
  assert.ok(mesh.triangles.every(t => t.vertices.flat().every(Number.isFinite)));
});

test('UC-29 no une vértices a través de una asíntota interior a una celda', () => {
  const mesh = surface('1/(x-0.2)', 0, 1, -1, 1, 4);
  assert.ok(mesh.triangles.every(t => !t.vertices.some(v => v[0] === 0)));
  assert.equal(mesh.triangles.length, 24);
});

test('UC-29 rechaza dominios, resoluciones y expresiones no admitidas', () => {
  for (const args of [['x', 1, 1, -1, 1, 4], ['x', -1, 1, -1, 1, 1],
    ['x', -1, 1, -1, 1, 129], ['alert(1)', -1, 1, -1, 1, 4]]) {
    assert.throws(() => surface(...args));
  }
});

test('UC-29 el formulario dibuja la malla y anuncia el número de triángulos', () => {
  const nodes = new Map();
  for (const [id,value] of Object.entries({'surface-function':'x+y','surface-x-min':'-1',
    'surface-x-max':'1','surface-y-min':'-1','surface-y-max':'1'})) nodes.set(id,{value});
  nodes.set('surface-result',{textContent:''});
  nodes.set('surface-draw',{disabled:false});
  nodes.set('surface-resolution',{value:'2'});
  nodes.set('surface-form',{addEventListener(_type,fn){this.submit=fn;}});
  let drawn;
  mountSurfaceControls({querySelector:selector=>nodes.get(selector.slice(1))},{setSurface:mesh=>{drawn=mesh;}});
  nodes.get('surface-form').submit({preventDefault(){}});
  assert.equal(drawn.triangles.length, 8);
  assert.match(nodes.get('surface-result').textContent,/triángulos/);
  nodes.get('surface-resolution').value = '0';
  nodes.get('surface-form').submit({preventDefault(){}});
  assert.match(nodes.get('surface-result').textContent,/inválidos/);
});
