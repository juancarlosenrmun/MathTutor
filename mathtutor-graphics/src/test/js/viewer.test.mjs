import test from 'node:test';
import assert from 'node:assert/strict';
import { WebGLViewer } from '../../main/resources/static/visor/viewer.mjs';
import { fakeGl } from './fake-gl.mjs';

function surface(gl = fakeGl()) {
  const listeners = new Map();
  const canvas = { width: 0, height: 0, clientWidth: 800, clientHeight: 400,
    getContext: kind => kind === 'webgl2' ? gl : null,
    addEventListener: (name, fn) => listeners.set(name, fn),
    removeEventListener: name => listeners.delete(name),
    setPointerCapture() {},
  };
  const host = { devicePixelRatio: 2, addEventListener() {}, removeEventListener() {} };
  return { canvas, listeners, host, status: { textContent: '' } };
}

test('el visor redimensiona y responde a teclado, rueda y arrastre', () => {
  const { canvas, listeners, host, status } = surface();
  const viewer = new WebGLViewer(canvas, status, host);
  assert.equal(canvas.width, 1600);
  assert.equal(canvas.height, 800);
  assert.match(status.textContent, /listo/);
  const before = [...viewer.camera.matrix(2)];
  const event = values => ({ preventDefault() {}, ...values });
  for (const key of ['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', '+', '-', 'Home', 'x']) {
    listeners.get('keydown')(event({ key, shiftKey: false }));
    listeners.get('keydown')(event({ key, shiftKey: true }));
  }
  listeners.get('pointermove')(event({ pointerId: 1, clientX: 1, clientY: 2 }));
  listeners.get('pointerdown')(event({ pointerId: 1, clientX: 0, clientY: 0 }));
  listeners.get('pointermove')(event({ pointerId: 2, clientX: 3, clientY: 2 }));
  listeners.get('pointermove')(event({ pointerId: 1, clientX: 30, clientY: 10, shiftKey: false }));
  listeners.get('pointermove')(event({ pointerId: 1, clientX: 40, clientY: 20, shiftKey: true }));
  listeners.get('pointerup')();
  listeners.get('pointercancel')();
  listeners.get('wheel')(event({ deltaY: 100 }));
  assert.notDeepEqual([...viewer.camera.matrix(2)], before);
  viewer.command('reset');
  assert.deepEqual([...viewer.camera.matrix(2)], before);
  viewer.command('in');
  viewer.command('out');
  viewer.dispose();
  assert.equal(listeners.size, 0);
});

test('pérdida de contexto, restauración y ausencia de WebGL2 tienen salida controlada', () => {
  const state = surface();
  const viewer = new WebGLViewer(state.canvas, state.status, state.host);
  state.listeners.get('webglcontextlost')({ preventDefault() {} });
  viewer.render();
  assert.match(state.status.textContent, /perdido/);
  state.listeners.get('webglcontextrestored')();
  assert.match(state.status.textContent, /listo/);
  state.canvas.clientWidth = 0;
  state.canvas.clientHeight = 0;
  state.host.devicePixelRatio = undefined;
  viewer.render();
  assert.equal(state.canvas.width, 1);
  viewer.dispose();
  const missing = surface(null);
  assert.throws(() => new WebGLViewer(missing.canvas, missing.status, missing.host), /WebGL 2/);
});
