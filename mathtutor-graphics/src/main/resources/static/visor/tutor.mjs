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
  let session, identity, controller, generation = 0, busy = false, loaded = false, loading = false;
  let visits = [], modules = [];
  const allowed = () => Boolean(client.token) && client.profile?.role === 'ALUMNO';
  function renderVisits() {
    const list = one('tutor-sessions');
    if (!list) return;
    list.replaceChildren();
    for (const visit of visits) {
      const button = document.createElement('button');
      button.type = 'button'; button.className = 'tutor-session-link';
      button.textContent = visit.title; button.setAttribute('aria-current', visit === session ? 'page' : 'false');
      button.addEventListener('click', () => {
        if (busy || !allowed()) return;
        if (session) session.messages = [...history.children];
        session = visit; history.replaceChildren(...visit.messages);
        one('tutor-chat-title').textContent = visit.title;
        status.textContent = 'Conversación recuperada. Puedes continuar preguntando.';
        controls(); renderVisits();
      });
      list.append(button);
    }
  }
  function showTopics() {
    if (one('tutor-topic-picker')) one('tutor-topic-picker').hidden = Boolean(session);
    if (one('tutor-chat-title')) one('tutor-chat-title').hidden = !session;
    if (one('tutor-viewer-slot')) one('tutor-viewer-slot').hidden = !session;
  }
  function controls() {
    one('tutor-composer').hidden = !session || !allowed();
    one('tutor-send').disabled = busy;
    one('tutor-start').disabled = busy || !allowed() || !loaded;
    one('tutor-block').disabled = busy || !allowed() || !loaded;
    one('tutor-cancel').disabled = !busy;
    showTopics();
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
      controller?.abort(); identity = next; generation++; session = null; busy = false; loaded = false; loading = false;
      one('tutor-message').value = '';
      status.textContent = 'Inicia sesión como alumno para conversar con el tutor.';
      history.replaceChildren(); visits = []; modules = []; renderVisits(); controls();
    }
    if (view !== 'tutor') {
      if (busy) {
        controller.abort(); generation++; busy = false; loading = false;
        status.textContent = 'Operación cancelada al salir de la conversación.'; controls();
      }
      return;
    }
    if (!allowed() || loaded || loading) return;
    loading = true; status.textContent = 'Cargando bloques temáticos…';
    const current = generation;
    try {
      const response = await request('/api/tutor/modules');
      if (!response.ok) throw new Error();
      modules = await response.json();
      if (current !== generation) return;
      one('tutor-block').replaceChildren();
      for (const module of modules) {
        const option = document.createElement('option'); option.value = module.code; option.textContent = module.name;
        one('tutor-block').append(option);
      }
      const topics = one('tutor-topics');
      if (topics) {
        topics.replaceChildren();
        for (const module of modules) {
          const button = document.createElement('button');
          button.type = 'button'; button.className = 'tutor-topic';
          button.textContent = module.name; button.disabled = !allowed();
          button.addEventListener('click', () => {
            one('tutor-block').value = module.code;
            one('tutor-select-form').requestSubmit();
          });
          topics.append(button);
        }
      }
      loaded = true; status.textContent = 'Elige un bloque para empezar una conversación.';
    } catch { if (current === generation) status.textContent = 'No se pudieron cargar los módulos. Vuelve a entrar para reintentar.'; }
    finally { if (current === generation) { loading = false; controls(); } }
  }
  one('tutor-select-form').addEventListener('submit', async event => {
    event.preventDefault(); if (busy || !allowed() || !loaded) return;
    const current = generation;
    busy = true; controller = new AbortController(); controls();
    try {
      const response = await request('/api/tutor/sessions', {moduleCode:one('tutor-block').value}, controller.signal);
      if (!response.ok) throw new Error();
      const created = await response.json();
      if (current !== generation) return;
      if (session) session.messages = [...history.children];
      session = {...created, title: modules.find(module => module.code === created.moduleCode)?.name || one('tutor-block').selectedOptions?.[0]?.textContent || 'Nueva conversación', messages: []};
      visits.unshift(session); history.replaceChildren(); renderVisits();
      one('tutor-message').value = '';
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
    question.className = 'tutor-question'; answer.className = 'tutor-answer';
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
  one('tutor-new')?.addEventListener('click', () => {
    if (busy || !allowed()) return;
    if (session) session.messages = [...history.children];
    session = null; history.replaceChildren(); status.textContent = 'Elige un bloque para empezar una conversación.';
    controls(); renderVisits();
  });
  controls();
  return { sync };
}
