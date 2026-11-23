import test from 'node:test';
import assert from 'node:assert/strict';
import { linePlane, raySurface } from '../../main/resources/static/visor/intersections.mjs';
import { Camera } from '../../main/resources/static/visor/camera.mjs';

const triangle = z => ({vertices: [[0, 0, z], [1, 0, z], [0, 1, z]]});

test('selecciona la superficie visible más cercana, incluidas las aristas', () => {
  assert.deepEqual(raySurface([0.5, 0.5, 3], [0, 0, -1], [triangle(0), triangle(2)]), [0.5, 0.5, 2]);
  assert.equal(raySurface([0.75, 0.75, 3], [0, 0, -1], [triangle(0)]), null);
  assert.equal(raySurface([0, 0, 3], [0, 0, 1], [triangle(0)]), null);
  assert.equal(raySurface([0, 0, 3], [1, 0, 0], [triangle(0)]), null);
  assert.equal(raySurface([0, 0, 3], [0, 0, -1], [triangle(0)], 2), null);
});

test('la recta admite ambos sentidos y exige una intersección única', () => {
  assert.deepEqual(linePlane([1, 2, 3], [0, 0, 1], [0, 0, 0], [0, 0, 1]), [1, 2, 0]);
  assert.equal(linePlane([1, 2, 3], [1, 0, 0], [0, 0, 0], [0, 0, 1]), null);
  assert.equal(linePlane([1, 2, 0], [1, 0, 0], [0, 0, 0], [0, 0, 1]), null);
});

test('la recta de visión se proyecta en el píxel elegido en ambas proyecciones', () => {
  const camera = new Camera();
  camera.pan(0.4, 0.2);
  camera.zoom(200);
  for (const span of [null, [9, 2]]) {
    camera.orthographicSpan = span;
    const {origin, direction} = camera.ray(0.36, -0.57, 1.7);
    const point = origin.map((value, i) => value + 5 * direction[i]);
    const matrix = camera.matrix(1.7);
    const clip = [0, 1, 3].map(row => point.reduce((sum, value, i) => sum + matrix[4 * i + row] * value, matrix[12 + row]));
    assert.ok(Math.abs(clip[0] / clip[2] - 0.36) < 1e-5);
    assert.ok(Math.abs(clip[1] / clip[2] + 0.57) < 1e-5);
  }
});
