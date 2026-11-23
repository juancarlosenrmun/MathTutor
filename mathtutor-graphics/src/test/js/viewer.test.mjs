import test from 'node:test';
import assert from 'node:assert/strict';
import { WebGLViewer } from '../../main/resources/static/visor/viewer.mjs';
import { curve } from '../../main/resources/static/visor/curves.mjs';
import { surface as meshSurface } from '../../main/resources/static/visor/surfaces.mjs';
import { fakeGl } from './fake-gl.mjs';

function surface(gl = fakeGl()) {
  const listeners = new Map();
  const canvas = { width: 0, height: 0, clientWidth: 800, clientHeight: 400,
    getContext: kind => kind === 'webgl2' ? gl : null,
    addEventListener: (name, fn) => listeners.set(name, fn),
    removeEventListener: name => listeners.delete(name),
    setPointerCapture() {},
    getBoundingClientRect: () => ({left: 100, top: 50, width: 800, height: 400}),
  };
  const host = { devicePixelRatio: 2, addEventListener() {}, removeEventListener() {} };
  return { canvas, listeners, host, status: { textContent: '' } };
}

test('EN-07 Entrar selecciona el centro del visor sin usar el puntero', () => {
  const { canvas, status, host, listeners } = surface();
  const viewer = new WebGLViewer(canvas, status, host);
  viewer.selectionMode = { value: 'surface' };
  viewer.setSurface(meshSurface('0', -1, 1, -1, 1, 4));
  let handled = false;
  listeners.get('keydown')({ key: 'Enter', preventDefault() { handled = true; } });
  assert.equal(handled, true);
  assert.ok(viewer.selection.every(value => Math.abs(value) < 1e-9));
  assert.match(status.textContent, /Punto seleccionado/);
  viewer.dispose();
});

test('UC-32 distingue pulsación, arrastre, cancelación y selecciona el objeto indicado', () => {
  const {canvas, status, host, listeners} = surface();
  const viewer = new WebGLViewer(canvas, status, host);
  viewer.selectionMode = {value: 'surface'};
  viewer.setSurface(meshSurface('0', -1, 1, -1, 1, 4, [0, 0]));
  const emit = (name, x, y, button = 0) => listeners.get(name)({pointerId: 1, clientX: x, clientY: y, button});
  const click = (x, y) => { emit('pointerdown', x, y); emit('pointerup', x, y); };
  emit('pointerdown', 500, 250, 2);
  emit('pointerup', 500, 250, 2);
  assert.equal(viewer.selection, null);
  emit('pointerdown', 500, 250);
  emit('pointercancel');
  emit('pointerup', 500, 250);
  assert.equal(viewer.selection, null);

  click(500, 250);
  assert.match(status.textContent, /Punto seleccionado/);
  assert.ok(viewer.selection.every(value => Math.abs(value) < 1e-9));
  assert.ok(viewer.pipeline.selectionIndex > 6);
  const previous = viewer.selection;
  emit('pointerdown', 500, 250);
  emit('pointermove', 530, 260);
  emit('pointerup', 530, 260);
  assert.equal(viewer.selection, previous);
  click(50, 50);
  assert.equal(viewer.selection, previous);

  viewer.selectionMode.value = 'plane';
  click(500, 250);
  assert.match(status.textContent, /Punto seleccionado/);
  click(100, 50);
  assert.match(status.textContent, /No hay intersección/);
  assert.equal(viewer.pipeline.selectionIndex, null);
  viewer.setSurface(meshSurface('0', -1, 1, -1, 1, 4));
  click(500, 250);
  assert.match(status.textContent, /No hay intersección/);
  viewer.dispose();
});
test('eliminar la geometría privada impide restaurarla después de perder WebGL',()=>{
  const state=surface();const viewer=new WebGLViewer(state.canvas,state.status,state.host);
  viewer.setGraph(curve('x',-1,1));viewer.clear();assert.equal(viewer.graph,null);assert.equal(viewer.pipeline.lineCount,0);
  state.listeners.get('webglcontextlost')({preventDefault(){}});viewer.clear();state.listeners.get('webglcontextrestored')();
  assert.equal(viewer.graph,null);assert.equal(viewer.surface,null);viewer.dispose();
});

test('UC-29 conserva la superficie al perder y recuperar WebGL2', () => {
  const state = surface();
  const viewer = new WebGLViewer(state.canvas, state.status, state.host);
  viewer.setSurface(meshSurface('x+y', -1, 1, -1, 1, 2));
  assert.equal(viewer.pipeline.surfaceCount, 24);
  state.listeners.get('webglcontextlost')({preventDefault(){}});
  state.listeners.get('webglcontextrestored')();
  assert.equal(viewer.pipeline.surfaceCount, 24);
  viewer.dispose();
});

test('UC-28 encuadra alturas y dominios grandes en escritorio y móvil y recupera la gráfica', () => {
  const state = surface();
  const viewer = new WebGLViewer(state.canvas,state.status,state.host);
  for (const graph of [curve('100',-1,1),curve('0',-10000,10000)]) {
    viewer.setGraph(graph);
    for (const aspect of [2,.4]) {
      const m = viewer.camera.matrix(aspect);
      for (const p of graph.points.filter(p => p.y !== null)) {
        const w = m[3]*p.x + m[7]*p.y + m[15];
        assert.ok(Math.abs((m[0]*p.x + m[4]*p.y + m[12])/w) <= 1);
        assert.ok(Math.abs((m[1]*p.x + m[5]*p.y + m[13])/w) <= 1);
      }
    }
  }
  state.listeners.get('webglcontextlost')({preventDefault(){}});
  viewer.setGraph(curve('x^2',-1,1));
  state.listeners.get('webglcontextrestored')();
  assert.ok(viewer.pipeline.areaCount > 0);
  viewer.command('reset'); assert.ok(viewer.camera.orthographicSpan);
  viewer.dispose();
});

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
