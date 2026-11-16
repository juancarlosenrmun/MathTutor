/** @brief Lee SSE de un POST sin reconectar ni volver a enviar el turno automáticamente. */
export async function readReply(response, onToken) {
  if (!response.ok || !response.body) throw new Error('No se pudo abrir la respuesta del tutor.');
  const reader = response.body.getReader(), decoder = new TextDecoder();
  let pending = '';
  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) throw new Error('La respuesta se interrumpió antes de terminar.');
      pending += decoder.decode(value, { stream: true });
      if (pending.length > 65536) throw new Error('La respuesta supera el límite permitido.');
      let delimiter;
      while ((delimiter = /\r?\n\r?\n/.exec(pending))) {
        const frame = pending.slice(0, delimiter.index);
        pending = pending.slice(delimiter.index + delimiter[0].length);
        const data = frame.split(/\r?\n/).filter(line => line.startsWith('data:')).map(line => line.slice(5).trim()).join('\n');
        if (!data) continue;
        const event = JSON.parse(data);
        if (event.type === 'DONE') return;
        if (event.type === 'ERROR') throw new Error('No se pudo completar la respuesta del tutor.');
        if (event.type !== 'TOKEN' || typeof event.content !== 'string') throw new Error('Respuesta del tutor inválida.');
        onToken(event.content);
      }
    }
  } finally { await reader.cancel().catch(() => {}); reader.releaseLock(); }
}

/** @brief UC-12/13: selección de bloque, historial accesible y cancelación sin HTML remoto. */
export function mountTutor(document, client) {
  const one = id => document.querySelector('#' + id);
  const status = one('tutor-status'), history = one('tutor-history');
  let session, identity, controller, generation = 0, busy = false, loaded = false;
  const allowed = () => Boolean(client.token) && client.profile?.role === 'ALUMNO';
  function controls() {
    one('tutor-composer').hidden = !session || !allowed();
    one('tutor-send').disabled = busy;
    one('tutor-start').disabled = busy || !allowed();
    one('tutor-cancel').disabled = !busy;
  }
  async function request(path, body, signal) {
    const token = client.token;
    const headers = { Authorization: `Bearer ${token}` };
    if (body) {
      const csrf = await client.fetch('/api/csrf-token', {credentials:'same-origin', signal});
      if (!csrf.ok) throw new Error('No se pudo preparar la petición.');
      headers['X-XSRF-TOKEN'] = (await csrf.json()).csrfToken;
      headers['Content-Type'] = 'application/json';
    }
    if (token !== client.token) throw new Error('La sesión ha cambiado.');
    return client.fetch(path, {method:body ? 'POST' : 'GET', headers, credentials:'same-origin',
      signal, ...(body ? {body:JSON.stringify(body)} : {})});
  }
  async function sync(view) {
    const next = allowed() ? client.profile.email : null;
    if (identity !== next) {
      controller?.abort(); identity = next; generation++; session = null; busy = false; loaded = false;
      history.replaceChildren(); controls();
    }
    if (view !== 'tutor') { controller?.abort(); return; }
    if (!allowed() || loaded) return;
    const current = generation;
    try {
      const response = await request('/api/tutor/modules');
      if (!response.ok) throw new Error();
      const modules = await response.json();
      if (current !== generation) return;
      one('tutor-block').replaceChildren();
      for (const module of modules) {
        const option = document.createElement('option'); option.value = module.code; option.textContent = module.name;
        one('tutor-block').append(option);
      }
      loaded = true; status.textContent = 'Elige un bloque para empezar una conversación.';
    } catch { if (current === generation) status.textContent = 'No se pudieron cargar los módulos. Vuelve a entrar para reintentar.'; }
  }
  one('tutor-select-form').addEventListener('submit', async event => {
    event.preventDefault(); if (busy || !allowed()) return;
    const current = generation;
    busy = true; controller = new AbortController(); controls();
    try {
      const response = await request('/api/tutor/sessions', {moduleCode:one('tutor-block').value}, controller.signal);
      if (!response.ok) throw new Error();
      const created = await response.json();
      if (current !== generation) return;
      session = created; history.replaceChildren();
      status.textContent = 'Conversación iniciada. Escribe tu pregunta.'; one('tutor-message').focus();
    } catch { if (current === generation) status.textContent = 'No se pudo iniciar la conversación.'; }
    finally { if (current === generation) { busy = false; controls(); } }
  });
  one('tutor-form').addEventListener('submit', async event => {
    event.preventDefault();
    const message = one('tutor-message').value;
    if (!session || busy || !allowed() || !message.trim() || message.length > 4000) return;
    const current = generation;
    busy = true; controller = new AbortController(); controls();
    const question = document.createElement('p'), answer = document.createElement('p');
    question.textContent = 'Tú: ' + message; answer.textContent = 'Tutor: ';
    history.append(question); history.append(answer); status.textContent = 'El tutor está respondiendo…';
    try {
      const response = await request(`/api/tutor/sessions/${session.id}/messages`, {message}, controller.signal);
      await readReply(response, token => { if (current === generation) answer.textContent += token; });
      if (current === generation) { one('tutor-message').value = ''; status.textContent = 'Respuesta completada.'; }
    } catch {
      if (current === generation) status.textContent = controller.signal.aborted ? 'Respuesta cancelada.' : 'No se pudo completar la respuesta. Puedes volver a intentarlo.';
    } finally { if (current === generation) { busy = false; controls(); } }
  });
  one('tutor-cancel').addEventListener('click', () => controller?.abort());
  controls();
  return { sync };
}
