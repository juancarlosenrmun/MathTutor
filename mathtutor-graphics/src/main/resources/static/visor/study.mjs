import { request, collection, readEvents } from './tutor-api.mjs';
import { drawSpecification } from './graphic-spec.mjs';
import { turnDescription } from './tutor-history.mjs';

/** Paneles de ejercicios, diagnóstico y gráficas vinculados a una conversación. */
export function mountStudy(document,client,viewer) {
  const one=id=>document.querySelector('#'+id);
  if (!one('tutor-study')) return {enter:async()=>{},refresh:async()=>{},setBusy(){}};
  let session, generation=0, controller, busy=false, chatBusy=false, bank=[], bankPage=0, runPage=0, runs=[], ownsGraph=false;
  const active=()=>Boolean(client.token) && client.profile?.role==='ALUMNO' && session?.status!=='CERRADA';
  const status=one('exercise-status');
  function controls() {
    one('tutor-study').hidden=!session;
    one('exercise-fields').disabled=!session || !active() || busy || chatBusy;
    one('graphic-fields').disabled=!session || !active() || busy || chatBusy || !viewer;
    one('bank-more').disabled=busy || chatBusy;
    one('runs-more').disabled=busy || chatBusy;
    one('exercise-bank-group').hidden=one('exercise-kind').value!=='BANCO';
    one('exercise-expression-group').hidden=one('exercise-kind').value==='BANCO';
    one('exercise-expression').required=one('exercise-kind').value!=='BANCO';
    one('exercise-bank').required=one('exercise-kind').value==='BANCO';
  }
  function renderRuns() {
    one('exercise-runs').replaceChildren();
    for (const run of runs) {
      const item=document.createElement('li');
      const label={COMPLETADO:'Completado',COMENZADO:'En curso',ABANDONADO:'Abandonado'}[run.state] || 'Estado desconocido';
      item.textContent=`${run.statement} — ${label}, ${Math.round(run.progress*100)} %${run.topic ? '. Tema: '+run.topic : ''}`;
      one('exercise-runs').append(item);
    }
    const current=runs.find(r=>r.state==='COMENZADO') || runs[0];
    one('exercise-progress').value=current?.progress || 0;
    one('exercise-summary').textContent=current ? `${current.statement}: ${Math.round(current.progress*100)} % verificado.` : 'Todavía no has empezado un ejercicio en esta conversación.';
  }
  async function loadRuns(append=false) {
    const current=generation;
    const result=await collection(client,`/api/tutor/sessions/${session.id}/exercises?page=${append ? runPage+1 : 0}&size=20`,controller.signal);
    if (current!==generation) return;
    runPage=append ? runPage+1 : 0; runs=append ? [...runs,...result] : result;
    one('runs-more').hidden=result.length<20; renderRuns();
  }
  async function loadBank(append=false) {
    const current=generation;
    const result=await collection(client,`/api/tutor/sessions/${session.id}/bank?page=${append ? bankPage+1 : 0}&size=20`,controller.signal);
    if (current!==generation) return;
    bankPage=append ? bankPage+1 : 0; bank=append ? [...bank,...result] : result;
    const selected=one('exercise-bank').value; one('exercise-bank').replaceChildren();
    for (const entry of bank) {
      const option=document.createElement('option'); option.value=String(entry.id);
      option.textContent=`${entry.statement} (nivel ${entry.difficulty})`; one('exercise-bank').append(option);
    }
    if (bank.some(e=>String(e.id)===selected)) one('exercise-bank').value=selected;
    one('bank-empty').hidden=bank.length>0; one('bank-more').hidden=result.length<20;
  }
  async function perform(action,readOnly=false,errorTarget=status) {
    if (!session || busy || (chatBusy && !readOnly)) return;
    const current=generation; busy=true; controls();
    try { await action(current); }
    catch (error) { if (current===generation) errorTarget.textContent=error.message; }
    finally { if (current===generation) { busy=false; controls(); } }
  }
  async function refresh(entries) {
    if (!session) return;
    if (entries) one('tutor-diagnosis').textContent=entries.length ? turnDescription(entries[0]) : 'Aún no hay respuestas guardadas.';
    await perform(async current=>{ await loadRuns(); if (current===generation) status.textContent='Progreso actualizado.'; },true);
  }
  async function enter(next,entries=[]) {
    generation++; controller?.abort(); controller=new AbortController(); session=next; busy=false;
    bank=[]; runs=[]; bankPage=runPage=0;
    one('exercise-runs').replaceChildren(); one('exercise-bank').replaceChildren();
    one('exercise-expression').value=''; one('graphic-function').value='x^2';
    one('tutor-diagnosis').textContent=entries.length ? turnDescription(entries[0]) : 'Aún no hay respuestas guardadas.';
    one('graphic-result').textContent=viewer ? 'Solicita una gráfica para explorarla en el visor.' : 'WebGL 2 no está disponible. Puedes continuar con el tutor.';
    if (ownsGraph) { viewer.clear(); ownsGraph=false; }
    one('bank-more').hidden=true; one('runs-more').hidden=true; renderRuns(); controls();
    if (!session) { status.textContent=''; return; }
    await perform(async current=>{
      await Promise.all([loadBank(),loadRuns()]);
      if (current===generation) status.textContent='Selecciona un ejercicio o plantea una operación libre.';
    },true);
  }
  one('exercise-kind').addEventListener('change',controls);
  one('bank-more').addEventListener('click',()=>perform(()=>loadBank(true)));
  one('runs-more').addEventListener('click',()=>perform(()=>loadRuns(true)));
  one('exercise-form').addEventListener('submit',event=>{
    event.preventDefault(); if (!active()) return;
    return perform(async current=>{
      const kind=one('exercise-kind').value;
      const body=kind==='BANCO' ? {exerciseId:Number(one('exercise-bank').value)} : {operation:kind,expression:one('exercise-expression').value};
      if (kind==='BANCO' && !bank.some(e=>e.id===body.exerciseId)) throw new Error('Selecciona un ejercicio del banco.');
      await request(client,`/api/tutor/sessions/${session.id}/exercises`,body,controller.signal);
      if (current!==generation) return;
      await loadRuns();
      if (current===generation) { status.textContent='Ejercicio iniciado. Escribe tu siguiente paso en el chat.'; one('tutor-message').focus(); }
    });
  });
  one('graphic-form').addEventListener('submit',event=>{
    event.preventDefault(); if (!active()) return;
    return perform(async current=>{
      const body={type:one('graphic-type').value,function:one('graphic-function').value,
        domain:{min:Number(one('graphic-min').value),max:Number(one('graphic-max').value),steps:Number(one('graphic-steps').value)},points:[]};
      const response=await request(client,`/api/tutor/sessions/${session.id}/graphics`,body,controller.signal);
      let spec;
      await readEvents(response,event=>{
        if (event.type!=='GRAPHIC_SPEC' || spec) throw new Error('Especificación gráfica inesperada.');
        spec=event.spec;
      });
      if (current!==generation) return;
      one('graphic-result').textContent=drawSpecification(viewer,spec); ownsGraph=true;
      status.textContent='Gráfica validada y dibujada.';
    },false,one('graphic-result'));
  });
  one('graphic-type').addEventListener('change',()=>{
    const surface=one('graphic-type').value==='SUPERFICIE';
    one('graphic-function').maxLength=surface ? 150 : 200;
    one('graphic-steps').max=surface ? '128' : '200';
    for (const id of ['graphic-min','graphic-max']) { one(id).min=surface ? '-100' : '-10000'; one(id).max=surface ? '100' : '10000'; }
  });
  return {enter,refresh,setBusy(value){chatBusy=value;controls();},
    suspend(){generation++;controller?.abort();busy=false;controls();},
    resume(){if (controller?.signal.aborted) controller=new AbortController();}};
}
