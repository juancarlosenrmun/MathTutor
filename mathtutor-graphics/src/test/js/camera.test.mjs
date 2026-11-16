import test from 'node:test';
import assert from 'node:assert/strict';
import { Camera } from '../../main/resources/static/visor/camera.mjs';

test('la cámara mira al origen y produce una matriz de proyección finita', () => {
  const camera = new Camera();
  const matrix = camera.matrix(1);
  assert.equal(matrix.length, 16);
  assert.ok([...matrix].every(Number.isFinite));
  // El origen queda delante de la cámara en coordenadas homogéneas.
  assert.ok(matrix[15] > 0);
  assert.notEqual(matrix[0], 0);
  assert.throws(() => camera.matrix(0), /aspecto/);
  assert.throws(() => camera.matrix(Infinity), /aspecto/);
});

test('órbita, zoom y desplazamiento cambian la vista y respetan límites', () => {
  const camera = new Camera();
  const original = [...camera.matrix(1)];
  camera.orbit(0.4, 100);
  assert.ok(camera.pitch < Math.PI / 2);
  camera.orbit(0, -100);
  assert.ok(camera.pitch > -Math.PI / 2);
  camera.zoom(1e6);
  assert.equal(camera.distance, 50);
  camera.zoom(-1e6);
  assert.equal(camera.distance, 1);
  camera.pan(0.4, -0.3);
  assert.notDeepEqual([...camera.matrix(1)], original);
  camera.reset();
  assert.deepEqual([...camera.matrix(1)], original);
});
