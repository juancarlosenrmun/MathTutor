import { renderTutorMarkdown } from './tutor-markdown.mjs';
import { request, collection, readEvents } from './tutor-api.mjs';
import { appendTurn, renderTurns } from './tutor-history.mjs';
import { mountSessions } from './tutor-sessions.mjs';
import { mountStudy } from './study.mjs';

export async function readReply(response,onToken) {
  return readEvents(response,event=>{
    if (event.type!=='TOKEN' || typeof event.content!=='string') throw new Error('Respuesta del tutor inválida.');
    onToken(event.content);
  });
}

/** Conversación persistente con cancelación, UUID por envío y aislamiento de identidad. */
export function mountTutor(document,client,{viewer,randomUUID=()=>globalThis.crypto.randomUUID()}={}) {
  const one=id=>document.querySelector('#'+id), history=one('tutor-history'), status=one('tutor-status');
  const noteQuestions={funciones:'¿Qué es la continuidad de una función?',
    'derivadas-integrales':'¿Cómo se calcula la derivada de una función?', 'metodos-3d':'¿Qué es un espacio vectorial?'};
  let session,identity,controller,generation=0,busy=false,loaded=false,loading=false,pending;
  let modules=[],entries=[],historyPage=0;
  const allowed=()=>Boolean(client.token) && ['ALUMNO','PROFESOR'].includes(client.profile?.role);
  const writable=()=>allowed() && session && session.status!=='CERRADA';
  const titleFor=visit=>visit.title || modules.find(m=>m.code===(visit.moduleCode || visit.module))?.name || 'Conversación de Matemáticas';
  const study=mountStudy(document,client,viewer);
  const visits=mountSessions(document,client,{isBusy:()=>busy || !allowed(),onOpen:open,titleFor});
  function controls() {
    one('tutor-composer').hidden=!writable(); one('tutor-send').disabled=busy || Boolean(pending);
    one('tutor-start').disabled=busy || !allowed() || !loaded; one('tutor-block').disabled=busy || !allowed() || !loaded;
    one('tutor-cancel').disabled=!busy;
    if (one('tutor-retry')) { one('tutor-retry').hidden=!pending; one('tutor-retry').disabled=busy; }
    if (one('tutor-topic-picker')) one('tutor-topic-picker').hidden=Boolean(session);
    if (one('tutor-chat-title')) one('tutor-chat-title').hidden=!session;
    if (one('tutor-viewer-slot')) one('tutor-viewer-slot').hidden=!session;
    study.setBusy(busy);
  }
  async function loadHistory(more=false) {
    const current=generation, id=session.id;
    const result=await collection(client,`/api/tutor/sessions/${id}/history?page=${more ? historyPage+1 : 0}&size=20`,controller?.signal);
    if (current!==generation) return;
    historyPage=more ? historyPage+1 : 0;
    entries=[...new Map((more ? [...entries,...result] : result).map(e=>[e.id,e])).values()];
    renderTurns(document,history,entries);
    if (one('history-more')) one('history-more').hidden=result.length<20;
    if (pending && entries.some(e=>e.requestId===pending.requestId && e.status!=='EN_CURSO')) pending=undefined;
  }
  async function open(visit) {
    if (busy || !allowed()) return;
    controller?.abort(); generation++; const current=generation; busy=true; controller=new AbortController();
    session={...visit,moduleCode:visit.moduleCode || visit.module}; pending=undefined; entries=[]; historyPage=0;
    study.enter(null);
    if (one('history-more')) one('history-more').hidden=true;
    history.replaceChildren(...(visit.messages || []));
    one('tutor-chat-title').textContent=titleFor(visit); visits.select(visit.id); controls();
    status.textContent='Recuperando conversación…';
    try {
      await loadHistory(); if (current!==generation) return;
      await study.enter(session,entries);
      if (current===generation) status.textContent=session.status==='CERRADA' ? 'Conversación cerrada. Puedes consultar sus mensajes y ejercicios.' : 'Conversación recuperada. Puedes continuar preguntando.';
    } catch { if (current===generation) {
      session=null;history.replaceChildren();visits.select(undefined);
      status.textContent='No se pudo recuperar el historial. Actualiza la lista y vuelve a abrir la conversación.';
    } }
    finally { if (current===generation) {busy=false;controls();} }
  }
  async function sync(view) {
    const next=allowed() ? client.profile.email : null;
    if (identity!==next) {
      controller?.abort(); identity=next; generation++; session=null; pending=undefined; busy=false; loaded=false; loading=false;
      one('tutor-message').value=''; status.textContent='Inicia sesión como alumno para conversar con el tutor.';
      history.replaceChildren(); entries=[]; modules=[]; visits.clear(); study.enter(null); controls();
      if (one('history-more')) one('history-more').hidden=true;
    }
    if (view!=='tutor') {
      if (busy) {controller.abort();generation++;busy=false;loading=false;status.textContent='Operación cancelada al salir de la conversación.';controls();}
      study.suspend?.(); return;
    }
    study.resume?.();
    if (!allowed() || loaded || loading) return;
    loading=true; const current=generation; status.textContent='Cargando bloques temáticos…';
    try {
      modules=await collection(client,'/api/tutor/modules'); if (current!==generation) return;
      one('tutor-block').replaceChildren(); one('tutor-topics')?.replaceChildren();
      for (const module of modules) {
        const option=document.createElement('option'); option.value=module.code; option.textContent=module.name; one('tutor-block').append(option);
        if (one('tutor-topics')) {
          const button=document.createElement('button'); button.type='button'; button.className='tutor-topic'; button.textContent=module.name;
          button.addEventListener('click',()=>{one('tutor-block').value=module.code;one('tutor-select-form').requestSubmit();}); one('tutor-topics').append(button);
        }
      }
      loaded=true; status.textContent='Elige un bloque para empezar una conversación.'; await visits.load();
    } catch { if (current===generation) status.textContent='No se pudieron cargar los módulos. Vuelve a entrar para reintentar.'; }
    finally {if (current===generation) {loading=false;controls();}}
  }
  one('tutor-select-form').addEventListener('submit',async event=>{
    event.preventDefault(); if (busy || !allowed() || !loaded) return;
    const current=generation; busy=true; controller=new AbortController(); controls();
    try {
      const created=await (await request(client,'/api/tutor/sessions',{moduleCode:one('tutor-block').value},controller.signal)).json();
      if (current!==generation) return;
      session={...created,status:created.status || 'ACTIVA'}; session.title=titleFor(session); entries=[]; pending=undefined;
      visits.add(session); history.replaceChildren(); one('tutor-chat-title').textContent=session.title;
      if (one('history-more')) one('history-more').hidden=true;
      await study.enter(session);
      if (current===generation) {one('tutor-message').value='';status.textContent='Conversación iniciada. Escribe tu pregunta.';one('tutor-message').focus();}
    } catch {if (current===generation) status.textContent='No se pudo iniciar la conversación.';}
    finally {if (current===generation) {busy=false;controls();}}
  });
  async function send(retry=false) {
    const message=retry ? pending?.message : one('tutor-message').value;
    if (!writable() || busy || (!retry && pending) || !message?.trim() || message.length>4000) return;
    const current=generation; busy=true; controller=new AbortController();
    const prior=retry ? pending : null;
    const bubbles=prior && [...history.children].includes(prior.answer) ? prior : appendTurn(document,history,message);
    pending={...bubbles,message,requestId:prior?.requestId || randomUUID()}; controls();
    const requestId=pending.requestId; let markdown='';
    renderTutorMarkdown(document,pending.body,''); status.textContent=retry ? 'Recuperando el envío sin iniciar otro turno…' : 'El tutor está respondiendo…';
    try {
      if (!retry) await study.fromMessage?.(message);
      if (current!==generation) return;
      const response=await request(client,`/api/tutor/sessions/${session.id}/messages`,{message,requestId},controller.signal);
      await readReply(response,token=>{if (current===generation) {markdown+=token;renderTutorMarkdown(document,bubbles.body,markdown);}});
      if (current===generation) {pending=undefined;one('tutor-message').value='';status.textContent='Respuesta completada.';}
    } catch (error) {
      if (current===generation) {
        if ([400,401,403,404].includes(error.status)) {pending=undefined;status.textContent=error.message;}
        else status.textContent=controller.signal.aborted ? 'Respuesta cancelada. Comprueba el historial antes de enviar otra pregunta.' : 'No se pudo completar la respuesta. Recupera el envío pendiente antes de continuar.';
      }
    } finally {
      if (current===generation) {
        try {
          const latest=await collection(client,`/api/tutor/sessions/${session.id}/history?page=0&size=20`);
          if (current===generation && latest.length) {
            entries=latest; historyPage=0; renderTurns(document,history,entries);
            if (one('history-more')) one('history-more').hidden=latest.length<20;
            const terminal=latest.find(e=>e.requestId===requestId && e.status!=='EN_CURSO');
            if (pending && terminal) {
              pending=undefined;
              status.textContent=['ERROR','CANCELADO'].includes(terminal.status)
                ? 'El envío anterior ha terminado. Puedes enviar una pregunta nueva.' : 'Respuesta recuperada del historial.';
            }
            busy=false; await study.refresh(latest);
          }
        } catch { /* La respuesta visible se conserva; el alumno puede actualizar el historial. */ }
        if (current===generation) {busy=false;controls();}
      }
    }
  }
  one('tutor-form').addEventListener('submit',event=>{event.preventDefault();return send();});
  one('tutor-retry')?.addEventListener('click',()=>send(true));
  one('history-more')?.addEventListener('click',async()=>{
    if (busy || !session || !allowed()) return;
    const current=generation; busy=true; controller=new AbortController(); controls();
    try {await loadHistory(true);} catch {if (current===generation) status.textContent='No se pudieron cargar los mensajes anteriores. Vuelve a intentarlo.';}
    finally {if (current===generation) {busy=false;controls();}}
  });
  for (const button of document.querySelectorAll('[data-prompt]')) button.addEventListener('click',()=>{
    if (!writable() || busy) return;
    const prompt=button.dataset.prompt==='notes' ? noteQuestions[session.moduleCode] : button.dataset.prompt;
    if (prompt) {one('tutor-message').value=prompt;one('tutor-message').focus();}
  });
  one('tutor-cancel').addEventListener('click',()=>controller?.abort());
  one('tutor-new')?.addEventListener('click',()=>{
    if (busy || !allowed()) return;
    session=null;pending=undefined;entries=[];history.replaceChildren();study.enter(null);visits.select(undefined);
    if (one('history-more')) one('history-more').hidden=true;
    status.textContent='Elige un bloque para empezar una conversación.';controls();
  });
  controls(); return {sync};
}
