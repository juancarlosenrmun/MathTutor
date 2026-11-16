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
  const original = f.doc.querySelectorAll;
  f.doc.querySelectorAll = selector => selector === '[data-camera]' ? [button] : original(selector);
  await mount(f.doc, f.host).ready;
  assert.match(f.nodes['#graphic-status'].textContent, /WebGL 2/);
  assert.equal(button.disabled, true);
  f.host.location.hash = '#/laboratorio'; f.host.listeners.hashchange();
});
