const PAGE_SIZE = 20;
const sessionDate = new Intl.DateTimeFormat('es', { dateStyle: 'short', timeStyle: 'short' });
const confirmations = {
  role: '¿Cambiar el rol del perfil seleccionado?',
  'password-reset': '¿Enviar un enlace de recuperación al perfil seleccionado?',
  deactivate: '¿Desactivar este perfil y cerrar sus sesiones?',
  anonymize: '¿Anonimizar este perfil y borrar sus datos personales? Esta acción es irreversible.',
};

/** @brief Gestiona la lista y las acciones del administrador, con autorización en cada solicitud. */
export function mountAdministration(document, host, client) {
  const one = selector => document.querySelector(selector);
  const status = one('#admin-status');
  const user = one('#admin-user');
  const role = one('#admin-role');
  const actions = one('#admin-actions');
  const loadButton = one('#admin-load');
  const previous = one('#admin-previous');
  const next = one('#admin-next');
  const topicUsage = mountUsageTable(document, client, 'topics',
    item => [item.module, item.topic, item.sessions, item.turns], 'Todavía no hay uso registrado por tema.');
  const sessionUsage = mountUsageTable(document, client, 'sessions',
    item => [item.id, item.module ?? 'Sin módulo', item.status === 'ACTIVA' ? 'Activa' : 'Cerrada',
      sessionDate.format(new Date(item.startedAt)), sessionDate.format(new Date(item.lastActivityAt))],
    'No hay sesiones en esta página.');
  let page = 0, generation = 0, identity, profiles = [];

  function sync(view) {
    const current = view === 'espacio' && client.profile?.role === 'ADMIN' ? client.profile.email : null;
    if (current === identity) return;
    identity = current;
    generation++;
    page = 0;
    profiles = [];
    user.replaceChildren();
    actions.disabled = previous.disabled = next.disabled = true;
    loadButton.disabled = false;
    status.textContent = '';
    topicUsage.reset(current ? '/api/admin/usage/topics' : null);
    sessionUsage.reset();
  }

  async function load(targetPage, message) {
    const current = ++generation;
    sessionUsage.reset();
    actions.disabled = loadButton.disabled = previous.disabled = next.disabled = true;
    try {
      const response = await client.request(`/api/admin/users?page=${targetPage}&size=${PAGE_SIZE}`);
      const list = await response.json();
      if (current !== generation) return;
      profiles = list.filter(profile => profile.id !== client.profile.id);
      const options = profiles.map(profile => {
        const option = document.createElement('option');
        option.value = String(profile.id);
        option.textContent = `${profile.email}${profile.active ? '' : ' (desactivado)'}`;
        return option;
      });
      page = targetPage;
      user.replaceChildren(...options);
      actions.disabled = profiles.length === 0;
      previous.disabled = page === 0;
      next.disabled = list.length < PAGE_SIZE;
      setRole();
      status.textContent = message || (profiles.length ? `Página ${page + 1}` : 'No hay perfiles en esta página.');
    } catch (error) {
      if (current === generation) status.textContent = error.message;
    } finally {
      if (current === generation) loadButton.disabled = false;
    }
  }

  function setRole() {
    const profile = profiles.find(item => String(item.id) === user.value);
    if (profile) role.value = profile.role;
    sessionUsage.reset(profile ? `/api/admin/users/${profile.id}/sessions` : null);
    for (const button of document.querySelectorAll('[data-admin-action]')) {
      button.disabled = !profile || (!profile.active && button.dataset.adminAction !== 'anonymize');
    }
  }

  user.addEventListener('change', setRole);
  loadButton.addEventListener('click', () => load(0));
  previous.addEventListener('click', () => load(page - 1));
  next.addEventListener('click', () => load(page + 1));
  for (const button of document.querySelectorAll('[data-admin-action]')) {
    button.addEventListener('click', async () => {
      const action = button.dataset.adminAction;
      if (!user.value || !host.confirm(confirmations[action])) return;
      const current = generation;
      actions.disabled = true;
      try {
        await client.request(`/api/admin/users/${user.value}/${action}`, {
          method: action === 'password-reset' ? 'POST' : 'PUT',
          body: action === 'role' ? { role: role.value } : undefined,
        });
        if (current === generation) await load(page, action === 'password-reset'
          ? 'Se ha solicitado el envío del enlace.' : 'Se ha actualizado el perfil.');
      } catch (error) {
        if (current === generation) { status.textContent = error.message; actions.disabled = false; }
      }
    });
  }
  return { sync };
}

/** @brief Muestra una página de metadatos y descarta respuestas de una selección anterior. */
function mountUsageTable(document, client, name, values, emptyMessage) {
  const one = id => document.querySelector(`#admin-${name}-${id}`);
  const loadButton = one('load');
  const previous = one('previous');
  const next = one('next');
  const status = one('status');
  const results = one('results');
  const table = one('table');
  let path, page, generation = 0;

  function reset(endpoint = null) {
    generation++;
    path = endpoint;
    page = 0;
    loadButton.disabled = !path;
    previous.disabled = next.disabled = true;
    results.replaceChildren();
    table.hidden = true;
    status.textContent = '';
  }

  async function load(targetPage) {
    if (!path) return;
    const current = ++generation;
    loadButton.disabled = previous.disabled = next.disabled = true;
    try {
      const { items, hasNext } = await (await client.request(`${path}?page=${targetPage}&size=${PAGE_SIZE}`)).json();
      if (current !== generation) return;
      results.replaceChildren(...items.map(item => {
        const row = document.createElement('tr');
        for (const value of values(item)) {
          const cell = document.createElement('td');
          cell.textContent = String(value);
          row.append(cell);
        }
        return row;
      }));
      page = targetPage;
      previous.disabled = page === 0;
      next.disabled = !hasNext;
      table.hidden = items.length === 0;
      status.textContent = items.length ? `Página ${page + 1}` : emptyMessage;
    } catch (error) {
      if (current === generation) status.textContent = error.message;
    } finally {
      if (current === generation) loadButton.disabled = false;
    }
  }

  loadButton.addEventListener('click', () => load(0));
  previous.addEventListener('click', () => load(page - 1));
  next.addEventListener('click', () => load(page + 1));
  return { reset };
}
