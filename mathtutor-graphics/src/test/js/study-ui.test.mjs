import test from 'node:test';
import assert from 'node:assert/strict';
import { mountTutor } from '../../main/resources/static/visor/tutor.mjs';
import { element } from './ui-fixture.mjs';

const frames=events=>({ok:true,body:new ReadableStream({start(c){c.enqueue(new TextEncoder().encode(events.map(e=>'data:'+JSON.stringify(e)+'\n\n').join('')));c.close();}})});
function fixture() {
  const ids=['tutor-form','tutor-select-form','tutor-block','tutor-message','tutor-send','tutor-start','tutor-cancel',
    'tutor-status','tutor-history','tutor-composer','tutor-new','tutor-sessions','tutor-topics','tutor-topic-picker',
    'tutor-chat-title','tutor-viewer-slot','tutor-viewer-toggle','graphic-fields','graphic-result',
    'viewer-tangent-controls','viewer-tangent-x','viewer-tangent-y','viewer-tangent-y-field',
    'graphic-function','graphic-form','graphic-type','graphic-min','graphic-max','graphic-steps',
    'graphic-tangent-x','graphic-tangent-y','graphic-tangent-x-field','graphic-tangent-y-field',
    'sessions-more','sessions-refresh','sessions-status','history-more','tutor-retry',
    'tutor-practice','tutor-practice-prompt','exercise-bank','exercise-bank-select','exercise-bank-use'];
  const nodes=Object.fromEntries(ids.map(id=>[id,element({children:[]})]));
  const doc={querySelector:selector=>nodes[selector.slice(1)],querySelectorAll:()=>[],createElement:tagName=>element({tagName,children:[]})};
  Object.assign(nodes['tutor-block'],{value:'funciones'});
  nodes['graphic-type'].value='CURVA_2D'; nodes['graphic-min'].value='-1'; nodes['graphic-max'].value='1'; nodes['graphic-steps'].value='32';
  const state={sessions:[{id:1,module:'funciones',status:'ACTIVA'}],turns:[],runs:[],bank:[{id:7,statement:'Deriva x^2',difficulty:1}],calls:[]};
  const client={token:'jwt',profile:{email:'a@test.es',role:'ALUMNO'},fetch:async(path,options)=>{
    state.calls.push([path,options]); const page=Number(new URL(path,'http://local').searchParams.get('page') || 0);
    const json=data=>({ok:true,json:async()=>data});
    if(path.endsWith('csrf-token')) return json({csrfToken:'csrf'});
    if(path.endsWith('/modules')) return json([{code:'funciones',name:'Funciones'}]);
    if(path.endsWith('/progression')) return state.practice ? json(state.practice) : {ok:true,status:204};
    if(path.includes('/progress')) return json(state.sessions.slice(page*20,(page+1)*20));
    if(path.includes('/history')) return json(state.turns.slice(page*20,(page+1)*20));
    if(path.includes('/bank')) return json(state.bank.slice(page*20,(page+1)*20));
    if(path.includes('/exercises')) {
      if(options.method==='POST') {state.runs=[{id:3,statement:'Derivar x^2',state:'COMENZADO',progress:0,verifiedSteps:0}];return json(state.runs[0]);}
      return json(state.runs.slice(page*20,(page+1)*20));
    }
    if(path.endsWith('/sessions')) return json({id:2,moduleCode:'funciones',status:'ACTIVA'});
    if(path.endsWith('/graphics')) return frames([{type:'GRAPHIC_SPEC',spec:JSON.parse(options.body)},{type:'DONE'}]);
    if(path.endsWith('/messages')) {
      if(state.failMessage) {state.failMessage=false;return {ok:false,status:503};}
      const body=JSON.parse(options.body); state.turns=[{id:1,...body,response:'Piensa',status:'COMPLETADO',verdict:'CORRECTO',diagnosis:'NINGUNO',topic:'derivadas'}];
      return frames([{type:'TOKEN',content:state.answer ?? 'Piensa'},{type:'DONE'}]);
    }
    throw Error('Ruta no prevista: '+path);
  }};
  const viewer={
    setGraph(graph){this.graph=graph;},
    setSurface(mesh){this.mesh=mesh;},
    updateTangency(model){if (model.points) this.setGraph(model); else this.setSurface(model);},
    clear(){this.graph=null;this.mesh=null;this.cleared=true;},
  };
  let sequence=0;
  const ui=mountTutor(doc,client,{viewer,randomUUID:()=>`00000000-0000-4000-8000-${String(++sequence).padStart(12,'0')}`});
  const click=id=>nodes[id].listeners.click();
  const submit=id=>nodes[id].listeners.submit({preventDefault(){}});
  const open=async()=>{await ui.sync('tutor');await nodes['tutor-sessions'].children[0].listeners.click();};
  return {nodes,doc,client,state,viewer,ui,click,submit,open};
}

test('recupera mensajes guardados sin mostrar metadatos internos y permite continuar',async()=>{
  const f=fixture(); f.state.turns=[{id:1,message:'x',response:'**Pista**',status:'ERROR',verdict:'INDETERMINADO',topic:'álgebra'}];
  await f.open();
  assert.equal(f.nodes['tutor-history'].children.length,2);
  assert.equal(f.nodes['tutor-history'].children[1].children.length,2);
  assert.equal(f.nodes['tutor-composer'].hidden,false);
  f.nodes['tutor-message'].value='2*x'; await f.submit('tutor-form');
  assert.equal(f.nodes['tutor-history'].children[1].children.length,2);
  const post=f.state.calls.find(([p])=>p.endsWith('/messages'));
  assert.match(JSON.parse(post[1].body).requestId,/00000000-0000-4000/);
});
test('recuperar un envío incierto reutiliza UUID sin reintento automático',async()=>{
  const f=fixture(); await f.open(); f.state.failMessage=true; f.nodes['tutor-message'].value='mi paso';
  await f.submit('tutor-form');
  assert.equal(f.nodes['tutor-retry'].hidden,false); assert.equal(f.nodes['tutor-send'].disabled,true);
  await f.submit('tutor-form');
  assert.equal(f.state.calls.filter(([p])=>p.endsWith('/messages')).length,1);
  await f.click('tutor-retry');
  const bodies=f.state.calls.filter(([p])=>p.endsWith('/messages')).map(([,o])=>JSON.parse(o.body));
  assert.equal(bodies.length,2); assert.equal(bodies[0].requestId,bodies[1].requestId);
  assert.equal(f.nodes['tutor-retry'].hidden,true);
});
test('el ejercicio del banco se selecciona desde el chat sin publicar progreso',async()=>{
  const f=fixture();await f.open();
  f.nodes['tutor-message'].value='Deriva x^2';await f.submit('tutor-form');
  assert.deepEqual(JSON.parse(f.state.calls.find(([p,o])=>p.endsWith('/exercises')&&o.method==='POST')[1].body),{exerciseId:7});
  assert.equal(f.state.calls.filter(([p,o])=>p.includes('/exercises')&&o.method!=='POST').length,0);
});

test('elegir un ejercicio prepara el borrador y solo lo inicia al enviar', async () => {
  const f = fixture();
  f.state.bank.push({id: 8, statement: 'Deriva x^2'});
  await f.open();
  assert.equal(f.nodes['exercise-bank'].hidden, false);
  f.nodes['exercise-bank-select'].value = '8';
  await f.click('exercise-bank-use');
  assert.equal(f.nodes['tutor-message'].value, 'Deriva x^2');
  assert.equal(f.state.calls.filter(([path]) => path.endsWith('/messages')).length, 0);
  await f.submit('tutor-form');
  const exercise = f.state.calls.find(([path]) => path.endsWith('/exercises'));
  assert.deepEqual(JSON.parse(exercise[1].body), {exerciseId: 8});
  await f.click('tutor-new');
  assert.equal(f.nodes['exercise-bank'].hidden, true);
});

test('la práctica guiada presenta la etapa y el enunciado sin enviar un turno', async () => {
  const f = fixture();
  await f.open();
  f.state.practice = {type: 'TEORICO', statement: 'Explica el significado del límite.'};
  f.nodes['tutor-message'].value = 'Mi borrador';
  await f.click('tutor-practice');
  assert.equal(f.nodes['tutor-practice-prompt'].hidden, false);
  assert.equal(f.nodes['tutor-practice-prompt'].textContent,
    'Práctica conceptual. Explica el significado del límite.');
  assert.equal(f.nodes['tutor-message'].value, 'Mi borrador');
  assert.equal(f.state.calls.filter(([path]) => path.endsWith('/messages')).length, 0);
  assert.equal(f.state.calls.find(([path]) => path.endsWith('/progression'))[1].method, 'POST');
  f.state.practice = null;
  await f.click('tutor-practice');
  assert.match(f.nodes['tutor-practice-prompt'].textContent, /completado/i);
  await f.click('tutor-new');
  assert.equal(f.nodes['tutor-practice-prompt'].hidden, true);
});

test('gráficas validadas se dibujan y se borran al perder la identidad',async()=>{
  const f=fixture();await f.open();f.nodes['graphic-function'].value='x^2';await f.submit('graphic-form');
  assert.ok(f.viewer.graph); assert.match(f.nodes['graphic-result'].textContent,/f\(x\)/);
  f.nodes['graphic-type'].value='SUPERFICIE';f.nodes['graphic-type'].listeners.change();f.nodes['graphic-function'].value='x*y';await f.submit('graphic-form');
  assert.ok(f.viewer.mesh); assert.equal(f.nodes['graphic-steps'].max,'128');
  f.nodes['graphic-function'].value='alert(1)';await f.submit('graphic-form');
  assert.match(f.nodes['graphic-result'].textContent,/inválida/);
  f.client.token=null;await f.ui.sync('tutor');
  assert.equal(f.viewer.cleared,true);assert.equal(f.nodes['tutor-history'].children.length,0);assert.equal(f.nodes['tutor-history'].children.length,0);
});

test('el chat abre el visor solo al recibir una gráfica válida y lo cierra al cambiar de conversación', async () => {
  const f = fixture();
  await f.open();
  assert.equal(f.nodes['tutor-viewer-slot'].hidden, true);
  assert.equal(f.nodes['tutor-viewer-toggle'].hidden, true);
  f.nodes['tutor-message'].value = '¿Qué es una función?';
  await f.submit('tutor-form');
  assert.equal(f.nodes['tutor-viewer-slot'].hidden, true);
  f.nodes['tutor-message'].value = 'Propónme un ejercicio de superficies';
  f.state.answer = 'Explora la superficie `z = x^2 + y^2`.';
  await f.submit('tutor-form');
  assert.ok(f.viewer.mesh);
  assert.equal(f.nodes['tutor-viewer-slot'].hidden, false);
  assert.equal(f.nodes['tutor-viewer-toggle'].hidden, false);
  await f.click('tutor-viewer-toggle');
  assert.equal(f.nodes['tutor-viewer-slot'].hidden, true);
  await f.click('tutor-viewer-toggle');
  assert.equal(f.nodes['tutor-viewer-slot'].hidden, false);
  const requests = f.state.calls.filter(([path]) => path.endsWith('/graphics')).length;
  f.nodes['viewer-tangent-y'].value = '1';
  f.nodes['viewer-tangent-y'].listeners.input();
  assert.ok(Math.abs(f.viewer.mesh.tangent.dy - 2) < 1e-6);
  assert.equal(f.state.calls.filter(([path]) => path.endsWith('/graphics')).length, requests);
  await f.click('tutor-new');
  assert.equal(f.nodes['tutor-viewer-slot'].hidden, true);
  assert.equal(f.viewer.mesh, null);
});

test('el plano tangente del tutor se redibuja para las coordenadas enviadas', async () => {
  const f = fixture();
  await f.open();
  f.nodes['graphic-type'].value = 'PLANO_TANGENTE';
  f.nodes['graphic-type'].listeners.change();
  assert.equal(f.nodes['graphic-tangent-y'].required, true);
  assert.equal(f.nodes['graphic-tangent-y-field'].hidden, false);
  f.nodes['graphic-function'].value = 'x^2+y^2';
  f.nodes['graphic-tangent-x'].valueAsNumber = 0;
  f.nodes['graphic-tangent-y'].valueAsNumber = -0.5;
  await f.submit('graphic-form');
  assert.match(f.nodes['graphic-result'].textContent, /Plano tangente/);
  assert.ok(Math.abs(f.viewer.mesh.tangent.dy + 1) < 1e-6);

  f.nodes['graphic-tangent-y'].valueAsNumber = 0.5;
  await f.submit('graphic-form');
  const graphics = f.state.calls.filter(([path]) => path.endsWith('/graphics'));
  assert.deepEqual(JSON.parse(graphics.at(-1)[1].body).points, [{x: 0, y: 0.5, z: 0}]);
  assert.ok(Math.abs(f.viewer.mesh.tangent.dy - 1) < 1e-6);
  f.nodes['graphic-type'].value = 'RECTA_TANGENTE';
  f.nodes['graphic-type'].listeners.change();
  assert.equal(f.nodes['graphic-tangent-y'].disabled, true);
});
test('una sesión cerrada admite consulta pero nunca mensajes, ejercicios ni gráficas',async()=>{
  const f=fixture();f.state.sessions[0].status='CERRADA';await f.open();
  assert.equal(f.nodes['tutor-composer'].hidden,true);assert.equal(f.nodes['graphic-fields'].disabled,true);
  await f.submit('graphic-form');f.nodes['tutor-message'].value='hola';await f.submit('tutor-form');
  assert.equal(f.state.calls.filter(([,o])=>o.method==='POST').length,0);
});
test('paginación de sesiones y turnos se mantiene disponible',async()=>{
  const f=fixture();f.state.sessions=Array.from({length:21},(_,i)=>({id:i+1,module:'funciones',status:'ACTIVA'}));
  f.state.turns=Array.from({length:21},(_,i)=>({id:21-i,message:'paso '+i,response:'pista',status:'COMPLETADO'}));
  f.state.bank=Array.from({length:21},(_,i)=>({id:i+1,statement:'Ejercicio '+i,difficulty:1}));
  f.state.runs=Array.from({length:21},(_,i)=>({id:i+1,statement:'Ejercicio '+i,state:'COMPLETADO',progress:1}));
  await f.open();await f.click('sessions-more');assert.equal(f.nodes['tutor-sessions'].children.length,21);
  await f.click('history-more');assert.equal(f.nodes['tutor-history'].children.length,42);
  await f.click('sessions-refresh');assert.equal(f.nodes['tutor-sessions'].children.length,20);
});
test('descarta un historial tardío al salir de la cuenta',async()=>{
  const f=fixture();await f.ui.sync('tutor');const original=f.client.fetch;let release;
  f.client.fetch=(path,options)=>path.includes('/history') ? new Promise(resolve=>{release=resolve;}) : original(path,options);
  const opening=f.nodes['tutor-sessions'].children[0].listeners.click();await new Promise(r=>setImmediate(r));
  f.client.token=null;await f.ui.sync('tutor');release({ok:true,json:async()=>[{id:1,message:'privado',response:'secreto'}]});await opening;
  assert.equal(f.nodes['tutor-history'].children.length,0);assert.equal(f.nodes['tutor-sessions'].children.length,0);
});
test('un rechazo previo al turno explica cómo corregirlo sin ofrecer recuperar un envío inexistente',async()=>{
  const f=fixture();await f.open();const original=f.client.fetch;
  f.client.fetch=(path,options)=>path.endsWith('/messages') ? Promise.resolve({ok:false,status:400}) : original(path,options);
  f.nodes['tutor-message'].value='paso';await f.submit('tutor-form');
  assert.equal(f.nodes['tutor-retry'].hidden,true);
  assert.match(f.nodes['tutor-status'].textContent,/Revisa/);
});
test('un flujo gráfico interrumpido nunca dibuja una especificación parcial',async()=>{
  const f=fixture();await f.open();const original=f.client.fetch;
  f.client.fetch=(path,options)=>path.endsWith('/graphics') ? Promise.resolve(frames([{type:'GRAPHIC_SPEC',spec:{}}])) : original(path,options);
  await f.submit('graphic-form');assert.equal(f.viewer.graph,undefined);
  assert.match(f.nodes['graphic-result'].textContent,/interrumpió/);
});
test('fallar al abrir otra conversación desactiva el panel anterior y evita mezclar sesiones',async()=>{
  const f=fixture();f.state.sessions.push({id:2,module:'funciones',status:'ACTIVA'});await f.open();
  const original=f.client.fetch;
  f.client.fetch=(path,options)=>path.includes('/sessions/2/history') ? Promise.resolve({ok:false,status:503}) : original(path,options);
  await f.nodes['tutor-sessions'].children[1].listeners.click();
  const before=f.state.calls.filter(([p,o])=>p.endsWith('/exercises')&&o.method==='POST').length;
  f.nodes['tutor-message'].value='Deriva x^2';await f.submit('tutor-form');
  assert.equal(f.nodes['graphic-fields'].disabled,true);
  assert.equal(f.state.calls.filter(([p,o])=>p.endsWith('/exercises')&&o.method==='POST').length,before);
  assert.equal(f.nodes['tutor-composer'].hidden,true);
});
test('cancelar un envío no deja abortado el controlador de paginación',async()=>{
  const f=fixture();f.state.turns=Array.from({length:21},(_,i)=>({id:21-i,message:'paso',response:'pista',status:'COMPLETADO'}));await f.open();
  const original=f.client.fetch;
  f.client.fetch=(path,options)=>{
    if (options.signal?.aborted) return Promise.reject(new DOMException('Cancelado','AbortError'));
    if (path.endsWith('/messages')) return new Promise((resolve,reject)=>options.signal.addEventListener('abort',()=>reject(new DOMException('Cancelado','AbortError'))));
    return original(path,options);
  };
  f.nodes['tutor-message'].value='nuevo';const sending=f.submit('tutor-form');await new Promise(r=>setImmediate(r));
  f.click('tutor-cancel');await sending;await f.click('history-more');
  assert.equal(f.nodes['tutor-history'].children.length,42);
});

test('la petici?n de derivar en el chat inicia el ejercicio antes de enviar el turno',async()=>{
  const f=fixture(); f.state.bank=[]; await f.open();
  f.nodes['tutor-message'].value='Deriva x^2'; await f.submit('tutor-form');
  const selection=f.state.calls.findIndex(([p,o])=>p.endsWith('/exercises') && o.method==='POST');
  const message=f.state.calls.findIndex(([p])=>p.endsWith('/messages'));
  assert.ok(selection>=0 && selection<message);
  assert.deepEqual(JSON.parse(f.state.calls[selection][1].body),{operation:'DERIVADA',expression:'x^2'});
});
