import test from 'node:test';
import assert from 'node:assert/strict';
import { curve } from '../../main/resources/static/visor/curves.mjs';
import { surface } from '../../main/resources/static/visor/surfaces.mjs';
import { WebGLViewer } from '../../main/resources/static/visor/viewer.mjs';
import { fakeGl } from './fake-gl.mjs';

const approximately = (actual, expected) => assert.ok(Math.abs(actual - expected) < 1e-7);

test('la secante conserva su apoyo y converge a la derivada, también en un extremo', () => {
  for (const x of [0, 2]) {
    const graph = curve('x^2', -2, 2, 32, x);
    for (const progress of [0, 0.5, 0.99]) {
      const secant = graph.secantAt(progress);
      const [[a, ya], [b, yb]] = secant.lines;
      assert.ok(secant.point[0] >= -2 && secant.point[0] <= 2);
      approximately(secant.point[1], secant.point[0] ** 2);
      approximately((yb - ya) / (b - a), secant.point[0] + x);
      approximately(ya, x ** 2 + (a - x) * secant.slope);
    }
    assert.deepEqual(graph.secantAt(1).lines, graph.tangent.lines);
    assert.ok(Math.abs(graph.secantAt(0.99).slope - 2 * x) < 0.011);
  }
});

test('acorta la trayectoria antes de una discontinuidad y omite intervalos sin secante representable', () => {
  const graph = curve('1/x', -2, 2, 64, 0.25);
  assert.ok(graph.secantAt(0).point[0] > 0);
  assert.equal(curve('x^2', -1, 1).secantAt, undefined);
  assert.equal(curve('0', 0, Number.MIN_VALUE, 16, 0).secantAt, undefined);
});

function fixture(reducedMotion = false) {
  const frames = new Map();
  const events = new Map();
  let next = 0;
  const gl = fakeGl({ POINTS: 10 });
  const host = {
    addEventListener() {}, removeEventListener() {},
    matchMedia: () => ({ matches: reducedMotion }),
    requestAnimationFrame(callback) { frames.set(++next, callback); return next; },
    cancelAnimationFrame(id) { frames.delete(id); },
  };
  const canvas = {
    clientWidth: 800, clientHeight: 400, getContext: () => gl,
    addEventListener(name, callback) { events.set(name, callback); },
    removeEventListener(name) { events.delete(name); }, setPointerCapture() {},
  };
  const viewer = new WebGLViewer(canvas, { textContent: '' }, host);
  const changes = [];
  viewer.onAnimationChange = (kind, status) => changes.push([kind, status]);
  const tick = time => {
    const [id, callback] = frames.entries().next().value;
    frames.delete(id);
    callback(time);
  };
  return { viewer, frames, tick, changes, events, gl };
}

test('reproduce la secante con un búfer acotado y termina exactamente en la tangente', () => {
  const { viewer, frames, tick, changes, gl } = fixture();
  viewer.playAnimation('secant');
  assert.equal(frames.size, 0);
  const graph = curve('x^2', -2, 2, 32, 0);
  viewer.setGraph(graph);
  const uploads = gl.calls.filter(([call]) => call === 'bufferData').length;
  viewer.playAnimation('secant');
  assert.equal(frames.size, 1);
  tick(100);
  const before = gl.calls.findLast(([call]) => call === 'bufferSubData').at(-1);
  tick(1100);
  const middle = gl.calls.findLast(([call]) => call === 'bufferSubData').at(-1);
  assert.notDeepEqual(before, middle);
  assert.notEqual(viewer.pipeline.selectionIndex, null);
  tick(2200);
  assert.equal(frames.size, 0);
  assert.equal(viewer.pipeline.selectionIndex, null);
  assert.equal(gl.calls.filter(([call]) => call === 'bufferData').length, uploads);
  assert.deepEqual(changes.slice(-2), [['secant', 'playing'], ['secant', 'finished']]);
  viewer.dispose();
});

test('la cámara vuelve a su vista inicial y la interacción manual interrumpe el recorrido', () => {
  const { viewer, frames, tick, changes, events } = fixture();
  viewer.setSurface(surface('x+y', -1, 1, -1, 1, 4));
  const initial = viewer.camera.matrix(2);
  viewer.playAnimation('camera');
  tick(10);
  tick(1010);
  assert.notDeepEqual(viewer.camera.matrix(2), initial);
  tick(4010);
  assert.deepEqual(viewer.camera.matrix(2), initial);
  viewer.playAnimation('camera');
  events.get('wheel')({ deltaY: 100, preventDefault() {} });
  assert.equal(frames.size, 0);
  assert.deepEqual(changes.at(-1), ['camera', 'stopped']);
  viewer.playAnimation('camera');
  events.get('webglcontextlost')({ preventDefault() {} });
  assert.equal(frames.size, 0);
  viewer.playAnimation('camera');
  assert.equal(frames.size, 0);
  events.get('webglcontextrestored')();
  viewer.playAnimation('camera');
  viewer.dispose();
  assert.equal(frames.size, 0);
});

test('la cámara conserva el plano de una curva y cambia el encuadre durante el recorrido', () => {
  const { viewer, tick, frames } = fixture();
  viewer.setGraph(curve('x', -10, 10));
  const original = viewer.camera.matrix(2);
  viewer.playAnimation('camera');
  assert.equal(frames.size, 1);
  tick(0);
  tick(2000);
  assert.equal(viewer.camera.yaw, 0);
  assert.equal(viewer.camera.pitch, 0);
  assert.notDeepEqual(viewer.camera.matrix(2), original);
  tick(4000);
  assert.deepEqual(viewer.camera.matrix(2), original);
  viewer.dispose();
});

test('con movimiento reducido ofrece el resultado sin programar fotogramas', () => {
  const { viewer, frames, changes } = fixture(true);
  viewer.setGraph(curve('x^2', -2, 2, 32, 0));
  viewer.playAnimation('secant');
  assert.deepEqual(changes.at(-1), ['secant', 'reduced']);
  viewer.setSurface(surface('x+y', -1, 1, -1, 1, 4));
  const initial = viewer.camera.matrix(2);
  viewer.playAnimation('camera');
  assert.equal(frames.size, 0);
  assert.notDeepEqual(viewer.camera.matrix(2), initial);
  assert.deepEqual(changes.at(-1), ['camera', 'reduced']);
  viewer.dispose();
});
