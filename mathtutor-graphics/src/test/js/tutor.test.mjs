import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readReply, mountTutor } from '../../main/resources/static/visor/tutor.mjs';
import { element } from './ui-fixture.mjs';

const response = (text, cuts = [text]) => ({ ok: true, body: new ReadableStream({ start(c) {
  for (const chunk of cuts) c.enqueue(new TextEncoder().encode(chunk)); c.close();
} }) });
const events = 'data:{"type":"TOKEN","content":"Función ñ"}\n\ndata:{"type":"DONE","content":""}\n\n';
test('SSE fragmentado y CRLF conserva tokens UTF-8 en orden', async () => {
  let text = '';
  await readReply(response(events, [...events]), delta => text += delta);
  assert.equal(text, 'Función ñ');
  await readReply(response(events.replaceAll('\n', '\r\n')), () => {});
});
test('SSE incompleto, error de servidor y desbordamiento se anuncian como errores', async () => {
  for (const text of ['data:{"type":"ERROR","content":"error"}\n\n', 'data: {}\n\n',
    'data:{"type":"TOKEN","content":"parcial"}\n\n', 'data: no-json\n\n', 'x'.repeat(65537)]) {
    await assert.rejects(readReply(response(text), () => {}));
  }
  await assert.rejects(readReply({ok:false}, () => {}));
});
function fixture() {
  const nodes = Object.fromEntries(['tutor-form','tutor-select-form','tutor-block','tutor-message',
    'tutor-send','tutor-start','tutor-cancel','tutor-status','tutor-history','tutor-composer'].map(k => [k, element()]));
  nodes['tutor-block'].value = 'funciones'; nodes['tutor-message'].value = '¿Qué es una función?';
  nodes['tutor-history'].append = function(node) { (this.children ||= []).push(node); };
  nodes['tutor-history'].replaceChildren = function() { this.children = []; };
  nodes['tutor-block'].append = function() {};
  nodes['tutor-block'].replaceChildren = function() {};
  const client = { token:'jwt', profile:{email:'alumno@test.es',role:'ALUMNO'}, calls:[],
    fetch: async (path, options) => {
      client.calls.push([path, options]);
      if (client.fail) return {ok:false};
      if (path.endsWith('/messages')) return response(events);
      return { ok:true, json:async () => path.endsWith('csrf-token') ? {csrfToken:'csrf'}
        : path.endsWith('/modules') ? [{code:'funciones',name:'Funciones'}] : {id:1,moduleCode:'funciones'} };
    } };
  const doc = { querySelector:s => nodes[s.slice(1)], createElement:() => element() };
  const ui = mountTutor(doc,client);
  const submit = id => nodes[id].listeners.submit({preventDefault(){}});
  return {nodes,client,ui,submit};
}
test('selección previa, POST protegido, respuesta progresiva y sesión nueva', async () => {
  const f = fixture();
  await f.ui.sync('tutor');
  await f.submit('tutor-form'); // No hay sesión todavía.
  assert.equal(f.client.calls.filter(([p]) => p.endsWith('/messages')).length,0);
  await f.submit('tutor-select-form');
  await f.submit('tutor-form');
  assert.equal(f.nodes['tutor-history'].children[1].textContent,'Tutor: Función ñ');
  const [, options] = f.client.calls.find(([p]) => p.endsWith('/messages'));
  assert.equal(options.headers.Authorization,'Bearer jwt');
  assert.equal(options.headers['X-XSRF-TOKEN'],'csrf');
  await f.submit('tutor-select-form');
  assert.equal(f.nodes['tutor-history'].children.length,0);
});
test('errores, mensajes vacíos, cancelación y pérdida de acceso no reutilizan sesiones', async () => {
  const f = fixture(); await f.ui.sync('inicio');
  f.client.fail = true; await f.ui.sync('tutor');
  assert.match(f.nodes['tutor-status'].textContent,/No se pudieron/);
  await f.submit('tutor-select-form');
  f.client.fail = false; await f.submit('tutor-select-form');
  f.nodes['tutor-message'].value = ' '; await f.submit('tutor-form');
  f.nodes['tutor-message'].value = 'a'.repeat(4001); await f.submit('tutor-form');
  f.client.fail = true; f.nodes['tutor-message'].value = 'hola'; await f.submit('tutor-form');
  assert.match(f.nodes['tutor-status'].textContent,/No se pudo/);
  f.nodes['tutor-cancel'].listeners.click();
  f.client.token = null; await f.ui.sync('tutor');
  assert.equal(f.nodes['tutor-composer'].hidden,true);
  f.client.token = 'jwt'; f.client.profile.role = 'PROFESOR'; await f.ui.sync('tutor');
  assert.equal(f.nodes['tutor-composer'].hidden,true);
});
