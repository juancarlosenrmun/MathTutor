import { request, collection, readEvents } from './tutor-api.mjs';
import { drawSpecification, specificationFromChat } from './graphic-spec.mjs';
import { renderExerciseText } from './tutor-markdown.mjs';
import { bindTangencyValue, configureTangencyRange } from './tangents.mjs';

/** Estado interno del ejercicio y controles del visor de la conversación. */
export function mountStudy(document,client,viewer,onGraph) {
  const one=id=>document.querySelector('#'+id);
  const graphicStatus = one('tutor-graphic-status');
  if (!graphicStatus) return {enter: async () => {}, setBusy() {}};
  const liveControls = one('viewer-tangent-controls');
  const liveX = one('viewer-tangent-x'), liveY = one('viewer-tangent-y');
  const showLiveX = bindTangencyValue(liveX, one('viewer-tangent-x-value'));
  const viewerResult = one('viewer-tangent-result');
  const bankPanel = one('exercise-bank');
  const bankSelect = one('exercise-bank-select');
  const bankPreview = one('exercise-bank-preview');
  const bankUse = one('exercise-bank-use');
  let selectedExercise, plotController;
  let moveTangency;
  let session, generation=0, controller, busy=false, chatBusy=false, bank=[], ownsGraph=false;
  const active=()=>Boolean(client.token) && ['ALUMNO','PROFESOR'].includes(client.profile?.role) && session?.status!=='CERRADA';
  function controls() {
    if (bankPanel) bankPanel.hidden = !session || !active() || !bank.length;
    if (bankUse) bankUse.disabled = busy || chatBusy;
  }
  async function loadBank(action) {
    const current=generation; busy=true; controls();
    try { await action(current); }
    catch { if (current===generation) bank=[]; }
    finally { if (current===generation) { busy=false; controls(); } }
  }
  function clearGraph() {
    plotController?.abort();
    moveTangency = undefined;
    graphicStatus.hidden = true;
    if (liveControls) liveControls.hidden = true;
    viewerResult.textContent = '';
    if (ownsGraph) viewer?.clear();
    ownsGraph = false;
    onGraph(false);
  }
  async function enter(next, entries = []) {
    generation++; controller?.abort(); controller=new AbortController(); session=next; busy=false; bank=[];
    selectedExercise = undefined;
    bankSelect?.replaceChildren();
    bankPreview?.replaceChildren();
    clearGraph();
    controls();
    if (!session || !active()) return;
    const lastRun = entries.find(entry => entry.runId)?.runId;
    const graphic = entries.find(entry => entry.status === 'COMPLETADO' && (!lastRun || entry.runId === lastRun)
      && specificationFromChat(entry.message, entry.response || ''));
    if (graphic) void fromReply(graphic.message, graphic.response || '');
    await loadBank(async current=>{
      const result=await collection(client,`/api/tutor/sessions/${session.id}/bank?page=0&size=20`,controller.signal);
      if (current!==generation) return;
      bank = result;
      if (!bankSelect) return;
      for (const [index, exercise] of bank.entries()) {
        const option = document.createElement('option');
        option.value = String(exercise.id);
        const kind = {PROCEDIMENTAL:'Procedimental', TEORICO:'Conceptual', INTERPRETACION:'Interpretación'}[exercise.type];
        option.textContent = kind ? `Ejercicio ${index + 1} · ${kind}` : `Ejercicio ${index + 1}`;
        bankSelect.append(option);
      }
      showExercise();
    });
  }
  function showExercise() {
    const exercise = bank.find(item => String(item.id) === bankSelect.value);
    if (bankPreview && exercise) renderExerciseText(document, bankPreview, exercise.statement);
  }
  bankSelect?.addEventListener('change', showExercise);
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
    moveTangency = undefined;
    if (liveControls) liveControls.hidden = true;
    const description = drawSpecification(viewer, spec, ([min, max], x, y, move) => {
      if (!liveControls) return;
      configureTangencyRange(liveX, min, max, x);
      configureTangencyRange(liveY, min, max, y);
      showLiveX();
      one('viewer-tangent-y-field').hidden = y === undefined;
      moveTangency = move;
      liveControls.hidden = false;
    });
    viewerResult.textContent = description;
    ownsGraph = Boolean(viewer);
    if (ownsGraph) onGraph(true);
  }
  const updateTangency = () => {
    if (!moveTangency) return;
    try { viewerResult.textContent = moveTangency(Number(liveX.value), Number(liveY.value)); }
    catch (error) { viewerResult.textContent = error.message; }
  };
  liveX?.addEventListener('input', updateTangency);
  liveY?.addEventListener('input', updateTangency);

  async function fromMessage(message) {
    if (!active() || !session) return;
    const match=message.trim().match(/^(?:deriva(?:r)?|simplifica(?:r)?)\s*:?\s+([x0-9+*^(). /-]{1,200})$/i);
    const exercise = selectedExercise?.statement === message ? selectedExercise :
      bank.find(e => e.statement.trim().toLocaleLowerCase('es') === message.trim().toLocaleLowerCase('es'));
    selectedExercise = undefined;
    if (!match && !exercise) return;
    const body=exercise ? {exerciseId:exercise.id} : {operation:/^deriv/i.test(message.trim()) ? 'DERIVADA' : 'SIMPLIFICAR',expression:match[1]};
    try {
      await request(client,`/api/tutor/sessions/${session.id}/exercises`,body,controller.signal);
    } catch (error) {
      error.exerciseSelection = true;
      throw error;
    }
  }
  async function fromReply(message, answer) {
    if (!session || !active()) return;
    const body = specificationFromChat(message, answer);
    if (!body) return;
    const current = generation;
    plotController?.abort();
    plotController = new AbortController();
    const {signal} = plotController;
    graphicStatus.hidden = true;
    try { await plot(body, current, signal); }
    catch (error) {
      if (current === generation && !signal.aborted) {
        graphicStatus.textContent = error.message;
        graphicStatus.hidden = false;
      }
    }
  }
  return {enter,fromMessage,fromReply,clearGraph,setBusy(value){chatBusy=value;controls();},
    suspend(){generation++;controller?.abort();busy=false;clearGraph();controls();}};
}
