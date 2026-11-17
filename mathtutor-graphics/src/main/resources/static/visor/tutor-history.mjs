import { renderTutorMarkdown } from './tutor-markdown.mjs';

const verdicts={CORRECTO:'Resultado o paso verificado',INCORRECTO:'Revisa el paso',INDETERMINADO:'No se ha podido comprobar la equivalencia',NO_APLICA:'Sin comprobación matemática'};
const states={COMPLETADO:'Respuesta completada',RECHAZADO:'Pregunta que necesita reformulación',ERROR:'Respuesta interrumpida por un error',CANCELADO:'Respuesta cancelada',EN_CURSO:'Respuesta en curso'};
export function turnDescription(entry) {
  return [states[entry.status] || 'Estado no disponible',verdicts[entry.verdict],
    entry.diagnosis==='SIGNO' ? 'Revisa el signo' : '',entry.topic ? 'Tema: '+entry.topic : ''].filter(Boolean).join('. ');
}
export function appendTurn(document,history,message,response='',entry) {
  const question=document.createElement('p'), answer=document.createElement('div');
  question.className='tutor-question'; question.textContent='Tú: '+message;
  answer.className='tutor-answer';
  const label=document.createElement('span'),body=document.createElement('div');
  label.className='tutor-answer-label'; label.textContent='Tutor:'; body.className='tutor-answer-body';
  answer.append(label,body); renderTutorMarkdown(document,body,response);
  if (entry) {
    const meta=document.createElement('p'); meta.className='turn-metadata'; meta.textContent=turnDescription(entry); answer.append(meta);
  }
  history.append(question); history.append(answer); return {body,answer};
}
export function renderTurns(document,history,entries) {
  history.replaceChildren();
  for (const entry of [...entries].reverse()) appendTurn(document,history,entry.message,entry.response || '',entry);
}
