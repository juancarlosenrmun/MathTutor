import test from 'node:test';
import assert from 'node:assert/strict';
import { mount } from '../../main/resources/static/visor/app.mjs';
import { fakeGl } from './fake-gl.mjs';

function element(extra = {}) {
  return { textContent: '', listeners: {}, addEventListener(name, fn) { this.listeners[name] = fn; },
    removeEventListener() {}, ...extra };
}

test('la página conecta controles y refleja cambios de rol en menos de cinco segundos', async () => {
  let currentRole = 'ALUMNO', online = true, loginAllowed = true;
  const nodes = { '#graphic-status': element(), '#role': element(),
    canvas: element({ clientWidth: 600, clientHeight: 400, getContext: () => fakeGl() }),
    form: element({ elements: { email: { value: 'a@test.es' }, password: { value: 'secret' } } }) };
  const button = element({ dataset: { camera: 'in' } });
  const host = element({ setInterval(fn, delay) { this.poll = fn; assert.equal(delay, 3000); return 7; },
    clearInterval(id) { this.cleared = id; },
    async fetch(path) {
      if (!online) throw new Error('Sin conexión');
      return { ok: loginAllowed, json: async () => path.endsWith('csrf-token') ? { csrfToken: 'csrf' }
        : { data: path.endsWith('login') ? { accessToken: 'jwt' } : { role: currentRole } } };
    } });
  mount({ querySelector: key => nodes[key], querySelectorAll: () => [button] }, host);
  button.listeners.click();
  const submit = () => nodes.form.listeners.submit({ preventDefault() {}, currentTarget: nodes.form });
  await submit();
  assert.equal(nodes['#role'].textContent, 'ALUMNO');
  assert.equal(nodes.form.elements.password.value, '');
  currentRole = 'PROFESOR';
  await host.poll();
  assert.equal(nodes['#role'].textContent, 'PROFESOR');
  online = false;
  await host.poll();
  assert.match(nodes['#role'].textContent, /No se pudo/);
  online = true;
  loginAllowed = false;
  await submit();
  assert.match(nodes['#role'].textContent, /Credenciales/);
  await host.poll();
  assert.equal(nodes['#role'].textContent, 'Sin sesión');
  host.listeners.pagehide({ persisted: true });
  assert.equal(host.cleared, undefined);
  host.listeners.pagehide({ persisted: false });
  assert.equal(host.cleared, 7);
});

test('sin WebGL2 muestra alternativa textual y desactiva controles', () => {
  const status = element();
  const button = element({ dataset: { camera: 'in' } });
  const nodes = { '#graphic-status': status, '#role': element(), canvas: element({ getContext: () => null }), form: element() };
  const host = element({ fetch() {}, setInterval() {}, clearInterval() {} });
  mount({ querySelector: key => nodes[key], querySelectorAll: () => [button] }, host);
  assert.match(status.textContent, /WebGL 2/);
  assert.equal(button.disabled, true);
  host.listeners.pagehide({ persisted: false });
});
