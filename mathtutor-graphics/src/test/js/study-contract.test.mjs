import test from 'node:test';
import assert from 'node:assert/strict';
import { request, readEvents } from '../../main/resources/static/visor/tutor-api.mjs';
import { drawSpecification } from '../../main/resources/static/visor/graphic-spec.mjs';

const stream = events => ({ok:true,body:new ReadableStream({start(c) {
  c.enqueue(new TextEncoder().encode(events.map(event => 'data:'+JSON.stringify(event)+'\n\n').join(''))); c.close();
}})});
test('peticiones de alumno incluyen autenticación y CSRF solo en escritura', async () => {
  const calls=[];
  const client={token:'jwt',fetch:async (path,options) => { calls.push([path,options]); return {ok:true,json:async()=>({csrfToken:'csrf'})}; }};
  await request(client,'/api/tutor/progress');
  await request(client,'/api/tutor/sessions',{moduleCode:'funciones'});
  assert.equal(calls[0][1].method,'GET');
  assert.equal(calls[2][1].headers['X-XSRF-TOKEN'],'csrf');
  assert.equal(calls[2][1].headers.Authorization,'Bearer jwt');
});
test('conflicto y cambios de identidad no se convierten en respuestas válidas', async () => {
  const client={token:'jwt',fetch:async()=>({ok:false,status:409})};
  await assert.rejects(request(client,'/x'),e=>e.status===409);
  client.fetch=async()=>{client.token='otro';return {ok:true};};
  await assert.rejects(request(client,'/x'),/sesión/);
});
test('el SSE gráfico solo completa con DONE y conserva la especificación', async () => {
  const events=[];
  await readEvents(stream([{type:'GRAPHIC_SPEC',spec:{function:'x'}},{type:'DONE'}]),e=>events.push(e));
  assert.equal(events[0].spec.function,'x');
  await assert.rejects(readEvents(stream([{type:'GRAPHIC_SPEC'}]),()=>{}));
  await assert.rejects(readEvents(stream([{type:'ERROR'}]),()=>{}));
});
test('el intérprete dibuja curva o superficie mediante los parsers existentes', () => {
  const viewer={setGraph(graph){this.graph=graph;},setSurface(mesh){this.mesh=mesh;}};
  const spec={type:'CURVA_2D',function:'1E-2*x^2',domain:{min:-1,max:1,steps:2},points:[]};
  assert.match(drawSpecification(viewer,spec),/1E-2\*x\^2/);
  assert.equal(viewer.graph.points.length,17);
  drawSpecification(viewer,{...spec,type:'SUPERFICIE',function:'x*y'});
  assert.equal(viewer.mesh.triangles.length,8);
});
test('la especificación remota nunca autoriza código ni geometría sin límites', () => {
  const viewer={setGraph(){throw new Error('no debe dibujar');}};
  const good={type:'CURVA_2D',function:'x',domain:{min:-1,max:1,steps:20},points:[]};
  for (const bad of [null,{}, {...good,type:'PLANO_TANGENTE'}, {...good,function:'alert(1)'},
    {...good,domain:{min:1,max:1,steps:20}}, {...good,domain:{min:NaN,max:1,steps:20}},
    {...good,domain:{min:-1,max:1,steps:201}}, {...good,points:[null]},
    {...good,type:'SUPERFICIE',domain:{min:-101,max:1,steps:20}},
    {...good,type:'SUPERFICIE',domain:{min:-1,max:1,steps:129}}]) {
    assert.throws(()=>drawSpecification(viewer,bad));
  }
  assert.throws(()=>drawSpecification(null,good),/WebGL/);
});
