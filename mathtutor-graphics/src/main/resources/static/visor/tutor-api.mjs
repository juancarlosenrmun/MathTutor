/** Transporte del tutor: credenciales en memoria, CSRF y descarte tras cambio de identidad. */
export async function request(client, path, body, signal, method = body === undefined ? 'GET' : 'POST') {
  const token = client.token;
  if (!token) throw new Error('Tu sesión ha terminado. Inicia sesión de nuevo.');
  const headers = { Authorization: `Bearer ${token}` };
  if (method !== 'GET') {
    const csrf = await client.fetch('/api/csrf-token', {credentials:'same-origin', signal});
    if (!csrf.ok) throw new Error('No se pudo preparar la petición.');
    const value = (await csrf.json()).csrfToken;
    if (typeof value !== 'string' || !value) throw new Error('No se pudo preparar la petición.');
    headers['X-XSRF-TOKEN'] = value;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
  }
  if (token !== client.token) throw new Error('La sesión ha cambiado.');
  const response = await client.fetch(path, {method, headers,
    credentials:'same-origin', signal, ...(body === undefined ? {} : {body:JSON.stringify(body)})});
  if (token !== client.token) throw new Error('La sesión ha cambiado.');
  if (!response.ok) {
    const messages = {409:'Hay un turno en curso o el envío ya terminó. Recupera el historial antes de continuar.',
      401:'Tu sesión ha terminado. Inicia sesión de nuevo.',403:'No tienes permiso para esta operación.',
      404:'La conversación o el ejercicio no está disponible.',400:'Revisa la expresión y los valores introducidos.'};
    const error = new Error(messages[response.status] || 'No se pudo completar la petición. Vuelve a intentarlo.');
    error.status = response.status; throw error;
  }
  return response;
}

/** SSE de POST compartido por texto y gráficas; no reconecta ni reenvía peticiones. */
export async function readEvents(response, onEvent) {
  if (!response.ok || !response.body) throw new Error('No se pudo abrir la respuesta del tutor.');
  const reader=response.body.getReader(), decoder=new TextDecoder(); let pending='';
  try {
    while (true) {
      const {value,done}=await reader.read();
      if (done) throw new Error('La respuesta se interrumpió antes de terminar.');
      pending+=decoder.decode(value,{stream:true});
      if (pending.length>65536) throw new Error('La respuesta supera el límite permitido.');
      let delimiter;
      while ((delimiter=/\r?\n\r?\n/.exec(pending))) {
        const frame=pending.slice(0,delimiter.index); pending=pending.slice(delimiter.index+delimiter[0].length);
        const data=frame.split(/\r?\n/).filter(line=>line.startsWith('data:')).map(line=>line.slice(5).trim()).join('\n');
        if (!data) continue;
        const event=JSON.parse(data);
        if (event.type==='DONE') return;
        if (event.type==='ERROR') throw new Error('No se pudo completar la respuesta del tutor. Consulta el historial del envío.');
        onEvent(event);
      }
    }
  } finally { await reader.cancel().catch(()=>{}); reader.releaseLock(); }
}

export async function collection(client,path,signal) {
  const entries=await (await request(client,path,undefined,signal)).json();
  if (!Array.isArray(entries)) throw new Error('El servidor devolvió una lista inválida.');
  return entries;
}
