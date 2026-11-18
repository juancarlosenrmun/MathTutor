import { collection } from './tutor-api.mjs';

/** Listado paginado de conversaciones propias; los datos tardíos se descartan al cambiar de cuenta. */
export function mountSessions(document,client,{isBusy,onOpen,titleFor}) {
  const one=id=>document.querySelector('#'+id), list=one('tutor-sessions');
  let visits=[], page=0, selected, generation=0, controller, loading=false;
  function render() {
    list?.replaceChildren();
    for (const visit of visits) {
      const button=document.createElement('button'); button.type='button'; button.className='tutor-session-link';
      button.textContent=`${titleFor(visit)}${visit.status==='CERRADA' ? ' (cerrada)' : ''}`;
      button.setAttribute('aria-current',visit.id===selected ? 'page' : 'false');
      button.addEventListener('click',()=>{if (!isBusy()) return onOpen(visit);}); list.append(button);
    }
  }
  async function load(more=false) {
    if (loading || !client.token || !['ALUMNO','PROFESOR'].includes(client.profile?.role)) return;
    const current=generation; loading=true; controller=new AbortController();
    if (one('sessions-status')) one('sessions-status').textContent='Cargando conversaciones guardadas…';
    try {
      const result=await collection(client,`/api/tutor/progress?page=${more ? page+1 : 0}&size=20`,controller.signal);
      if (current!==generation) return;
      page=more ? page+1 : 0;
      visits=[...new Map((more ? [...visits,...result] : result).map(v=>[v.id,v])).values()];
      render();
      if (one('sessions-more')) one('sessions-more').hidden=result.length<20;
      if (one('sessions-status')) one('sessions-status').textContent=visits.length ? 'Elige una conversación para continuar o consultar su historial.' : 'Todavía no tienes conversaciones guardadas.';
    } catch { if (current===generation && one('sessions-status')) one('sessions-status').textContent='No se pudo cargar el historial. Pulsa Actualizar historial para reintentar.'; }
    finally { if (current===generation) loading=false; }
  }
  one('sessions-more')?.addEventListener('click',()=>{if (!isBusy()) return load(true);});
  one('sessions-refresh')?.addEventListener('click',()=>{if (!isBusy()) return load();});
  return {load,select(id){selected=id;render();},add(visit){visits=[visit,...visits.filter(v=>v.id!==visit.id)];selected=visit.id;render();},
    clear(){generation++;controller?.abort();loading=false;visits=[];selected=undefined;render();
      if (one('sessions-more')) one('sessions-more').hidden=true;
      if (one('sessions-status')) one('sessions-status').textContent='Tus conversaciones se guardan en tu cuenta.';}};
}
