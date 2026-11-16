import test from 'node:test';
import assert from 'node:assert/strict';
import { mountInterface } from '../../main/resources/static/visor/interface.mjs';
import { fixture } from './ui-fixture.mjs';

async function setup() { const f = fixture(); f.app = mountInterface(f.doc, f.host, () => {}); await f.app.ready; return f; }

test('restaura cookie, protege cuenta y actualiza navegación, título y rol', async () => {
  const f = fixture(); f.host.location.hash = '#/cuenta'; f.state.restore = true;
  const app = mountInterface(f.doc, f.host, () => {});
  assert.equal(f.forms[0].button.disabled, true);
  await app.ready;
  assert.equal(f.doc.title, 'Tu cuenta · MathTutor');
  assert.equal(f.nodes['#role'].textContent, 'ALUMNO');
  assert.equal(f.nodes['#account-email'].textContent, 'a@test.es');
  assert.equal(f.guests[0].hidden, true); assert.equal(f.members[0].hidden, false);
  assert.equal(f.host.delay, 3000);
  f.state.role = 'PROFESOR'; await app.updateRole();
  assert.equal(f.nodes['#role'].textContent, 'PROFESOR');
  f.host.location.hash = '#/laboratorio'; f.host.listeners.hashchange();
  assert.equal(f.navs[1].attributes['aria-current'], 'page');
  f.host.listeners.pagehide({ persisted: true });
  assert.equal(app.client.token, 'jwt');
  f.host.listeners.pagehide({ persisted: false });
  app.render(); await app.updateRole();
  assert.equal(app.client.token, null); assert.equal(f.host.cleared, 7);
});

test('login y registro reales, limpieza de contraseña, estado ocupado y logout', async () => {
  for (const index of [0, 1]) {
    const f = await setup();
    f.host.location.hash = index ? '#/registro' : '#/acceso'; f.app.render();
    const submission = f.submit(f.forms[index]);
    assert.equal(f.forms[index].button.disabled, true);
    await f.submit(f.forms[index]); await f.app.updateRole();
    await submission;
    assert.equal(f.host.location.hash, '#/espacio');
    assert.equal(f.forms[index].elements.password.value, '');
    assert.equal(f.forms[index].button.disabled, false);
    assert.match(f.nodes['#notice'].textContent, index ? /cuenta está lista/ : /iniciado sesión/);
    const logout = f.nodes['#logout'].listeners.click();
    await f.nodes['#logout'].listeners.click();
    await logout;
    assert.equal(f.app.client.token, null); assert.equal(f.host.location.hash, '#/acceso');
    assert.match(f.nodes['#notice'].textContent, /cerrado/);
  }
});

test('el espacio autenticado refleja cambios de rol sin crear rutas de sprints futuros', async () => {
  const f = fixture(); f.host.location.hash = '#/espacio'; f.state.restore = true;
  const app = mountInterface(f.doc, f.host, () => {}); await app.ready;
  assert.equal(f.views.find(view => !view.hidden).dataset.view, 'espacio');
  assert.equal(f.doc.title, 'Tu espacio · MathTutor');
  assert.equal(f.nodes['#workspace-role'].textContent, 'Alumno');
  assert.match(f.nodes['#workspace-title'].textContent, /aprendizaje/);
  for (const [role, label] of [['PROFESOR', 'Profesor'], ['ADMIN', 'Administrador']]) {
    f.state.role = role; await app.updateRole();
    assert.equal(f.nodes['#workspace-role'].textContent, label);
    assert.match(f.nodes['#workspace-description'].textContent, /laboratorio/);
  }
  f.state.status = 401; await app.updateRole();
  assert.equal(f.views.find(view => !view.hidden).dataset.view, 'acceso');
});

test('validación de confirmación y errores del API enfocados sin filtrar detalles', async () => {
  const f = await setup(); f.host.location.hash = '#/registro'; f.app.render();
  const form = f.forms[1]; form.elements.password.value = 'aaa'; form.elements.confirm.value = 'bbb';
  await form.listeners.submit({ preventDefault() {} });
  assert.match(form.error.textContent, /no coinciden/); assert.equal(form.elements.confirm.focused, true);
  assert.equal(form.elements.confirm.attributes['aria-invalid'], 'true');
  f.state.status = 400; await f.submit(form);
  assert.match(form.error.textContent, /registro/); assert.equal(form.error.focused, true);
  assert.equal(form.elements.confirm.attributes['aria-invalid'], undefined);
  f.state.status = 403; await f.submit(f.forms[0]);
  assert.equal(f.host.location.hash, '#/restringido');
});

test('mostrar y ocultar, navegación borra secretos aunque estén visibles', async () => {
  const f = await setup(); const button = f.passwords[0], input = f.forms[0].elements.password;
  input.value = 'privado'; button.listeners.click();
  assert.equal(input.type, 'text'); assert.equal(button.attributes['aria-pressed'], 'true');
  button.listeners.click(); assert.equal(input.type, 'password');
  button.listeners.click(); f.host.location.hash = '#/registro'; f.app.render();
  assert.equal(input.value, ''); assert.equal(input.type, 'password');
});

test('errores transitorios conservan sesión, 403 restringe y 401 la caduca', async () => {
  const f = await setup(); await f.submit(f.forms[0]);
  f.state.online = false; await f.app.updateRole();
  assert.match(f.nodes['#notice'].textContent, /No se pudo comprobar/); assert.equal(f.app.client.token, 'jwt');
  f.state.online = true; f.state.status = 403; await f.app.updateRole();
  assert.equal(f.host.location.hash, '#/restringido');
  f.state.status = 200; await f.submit(f.forms[0]);
  f.state.status = 401; await f.app.updateRole();
  assert.equal(f.app.client.token, null); assert.match(f.nodes['#notice'].textContent, /terminado/);
});

test('restauración sin red permite volver a iniciar sesión; cierre fallido ofrece reintento', async () => {
  const f = fixture(); f.state.online = false;
  const app = mountInterface(f.doc, f.host, () => {}); await app.ready;
  assert.match(f.nodes['#notice'].textContent, /recuperar/);
  f.state.online = true; await f.submit(f.forms[0]); f.state.status = 500;
  await f.nodes['#logout'].listeners.click();
  assert.equal(app.client.token, null); assert.equal(f.nodes['#retry-logout'].hidden, false);
  f.state.status = 200; await f.nodes['#retry-logout'].listeners.click();
  assert.equal(f.nodes['#retry-logout'].hidden, true);
});

test('enviar durante la restauración no realiza una segunda autenticación', async () => {
  const f = fixture(); const app = mountInterface(f.doc, f.host, () => {});
  await f.submit(f.forms[0]); await app.ready;
  assert.equal(f.state.calls.some(([path]) => path.endsWith('/login')), false);
});

test('saltar al contenido conserva la vista actual', async () => {
  const f = await setup(); f.host.location.hash = '#/laboratorio'; f.app.render();
  f.nodes['.skip-link'].listeners.click({ preventDefault() {} });
  assert.equal(f.views.find(view => !view.hidden).dataset.view, 'laboratorio');
  assert.equal(f.host.location.hash, '#/laboratorio');
  assert.equal(f.nodes['#main'].focused, true);
});

test('el error tardío de permisos no oculta el reintento de cierre fallido', async () => {
  const f = await setup(); await f.submit(f.forms[0]);
  let reject;
  const previous = f.app.client.fetch;
  f.app.client.fetch = (path, options) => path.endsWith('/me')
    ? new Promise((_, fail) => { reject = fail; }) : previous(path, options);
  const poll = f.app.updateRole();
  f.state.status = 500; await f.nodes['#logout'].listeners.click();
  reject(new Error('timeout')); await poll;
  assert.equal(f.nodes['#retry-logout'].hidden, false);
  assert.match(f.nodes['#notice'].textContent, /servidor no confirmó/);
});
