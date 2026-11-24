import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
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
function fixture(moduleCode = 'funciones') {
  const nodes = Object.fromEntries(['tutor-form','tutor-select-form','tutor-block','tutor-message',
    'tutor-send','tutor-start','tutor-cancel','tutor-status','tutor-announcement','tutor-history','tutor-composer',
    'tutor-new','tutor-sessions','tutor-topics','tutor-topic-picker','tutor-chat-title','tutor-viewer-slot','tutor-viewer-toggle'].map(k => [k, element()]));
  nodes['tutor-announcement'].querySelectorAll = () => [];
  const starters = ['notes', 'x^2+2*x=0'].map(prompt =>
    element({dataset:{prompt}}));
  nodes['tutor-block'].value = moduleCode; nodes['tutor-message'].value = '¿Qué es una función?';
  nodes['tutor-history'].append = function(node) { (this.children ||= []).push(node); };
  nodes['tutor-history'].replaceChildren = function(...children) { this.children = children; };
  nodes['tutor-block'].append = function() {};
  nodes['tutor-block'].replaceChildren = function() {};
  for (const id of ['tutor-sessions','tutor-topics']) {
    nodes[id].replaceChildren = function() { this.children = []; };
    nodes[id].append = function(node) { (this.children ||= []).push(node); };
  }
  const client = { token:'jwt', profile:{email:'alumno@test.es',role:'ALUMNO'}, calls:[],
    fetch: async (path, options) => {
      client.calls.push([path, options]);
      if (client.fail) return {ok:false};
      if (path.includes('/progress')) return {ok:true,json:async()=>[]};
      if (path.includes('/history')) return {ok:true,json:async()=>client.entries || []};
      if (path.endsWith('/messages')) {
        const body=JSON.parse(options.body);
        client.entries=[{id:1,message:body.message,response:'Función ñ',status:'COMPLETADO',requestId:body.requestId,verdict:'NO_APLICA'}];
        return response(events);
      }
      return { ok:true, json:async () => path.endsWith('csrf-token') ? {csrfToken:'csrf'}
        : path.endsWith('/modules') ? [{code:'funciones',name:'Funciones'}]
          : {id:1,moduleCode:JSON.parse(options?.body || '{}').moduleCode || 'funciones'} };
    } };
  const doc = { querySelector:s => nodes[s.slice(1)], querySelectorAll:s => s === '[data-prompt]' ? starters : [],
    createElement:tagName => element({tagName}) };
  const ui = mountTutor(doc,client);
  const submit = id => nodes[id].listeners.submit({preventDefault(){}});
  return {nodes,client,ui,submit,starters};
}
test('la pregunta sugerida usa un tema concreto de cada módulo sin enviar el turno', async () => {
  const examples = [
    ['funciones', '¿Qué es la continuidad de una función?'],
    ['derivadas-integrales', '¿Cómo se calcula la derivada de una función?'],
    ['metodos-3d', '¿Qué es un espacio vectorial?'],
  ];
  for (const [moduleCode, expected] of examples) {
    const f = fixture(moduleCode); await f.ui.sync('tutor'); await f.submit('tutor-select-form');
    f.starters[0].listeners.click();
    assert.equal(f.nodes['tutor-message'].value, expected);
    assert.equal(f.client.calls.filter(([path]) => path.endsWith('/messages')).length, 0);
    f.starters[1].listeners.click();
    assert.equal(f.nodes['tutor-message'].value, 'x^2+2*x=0');
    assert.equal(f.nodes['tutor-message'].focused, true);
    f.client.token = null; await f.ui.sync('tutor');
    f.starters[0].listeners.click();
    assert.equal(f.nodes['tutor-message'].value, '');
  }
});
test('selección previa, POST protegido, respuesta progresiva y sesión nueva', async () => {
  const f = fixture();
  await f.ui.sync('tutor');
  await f.submit('tutor-form'); // No hay sesión todavía.
  assert.equal(f.client.calls.filter(([p]) => p.endsWith('/messages')).length,0);
  await f.submit('tutor-select-form');
  f.nodes['tutor-message'].value = '¿Qué es una función?';
  await f.submit('tutor-form');
  const answer = f.nodes['tutor-history'].children[1];
  assert.equal(answer.children[0].textContent, 'Tutor:');
  assert.equal(answer.children[1].children[0].children[0].textContent, 'Función ñ');
  assert.equal(f.nodes['tutor-announcement'].children[0].children[0].textContent, 'Función ñ');
  const [, options] = f.client.calls.find(([p]) => p.endsWith('/messages'));
  assert.equal(options.headers.Authorization,'Bearer jwt');
  assert.equal(options.headers['X-XSRF-TOKEN'],'csrf');
  await f.submit('tutor-select-form');
  assert.equal(f.nodes['tutor-history'].children.length,0);
  assert.equal(f.nodes['tutor-announcement'].children.length,0);
});
test('la pestaña del tutor deja el cambio de módulo en Nueva conversación', async () => {
  const html = readFileSync(new URL('../../main/resources/static/visor/index.html', import.meta.url), 'utf8');
  assert.doesNotMatch(html, /Cambiar de módulo|Cambiar tema/);
  const f = fixture(); await f.ui.sync('tutor'); await f.submit('tutor-select-form');
  f.nodes['tutor-new'].listeners.click();
  assert.equal(f.nodes['tutor-topic-picker'].hidden, false);
  assert.equal(f.nodes['tutor-topics'].children.length, 1);
});
test('los tokens Markdown se convierten en formato en la conversación', async () => {
  const f = fixture(); await f.ui.sync('tutor'); await f.submit('tutor-select-form');
  f.nodes['tutor-message'].value = '¿Cómo derivo?';
  const original = f.client.fetch;
  f.client.fetch = (path, options) => path.endsWith('/messages')
    ? response('data:{"type":"TOKEN","content":"## Deri"}\n\ndata:{"type":"TOKEN","content":"vadas\\n\\n**Regla**"}\n\ndata:{"type":"DONE","content":""}\n\n')
    : original(path, options);
  await f.submit('tutor-form');
  const body = f.nodes['tutor-history'].children[1].children[1];
  assert.deepEqual(body.children.map(node => node.tagName), ['h2', 'p']);
  assert.equal(body.children[0].children[0].textContent, 'Derivadas');
  assert.equal(body.children[1].children[0].tagName, 'strong');
});
test('errores, mensajes vacíos, cancelación y pérdida de acceso no reutilizan sesiones', async () => {
  const f = fixture(); await f.ui.sync('inicio');
  f.client.fail = true; await f.ui.sync('tutor');
  assert.match(f.nodes['tutor-status'].textContent,/No se pudieron/);
  await f.submit('tutor-select-form');
  f.client.fail = false; await f.ui.sync('tutor'); await f.submit('tutor-select-form');
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


test('el bloque no permite iniciar hasta que el catálogo esté disponible', async () => {
  const f = fixture();
  assert.equal(f.nodes['tutor-start'].disabled, true);
  await f.ui.sync('tutor');
  assert.equal(f.nodes['tutor-start'].disabled, false);
});

test('perder la identidad elimina también el borrador privado', async () => {
  const f = fixture(); await f.ui.sync('tutor');
  f.nodes['tutor-message'].value = 'Mi pregunta privada';
  f.client.token = null; await f.ui.sync('tutor');
  assert.equal(f.nodes['tutor-message'].value, '');
  assert.match(f.nodes['tutor-status'].textContent, /Inicia sesi/);
});

test('salir durante el alta descarta la sesión tardía y permite volver a empezar', async () => {
  const f = fixture(); await f.ui.sync('tutor');
  const original = f.client.fetch;
  let complete;
  f.client.fetch = (path, options) => path.endsWith('/sessions')
    ? new Promise(resolve => { complete = resolve; }) : original(path, options);
  const pending = f.submit('tutor-select-form');
  await new Promise(resolve => setImmediate(resolve));
  await f.ui.sync('inicio');
  complete({ok:true,json:async () => ({id:99})}); await pending;
  await f.ui.sync('tutor');
  assert.equal(f.nodes['tutor-composer'].hidden, true);
  assert.equal(f.nodes['tutor-start'].disabled, false);
});

test('las tarjetas usan el catálogo y los chats de la visita se pueden recuperar', async () => {
  const f = fixture(); await f.ui.sync('tutor');
  assert.equal(f.nodes['tutor-topics'].children[0].textContent, 'Funciones');
  await f.submit('tutor-select-form');
  f.nodes['tutor-message'].value = 'Primera pregunta'; await f.submit('tutor-form');
  f.nodes['tutor-new'].listeners.click();
  assert.equal(f.nodes['tutor-topic-picker'].hidden, false);
  assert.equal(f.nodes['tutor-sessions'].children.length, 1);
  await f.nodes['tutor-sessions'].children[0].listeners.click();
  assert.equal(f.nodes['tutor-history'].children.length, 2);
  f.client.token = null; await f.ui.sync('tutor');
  assert.equal(f.nodes['tutor-sessions'].children.length, 0);
});

test('el docente inicia y usa conversaciones propias', async () => {
  const f=fixture(); f.client.profile.role='PROFESOR';
  await f.ui.sync('tutor'); await f.submit('tutor-select-form');
  assert.equal(f.nodes['tutor-composer'].hidden,false);
  f.nodes['tutor-message'].value='?Qu? es una funci?n?'; await f.submit('tutor-form');
  assert.ok(f.client.calls.some(([p])=>p.endsWith('/messages')));
});
test('el visor se pliega y el ejercicio no tiene selecci?n manual', () => {
  const html=readFileSync(new URL('../../main/resources/static/visor/index.html',import.meta.url),'utf8');
  assert.match(html, /id="tutor-viewer-toggle"[^>]*aria-controls="tutor-viewer-slot"/);
  assert.doesNotMatch(html, /id="tutor-study"|id="exercise-progress"|id="exercise-runs"|id="tutor-diagnosis"/);
  assert.doesNotMatch(html, /Empezar ejercicio|id="exercise-kind"|id="exercise-form"/);
});

test('una conversación sin gráfica mantiene el visor contraído',async()=>{
  const f=fixture();await f.ui.sync('tutor');await f.submit('tutor-select-form');
  f.nodes['tutor-viewer-toggle'].listeners.click();
  assert.equal(f.nodes['tutor-viewer-slot'].hidden,true);
  assert.equal(f.nodes['tutor-viewer-toggle'].attributes['aria-expanded'],'false');
  f.nodes['tutor-viewer-toggle'].listeners.click();
  assert.equal(f.nodes['tutor-viewer-slot'].hidden,true);
  assert.equal(f.nodes['tutor-viewer-toggle'].attributes['aria-expanded'],'false');
  assert.equal(f.nodes['tutor-composer'].hidden,false);
});
