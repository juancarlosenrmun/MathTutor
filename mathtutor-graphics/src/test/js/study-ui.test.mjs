import test from 'node:test';
import assert from 'node:assert/strict';
import { mountTutor } from '../../main/resources/static/visor/tutor.mjs';
import { element } from './ui-fixture.mjs';

const frames=events=>({ok:true,body:new ReadableStream({start(c){c.enqueue(new TextEncoder().encode(events.map(e=>'data:'+JSON.stringify(e)+'\n\n').join('')));c.close();}})});
function fixture() {
  const ids=['tutor-form','tutor-select-form','tutor-block','tutor-message','tutor-send','tutor-start','tutor-cancel',
    'tutor-status','tutor-history','tutor-composer','tutor-new','tutor-sessions','tutor-topics','tutor-topic-picker',
    'tutor-chat-title','tutor-viewer-slot','tutor-study','exercise-status','exercise-fields','graphic-fields',
    'bank-more','runs-more','exercise-kind','exercise-bank-group','exercise-expression-group','exercise-expression',
    'exercise-bank','exercise-runs','exercise-progress','exercise-summary','bank-empty','tutor-diagnosis','graphic-result',
    'graphic-function','exercise-form','graphic-form','graphic-type','graphic-min','graphic-max','graphic-steps',
    'sessions-more','sessions-refresh','sessions-status','history-more','tutor-retry'];
  const nodes=Object.fromEntries(ids.map(id=>[id,element({children:[]})]));
  const doc={querySelector:selector=>nodes[selector.slice(1)],querySelectorAll:()=>[],createElement:tagName=>element({tagName,children:[]})};
  Object.assign(nodes['tutor-block'],{value:'funciones'}); nodes['exercise-kind'].value='DERIVADA';
  nodes['graphic-type'].value='CURVA_2D'; nodes['graphic-min'].value='-1'; nodes['graphic-max'].value='1'; nodes['graphic-steps'].value='32';
  const state={sessions:[{id:1,module:'funciones',status:'ACTIVA'}],turns:[],runs:[],bank:[{id:7,statement:'Deriva x^2',difficulty:1}],calls:[]};
  const client={token:'jwt',profile:{email:'a@test.es',role:'ALUMNO'},fetch:async(path,options)=>{
    state.calls.push([path,options]); const page=Number(new URL(path,'http://local').searchParams.get('page') || 0);
    const json=data=>({ok:true,json:async()=>data});
    if(path.endsWith('csrf-token')) return json({csrfToken:'csrf'});
    if(path.endsWith('/modules')) return json([{code:'funciones',name:'Funciones'}]);
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
      return frames([{type:'TOKEN',content:'Piensa'},{type:'DONE'}]);
    }
    throw Error('Ruta no prevista: '+path);
  }};
  const viewer={setGraph(graph){this.graph=graph;},setSurface(mesh){this.mesh=mesh;},clear(){this.graph=null;this.mesh=null;this.cleared=true;}};
  let sequence=0;
  const ui=mountTutor(doc,client,{viewer,randomUUID:()=>`00000000-0000-4000-8000-${String(++sequence).padStart(12,'0')}`});
  const click=id=>nodes[id].listeners.click();
  const submit=id=>nodes[id].listeners.submit({preventDefault(){}});
  const open=async()=>{await ui.sync('tutor');await nodes['tutor-sessions'].children[0].listeners.click();};
  return {nodes,doc,client,state,viewer,ui,click,submit,open};
}

test('recupera conversaciones y mensajes guardados con diagnóstico, y permite continuar',async()=>{
  const f=fixture(); f.state.turns=[{id:1,message:'x',response:'**Pista**',status:'ERROR',verdict:'INDETERMINADO',topic:'álgebra'}];
  await f.open();
  assert.equal(f.nodes['tutor-history'].children.length,2);
  assert.match(f.nodes['tutor-diagnosis'].textContent,/No se ha podido comprobar/);
  assert.equal(f.nodes['tutor-composer'].hidden,false);
  f.nodes['tutor-message'].value='2*x'; await f.submit('tutor-form');
  assert.match(f.nodes['tutor-diagnosis'].textContent,/verificado/);
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
test('selecciona banco u operación libre y muestra progreso de cada ejercicio',async()=>{
  const f=fixture();await f.open();
  f.nodes['exercise-kind'].value='BANCO';f.nodes['exercise-kind'].listeners.change();f.nodes['exercise-bank'].value='7';
  await f.submit('exercise-form');
  assert.match(f.nodes['exercise-status'].textContent,/Ejercicio iniciado/);
  assert.deepEqual(JSON.parse(f.state.calls.find(([p,o])=>p.endsWith('/exercises')&&o.method==='POST')[1].body),{exerciseId:7});
  f.nodes['exercise-kind'].value='DERIVADA'; f.nodes['exercise-expression'].value='x^2'; await f.submit('exercise-form');
  assert.equal(f.nodes['exercise-runs'].children.length,1);
  f.state.runs=[{id:3,statement:'Derivar x^2',state:'COMPLETADO',progress:1}];
  f.nodes['tutor-message'].value='2*x'; await f.submit('tutor-form');
  assert.equal(f.nodes['exercise-progress'].value,1);
});
test('gráficas validadas se dibujan y se borran al perder la identidad',async()=>{
  const f=fixture();await f.open();f.nodes['graphic-function'].value='x^2';await f.submit('graphic-form');
  assert.ok(f.viewer.graph); assert.match(f.nodes['graphic-result'].textContent,/f\(x\)/);
  f.nodes['graphic-type'].value='SUPERFICIE';f.nodes['graphic-type'].listeners.change();f.nodes['graphic-function'].value='x*y';await f.submit('graphic-form');
  assert.ok(f.viewer.mesh); assert.equal(f.nodes['graphic-steps'].max,'128');
  f.nodes['graphic-function'].value='alert(1)';await f.submit('graphic-form');
  assert.match(f.nodes['graphic-result'].textContent,/inválida/);
  f.client.token=null;await f.ui.sync('tutor');
  assert.equal(f.viewer.cleared,true);assert.equal(f.nodes['tutor-history'].children.length,0);assert.equal(f.nodes['exercise-runs'].children.length,0);
});
test('una sesión cerrada admite consulta pero nunca mensajes, ejercicios ni gráficas',async()=>{
  const f=fixture();f.state.sessions[0].status='CERRADA';await f.open();
  assert.equal(f.nodes['tutor-composer'].hidden,true);assert.equal(f.nodes['exercise-fields'].disabled,true);
  await f.submit('exercise-form');await f.submit('graphic-form');f.nodes['tutor-message'].value='hola';await f.submit('tutor-form');
  assert.equal(f.state.calls.filter(([,o])=>o.method==='POST').length,0);
});
test('paginación de sesiones, turnos, banco y ejercicios se mantiene disponible',async()=>{
  const f=fixture();f.state.sessions=Array.from({length:21},(_,i)=>({id:i+1,module:'funciones',status:'ACTIVA'}));
  f.state.turns=Array.from({length:21},(_,i)=>({id:21-i,message:'paso '+i,response:'pista',status:'COMPLETADO'}));
  f.state.bank=Array.from({length:21},(_,i)=>({id:i+1,statement:'Ejercicio '+i,difficulty:1}));
  f.state.runs=Array.from({length:21},(_,i)=>({id:i+1,statement:'Ejercicio '+i,state:'COMPLETADO',progress:1}));
  await f.open();await f.click('sessions-more');assert.equal(f.nodes['tutor-sessions'].children.length,21);
  await f.click('history-more');assert.equal(f.nodes['tutor-history'].children.length,42);
  await f.click('bank-more');assert.equal(f.nodes['exercise-bank'].children.length,21);
  await f.click('runs-more');assert.equal(f.nodes['exercise-runs'].children.length,21);
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
  f.nodes['exercise-expression'].value='x^2';await f.submit('exercise-form');
  assert.equal(f.nodes['exercise-fields'].disabled,true);
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
