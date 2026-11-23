import test from 'node:test';
import assert from 'node:assert/strict';
import { mount } from '../../main/resources/static/visor/app.mjs';
import { fakeGl } from './fake-gl.mjs';
import { element, fixture } from './ui-fixture.mjs';

test('la página monta WebGL, conecta cámara, cambia de vista y libera recursos', async () => {
  const f = fixture();
  f.nodes.canvas = element({ clientWidth: 600, clientHeight: 400, getContext: () => fakeGl() });
  const button = element({ dataset: { camera: 'in' } });
  const original = f.doc.querySelectorAll;
  f.doc.querySelectorAll = selector => selector === '[data-camera]' ? [button] : original(selector);
  const app = mount(f.doc, f.host);
  await app.ready;
  assert.match(f.nodes['#graphic-status'].textContent, /Visor listo/);
  button.listeners.click();
  f.host.location.hash = '#/laboratorio';
  f.host.listeners.hashchange();
  // El doble conserva solo el último listener; comprobar también limpieza del visor directamente.
  const uiHide = f.host.listeners.pagehide;
  uiHide({ persisted: true });
  assert.equal(f.host.cleared, undefined);
  uiHide({ persisted: false });
  assert.equal(f.host.cleared, 7);
});

test('sin WebGL2 la interfaz sigue operativa y desactiva controles', async () => {
  const f = fixture();
  f.nodes.canvas = element({ getContext: () => null });
  const button = element({ dataset: { camera: 'in' } });
  const contrast = f.nodes['#graphic-contrast'] = element({ disabled: true });
  const original = f.doc.querySelectorAll;
  f.doc.querySelectorAll = selector => selector === '[data-camera]' ? [button] : original(selector);
  await mount(f.doc, f.host).ready;
  assert.match(f.nodes['#graphic-status'].textContent, /WebGL 2/);
  assert.equal(button.disabled, true);
  assert.equal(contrast.disabled, true);
  f.host.location.hash = '#/laboratorio'; f.host.listeners.hashchange();
});

test('el control de contraste mantiene la vista directa si falla el shader', async () => {
  const f = fixture();
  const gl = fakeGl();
  f.nodes.canvas = element({ clientWidth: 600, clientHeight: 400, getContext: () => gl });
  const contrast = f.nodes['#graphic-contrast'] = element({ disabled: true });
  await mount(f.doc, f.host).ready;
  assert.equal(contrast.disabled, false);
  assert.equal(contrast.checked, false);
  gl.getShaderParameter = () => false;
  contrast.checked = true;
  contrast.listeners.change();
  assert.equal(contrast.checked, false);
  assert.match(f.nodes['#graphic-status'].textContent, /No se pudo aplicar el contraste/);
});

test('la página habilita la secante solo cuando puede reproducirse y devuelve el foco al detenerla', async () => {
  const f = fixture();
  f.nodes.canvas = element({ clientWidth: 600, clientHeight: 400, getContext: () => fakeGl() });
  f.nodes['#animation-status'] = element();
  f.nodes['#curve-form'] = element();
  f.nodes['#curve-tangent-x'] = element({ value: '0' });
  f.nodes['#curve-function'] = element({ value: 'x^2' });
  f.nodes['#curve-min'] = element({ value: '-1' });
  f.nodes['#curve-max'] = element({ value: '1' });
  f.nodes['#curve-result'] = element();
  const [secant, camera, stop] = ['secant', 'camera', 'stop'].map(animation => element({ dataset: { animation } }));
  const original = f.doc.querySelectorAll;
  f.doc.querySelectorAll = selector => selector === '[data-animation]' ? [secant, camera, stop] : original(selector);
  f.host.requestAnimationFrame = callback => { f.host.frame = callback; return 1; };
  f.host.cancelAnimationFrame = () => { f.host.frame = null; };

  await mount(f.doc, f.host).ready;
  assert.equal(secant.disabled, true);
  assert.equal(camera.disabled, false);
  assert.equal(stop.disabled, true);
  f.host.location.hash = '#/laboratorio';
  f.host.listeners.hashchange();
  f.nodes['#curve-form'].listeners.submit({ preventDefault() {} });
  assert.equal(secant.disabled, false);
  secant.listeners.click();
  assert.match(f.nodes['#animation-status'].textContent, /Secante en movimiento/);
  assert.equal(stop.disabled, false);
  f.doc.activeElement = stop;
  stop.listeners.click();
  assert.equal(secant.focused, true);
  assert.equal(stop.disabled, true);
  assert.equal(f.host.frame, null);
  assert.equal(f.nodes['#animation-status'].textContent, 'Animación detenida.');

  f.doc.activeElement = camera;
  camera.listeners.click();
  f.host.frame(0);
  f.host.frame(4100);
  assert.match(f.nodes['#animation-status'].textContent, /posición inicial/);
  camera.listeners.click();
  f.host.location.hash = '#/inicio';
  f.host.listeners.hashchange();
  assert.equal(f.host.frame, null);
});
