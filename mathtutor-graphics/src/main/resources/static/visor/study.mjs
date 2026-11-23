import { request, collection, readEvents } from './tutor-api.mjs';
import { drawSpecification } from './graphic-spec.mjs';

/** Estado interno del ejercicio y controles del visor de la conversación. */
export function mountStudy(document,client,viewer) {
  const one=id=>document.querySelector('#'+id);
  const form = one('graphic-form');
  if (!form) return {enter:async()=>{},setBusy(){}};
  const graphicType = one('graphic-type');
  const tangentX = one('graphic-tangent-x'), tangentY = one('graphic-tangent-y');
  let session, generation=0, controller, busy=false, chatBusy=false, bank=[], ownsGraph=false;
  const active=()=>Boolean(client.token) && ['ALUMNO','PROFESOR'].includes(client.profile?.role) && session?.status!=='CERRADA';
  function controls() {
    one('graphic-fields').disabled=!session || !active() || busy || chatBusy || !viewer;
  }
  async function perform(action,readOnly=false,errorTarget=one('graphic-result')) {
    if (!session || busy || (chatBusy && !readOnly)) return;
    const current=generation; busy=true; controls();
    try { await action(current); }
    catch (error) { if (current===generation) errorTarget.textContent=error.message; }
    finally { if (current===generation) { busy=false; controls(); } }
  }
  async function enter(next) {
    generation++; controller?.abort(); controller=new AbortController(); session=next; busy=false; bank=[];
    one('graphic-function').value='x^2';
    for (const field of [tangentX, tangentY]) {
      if (field) field.value = '0';
    }
    one('graphic-result').textContent=viewer ? 'Solicita una gráfica para explorarla en el visor.' : 'WebGL 2 no está disponible. Puedes continuar con el tutor.';
    if (ownsGraph) { viewer.clear(); ownsGraph=false; }
    controls();
    if (!session) return;
    await perform(async current=>{
      const result=await collection(client,`/api/tutor/sessions/${session.id}/bank?page=0&size=20`,controller.signal);
      if (current===generation) bank=result;
    },true);
  }
  form.addEventListener('submit',event=>{
    event.preventDefault(); if (!active()) return;
    return perform(async current=>{
      const type = graphicType.value;
      const points = ['RECTA_TANGENTE', 'PLANO_TANGENTE'].includes(type)
        ? [{ x: tangentX.valueAsNumber, y: type === 'PLANO_TANGENTE' ? tangentY.valueAsNumber : 0, z: 0 }]
        : [];
      const body={type,function:one('graphic-function').value,
        domain:{min:Number(one('graphic-min').value),max:Number(one('graphic-max').value),steps:Number(one('graphic-steps').value)},points};
      const response=await request(client,`/api/tutor/sessions/${session.id}/graphics`,body,controller.signal);
      let spec;
      await readEvents(response,event=>{
        if (event.type!=='GRAPHIC_SPEC' || spec) throw new Error('Especificación gráfica inesperada.');
        spec=event.spec;
      });
      if (current!==generation) return;
      one('graphic-result').textContent=drawSpecification(viewer,spec); ownsGraph=true;

    },false,one('graphic-result'));
  });
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
  });
  async function fromMessage(message) {
    if (!active() || !session) return;
    const match=message.trim().match(/^(?:deriva(?:r)?|simplifica(?:r)?)\s*:?\s+([x0-9+*^(). /-]{1,200})$/i);
    const exercise=bank.find(e=>e.statement.trim().toLocaleLowerCase('es')===message.trim().toLocaleLowerCase('es'));
    if (!match && !exercise) return;
    const body=exercise ? {exerciseId:exercise.id} : {operation:/^deriv/i.test(message.trim()) ? 'DERIVADA' : 'SIMPLIFICAR',expression:match[1]};
    await request(client,`/api/tutor/sessions/${session.id}/exercises`,body,controller.signal);
  }
  return {enter,fromMessage,setBusy(value){chatBusy=value;controls();},
    suspend(){generation++;controller?.abort();busy=false;controls();},
    resume(){if (controller?.signal.aborted) controller=new AbortController();}};
}
