import { renderStudentMessage, renderTutorMarkdown } from './tutor-markdown.mjs';

export function appendTurn(document,history,message,response='') {
  const question=document.createElement('p'), answer=document.createElement('div');
  question.className='tutor-question'; renderStudentMessage(document,question,message);
  answer.className='tutor-answer';
  const label=document.createElement('span'),body=document.createElement('div');
  label.className='tutor-answer-label'; label.textContent='Tutor:'; body.className='tutor-answer-body';
  answer.append(label,body); renderTutorMarkdown(document,body,response);
  history.append(question); history.append(answer); return {body,answer};
}
export function renderTurns(document,history,entries) {
  history.replaceChildren();
  for (const entry of [...entries].reverse()) appendTurn(document,history,entry.message,entry.response || '');
}
