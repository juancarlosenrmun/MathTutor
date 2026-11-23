const PAGE_SIZE = 20;
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
  }

  async function load(targetPage, message) {
    const current = ++generation;
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
