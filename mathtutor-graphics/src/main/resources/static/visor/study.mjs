import { request, collection, readEvents } from './tutor-api.mjs';
import { drawSpecification, specificationFromChat } from './graphic-spec.mjs';
import { configureTangencyRange } from './tangents.mjs';

/** Estado interno del ejercicio y controles del visor de la conversación. */
export function mountStudy(document,client,viewer,onGraph) {
  const one=id=>document.querySelector('#'+id);
  const form = one('graphic-form');
  if (!form) return {enter:async()=>{},setBusy(){}};
  const graphicType = one('graphic-type');
  const tangentX = one('graphic-tangent-x'), tangentY = one('graphic-tangent-y');
  const liveControls = one('viewer-tangent-controls');
  const liveX = one('viewer-tangent-x'), liveY = one('viewer-tangent-y');
  const graphicResult = one('graphic-result');
  const viewerResult = one('viewer-tangent-result') ?? graphicResult;
  const bankPanel = one('exercise-bank');
  const bankSelect = one('exercise-bank-select');
  const bankUse = one('exercise-bank-use');
  let selectedExercise;
  let moveTangency;
  let session, generation=0, controller, busy=false, chatBusy=false, bank=[], ownsGraph=false;
  const active=()=>Boolean(client.token) && ['ALUMNO','PROFESOR'].includes(client.profile?.role) && session?.status!=='CERRADA';
  function controls() {
    one('graphic-fields').disabled=!session || !active() || busy || chatBusy;
    if (bankPanel) bankPanel.hidden = !session || !active() || !bank.length;
    if (bankUse) bankUse.disabled = busy || chatBusy;
  }
  async function perform(action,readOnly=false,errorTarget=one('graphic-result')) {
    if (!session || busy || (chatBusy && !readOnly)) return;
    const current=generation; busy=true; controls();
    try { await action(current); }
    catch (error) { if (current===generation) errorTarget.textContent=error.message; }
    finally { if (current===generation) { busy=false; controls(); } }
  }
  function clearGraph() {
    moveTangency = undefined;
    if (liveControls) liveControls.hidden = true;
    if (ownsGraph) viewer?.clear();
    ownsGraph = false;
    onGraph(false);
  }
  async function enter(next) {
    generation++; controller?.abort(); controller=new AbortController(); session=next; busy=false; bank=[];
    selectedExercise = undefined;
    bankSelect?.replaceChildren();
    one('graphic-function').value='';
    for (const field of [tangentX, tangentY]) {
      if (field) field.value = '0';
    }
    graphicResult.textContent='';
    viewerResult.textContent='';
    clearGraph();
    controls();
    if (!session || !active()) return;
    await perform(async current=>{
      const result=await collection(client,`/api/tutor/sessions/${session.id}/bank?page=0&size=20`,controller.signal);
      if (current!==generation) return;
      bank = result;
      if (!bankSelect) return;
      for (const exercise of bank) {
        const option = document.createElement('option');
        option.value = String(exercise.id);
        option.textContent = exercise.statement;
        bankSelect.append(option);
      }
    },true);
  }
  bankUse?.addEventListener('click', () => {
    if (!session || !active() || busy || chatBusy) return;
    selectedExercise = bank.find(exercise => String(exercise.id) === bankSelect.value);
    if (!selectedExercise) return;
    const message = one('tutor-message');
    message.value = selectedExercise.statement;
    message.focus();
  });
  async function plot(body, current, signal) {
    const response = await request(client, `/api/tutor/sessions/${session.id}/graphics`, body, signal);
    let spec;
    await readEvents(response, event => {
      if (event.type !== 'GRAPHIC_SPEC' || spec) throw new Error('Especificación gráfica inesperada.');
      spec = event.spec;
    });
    if (current !== generation || signal.aborted) return;
    const description = drawSpecification(viewer, spec, ([min, max], x, y, move) => {
      if (!liveControls) return;
      configureTangencyRange(liveX, min, max, x);
      configureTangencyRange(liveY, min, max, y);
      one('viewer-tangent-y-field').hidden = y === undefined;
      moveTangency = move;
      liveControls.hidden = false;
    });
    graphicResult.textContent = description;
    viewerResult.textContent = description;
    ownsGraph = Boolean(viewer);
    onGraph(true);
  }
  const updateTangency = () => {
    if (!moveTangency) return;
    try { viewerResult.textContent = moveTangency(Number(liveX.value), Number(liveY.value)); }
    catch (error) { viewerResult.textContent = error.message; }
  };
  liveX?.addEventListener('input', updateTangency);
  liveY?.addEventListener('input', updateTangency);

  form.addEventListener('submit',event=>{
    event.preventDefault(); if (!active()) return;
    return perform(async current=>{
      const type = graphicType.value;
      const points = ['RECTA_TANGENTE', 'PLANO_TANGENTE'].includes(type)
        ? [{ x: tangentX.valueAsNumber, y: type === 'PLANO_TANGENTE' ? tangentY.valueAsNumber : 0, z: 0 }]
        : [];
      const body={type,function:one('graphic-function').value,
        domain:{min:Number(one('graphic-min').value),max:Number(one('graphic-max').value),steps:Number(one('graphic-steps').value)},points};
      await plot(body, current, controller.signal);
    },false,one('graphic-result'));
  });
  function updateInitialTangency() {
    const min = Number(one('graphic-min').value), max = Number(one('graphic-max').value);
    if (!Number.isFinite(min) || !Number.isFinite(max) || min >= max) return;
    for (const field of [tangentX, tangentY]) {
      if (field && !field.disabled) configureTangencyRange(field, min, max, Number(field.value));
    }
  }
  one('graphic-min').addEventListener('input', updateInitialTangency);
  one('graphic-max').addEventListener('input', updateInitialTangency);
  graphicType.addEventListener('change',()=>{
    const type = graphicType.value;
    const surface = ['SUPERFICIE', 'PLANO_TANGENTE'].includes(type);
    const tangent = ['RECTA_TANGENTE', 'PLANO_TANGENTE'].includes(type);
    one('graphic-function').maxLength=surface ? 150 : 200;
    one('graphic-steps').max=surface ? '128' : '200';
    for (const id of ['graphic-min','graphic-max']) { one(id).min=surface ? '-100' : '-10000'; one(id).max=surface ? '100' : '10000'; }
    for (const [axis, field, visible] of [['x', tangentX, tangent], ['y', tangentY, tangent && surface]]) {
      if (!field) continue;
      one(`graphic-tangent-${axis}-field`).hidden = !visible;
      field.disabled = !visible;
      field.required = visible;
    }
    updateInitialTangency();
  });
  async function fromMessage(message) {
    if (!active() || !session) return;
    const match=message.trim().match(/^(?:deriva(?:r)?|simplifica(?:r)?)\s*:?\s+([x0-9+*^(). /-]{1,200})$/i);
    const exercise = selectedExercise?.statement === message ? selectedExercise :
      bank.find(e => e.statement.trim().toLocaleLowerCase('es') === message.trim().toLocaleLowerCase('es'));
    selectedExercise = undefined;
    if (!match && !exercise) return;
    const body=exercise ? {exerciseId:exercise.id} : {operation:/^deriv/i.test(message.trim()) ? 'DERIVADA' : 'SIMPLIFICAR',expression:match[1]};
    await request(client,`/api/tutor/sessions/${session.id}/exercises`,body,controller.signal);
  }
  return {enter,fromMessage,setBusy(value){chatBusy=value;controls();},
    async fromReply(message, answer, signal) {
      if (!session || !active() || signal.aborted) return;
      const current = generation;
      const body = specificationFromChat(message, answer);
      if (!body) return;
      try { await plot(body, current, signal); }
      catch (error) { if (current === generation && !signal.aborted) graphicResult.textContent = error.message; }
    },
    suspend(){generation++;controller?.abort();busy=false;clearGraph();controls();},
    resume(){if (controller?.signal.aborted) controller=new AbortController();}};
}
