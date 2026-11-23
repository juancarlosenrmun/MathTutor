import { SessionClient } from './session.mjs';

const titles = { inicio: 'Inicio', acceso: 'Iniciar sesión', registro: 'Crear cuenta', espacio: 'Tu espacio',
  laboratorio: 'Laboratorio', tutor: 'Tutor de Matemáticas', cuenta: 'Tu cuenta', restringido: 'Acceso restringido',
  privacidad: 'Privacidad y cookies', recuperar: 'Recuperar contraseña', restablecer: 'Nueva contraseña' };

const resetLink = /^#\/restablecer\/([A-Za-z0-9_-]{43})$/;

export function viewFor(hash, signedIn) {
  if (resetLink.test(hash)) return 'restablecer';
  if (hash === '#/restablecer' || hash.startsWith('#/restablecer/')) return 'recuperar';
  const requested = hash.replace('#/', '');
  const view = Object.hasOwn(titles, requested) ? requested : 'inicio';
  if ((view === 'cuenta' || view === 'espacio' || view === 'tutor') && !signedIn) return 'acceso';
  if (signedIn && (view === 'acceso' || view === 'registro')) return 'espacio';
  return view;
}

export function permissionDescription(role) {
  return { ALUMNO: 'Acceso a tus contenidos y recursos propios.',
    PROFESOR: 'Permisos de profesor: contenido y estadísticas agregadas.',
    ADMIN: 'Permisos de administración del centro.' }[role] || 'Sin permisos asignados. Contacta con tu centro.';
}

export function workspaceFor(role) {
  return {
    ALUMNO: { label: 'Alumno', title: 'Tu espacio de aprendizaje.',
      description: 'Elige un bloque para conversar con el tutor o explora funciones, derivadas e integrales en el laboratorio.' },
    PROFESOR: { label: 'Profesor', title: 'Tu espacio docente.',
      description: 'Conversa con el tutor, explora el laboratorio y consulta los permisos de tu cuenta de profesor.' },
    ADMIN: { label: 'Administrador', title: 'Tu espacio de administración.',
      description: 'Consulta tu cuenta y tus permisos actuales. También puedes explorar el laboratorio.' },
  }[role] || { label: 'Sin rol asignado', title: 'Tu espacio.',
    description: 'Consulta tu cuenta para comprobar tus permisos actuales.' };
}

/** Navegación local: el token nunca viaja en enlaces ni se guarda en disco. */
export function mountInterface(document, host, onView) {
  const client = new SessionClient(host.fetch.bind(host));
  const all = selector => [...document.querySelectorAll(selector)];
  const one = selector => document.querySelector(selector);
  const notice = one('#notice');
  let busy = true, polling = false, disposed = false, currentView, activity = 0;
  function announce(message) {
    notice.textContent = message; notice.hidden = !message; one('#retry-logout').hidden = true;
  }
  function render() {
    if (disposed) return;
    const signedIn = Boolean(client.token);
    const view = viewFor(host.location.hash, signedIn);
    for (const section of all('[data-view]')) section.hidden = section.dataset.view !== view;
    for (const link of all('[data-nav]')) {
      if (link.dataset.nav === view) link.setAttribute('aria-current', 'page');
      else link.removeAttribute('aria-current');
    }
    for (const link of all('[data-guest]')) link.hidden = signedIn;
    for (const link of all('[data-member]')) link.hidden = !signedIn;
    for (const link of all('[data-student]')) link.hidden = !signedIn || !['ALUMNO', 'PROFESOR'].includes(client.profile?.role);
    for (const panel of all('[data-permission]')) {
      panel.hidden = !signedIn || !panel.dataset.permission.split(' ').includes(client.profile?.role);
    }
    one('#role').textContent = client.profile?.role || 'Sin sesión';
    one('#account-email').textContent = client.profile?.email || '—';
    one('#permission-description').textContent = permissionDescription(client.profile?.role);
    const workspace = workspaceFor(client.profile?.role);
    one('#workspace-role').textContent = workspace.label;
    one('#workspace-title').textContent = workspace.title;
    one('#workspace-description').textContent = workspace.description;
    document.title = `${titles[view]} · MathTutor`;
    if (currentView !== view) {
      for (const input of all('input[type="password"], [data-visible-password]')) input.value = '';
      for (const button of all('[data-password]')) {
        const input = document.getElementById(button.dataset.password);
        input.type = 'password'; input.removeAttribute('data-visible-password');
        button.textContent = 'Mostrar'; button.setAttribute('aria-pressed', 'false');
        button.setAttribute('aria-label', 'Mostrar contraseña');
      }
      for (const error of all('[data-error]')) error.hidden = true;
      if (currentView !== undefined) {
        one('#main').focus({ preventScroll: true });
        host.scrollTo(0, 0);
      }
      currentView = view;
    }
    onView(view, client);
  }
  function setBusy(value) {
    busy = value;
    if (value) activity++;
    for (const button of all('[data-auth] button[type="submit"], [data-logout]')) button.disabled = value;
    for (const button of all('[data-reset] button[type="submit"]')) button.disabled = value;
  }
  async function updateRole() {
    if (busy || polling || disposed || !client.token) return;
    polling = true;
    const generation = activity;
    try {
      const role = await client.role();
      if (disposed || generation !== activity) return;
      if (role !== undefined) {
        if (!role) announce('Tu sesión ha terminado. Inicia sesión para volver a tu cuenta.');
        render();
      }
    } catch (error) {
      if (disposed || generation !== activity) return;
      if (error.status === 403) host.location.hash = '#/restringido';
      announce(error.status === 403 ? error.message : 'No se pudo comprobar el permiso actual. Reintentaremos automáticamente.');
      render();
    } finally { polling = false; }
  }
  for (const form of all('[data-auth]')) {
    form.addEventListener('submit', async event => {
      event.preventDefault();
      if (busy) return;
      const error = form.querySelector('[data-error]');
      const password = form.elements.password;
      const confirm = form.elements.confirm;
      if (confirm && confirm.value !== password.value) {
        error.textContent = 'Las contraseñas no coinciden. Vuelve a escribirlas.';
        error.hidden = false; confirm.setAttribute('aria-invalid', 'true'); confirm.focus(); return;
      }
      confirm?.removeAttribute('aria-invalid');
      error.hidden = true; announce('');
      const route = host.location.hash;
      const button = form.querySelector('[type="submit"]');
      const label = button.textContent;
      setBusy(true); button.textContent = 'Un momento…';
      try {
        await client[form.dataset.auth](form.elements.email.value.trim(), password.value);
        await client.role();
        if (!client.token) throw new Error('Tu sesión ha terminado. Vuelve a iniciar sesión.');
        if (host.location.hash === route) host.location.hash = '#/espacio';
        announce(form.dataset.auth === 'register' ? 'Tu cuenta está lista. Bienvenido a MathTutor.' : 'Has iniciado sesión.');
      } catch (failure) {
        if (failure.status === 403) host.location.hash = '#/restringido';
        error.textContent = failure.message; error.hidden = false; error.focus();
      } finally {
        password.value = ''; if (confirm) confirm.value = '';
        setBusy(false); button.textContent = label; render();
      }
    });
  }
  for (const button of all('[data-password]')) button.addEventListener('click', () => {
    const input = document.getElementById(button.dataset.password);
    const show = input.type === 'password';
    input.type = show ? 'text' : 'password';
    input.toggleAttribute('data-visible-password', show);
    button.textContent = show ? 'Ocultar' : 'Mostrar';
    button.setAttribute('aria-pressed', String(show));
    button.setAttribute('aria-label', show ? 'Ocultar contraseña' : 'Mostrar contraseña');
  });
  for (const form of all('[data-reset]')) form.addEventListener('submit', async event => {
    event.preventDefault();
    if (busy) return;
    const route = host.location.hash;
    const error = form.querySelector('[data-error]');
    const password = form.elements.password;
    const confirmation = form.elements.confirm;
    error.hidden = true;
    if (password && password.value !== confirmation.value) {
      error.textContent = 'Las contraseñas no coinciden.';
      error.hidden = false;
      confirmation.focus();
      return;
    }
    setBusy(true);
    try {
      if (form.dataset.reset === 'request') {
        await client.requestPasswordReset(form.elements.email.value.trim());
        if (host.location.hash === route) {
          form.elements.email.value = '';
          announce('Si existe una cuenta activa con ese correo, recibirás un enlace de recuperación.');
        }
      } else {
        const token = resetLink.exec(route)?.[1];
        if (!token) throw new Error('Abre el enlace de recuperación desde el correo.');
        await client.confirmPasswordReset(token, password.value);
        if (host.location.hash === route) {
          host.location.replace(new URL('#/acceso', host.location.href));
          announce('Tu contraseña ha cambiado. Ya puedes iniciar sesión.');
        }
      }
    } catch (failure) {
      if (host.location.hash === route) {
        error.textContent = failure.message;
        error.hidden = false;
        error.focus();
      }
    } finally {
      if (password) {
        password.value = '';
        confirmation.value = '';
      }
      setBusy(false);
      render();
    }
  });
  for (const button of all('[data-logout]')) button.addEventListener('click', async () => {
    if (busy) return;
    setBusy(true);
    try { await client.logout(); announce('Has cerrado tu sesión. Hasta la próxima.'); }
    catch {
      announce('La sesión local se ha cerrado, pero el servidor no confirmó el cierre. Reintenta antes de dejar este equipo.');
      one('#retry-logout').hidden = false;
    }
    finally { setBusy(false); host.location.hash = '#/acceso'; render(); }
  });
  one('#export-account')?.addEventListener('click', async event => {
    if (busy) return;
    const button = event.currentTarget;
    const generation = activity;
    const owner = client.profile?.email;
    const stillOwnAccount = () => !disposed && generation === activity && currentView === 'cuenta'
      && Boolean(client.token) && owner === client.profile?.email;
    button.disabled = true;
    try {
      const response = await client.request('/api/privacy/export');
      const snapshot = await response.blob();
      if (!stillOwnAccount()) return;
      const url = host.URL.createObjectURL(snapshot);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'mathtutor-datos.json';
      link.click();
      host.setTimeout(() => host.URL.revokeObjectURL(url), 1000);
      announce('La descarga de tus datos está lista.');
    } catch (error) { if (stillOwnAccount()) announce(error.message); }
    finally { button.disabled = false; }
  });
  one('#erase-account')?.addEventListener('click', async event => {
    if (busy) return;
    const button = event.currentTarget;
    button.disabled = true;
    setBusy(true);
    try {
      await client.request('/api/privacy/account', { method: 'DELETE' });
      client.clear();
      button.closest('details').open = false;
      host.location.hash = '#/acceso';
      announce('Tu cuenta y tus datos se han suprimido.');
    } catch (error) { announce(error.message); }
    finally { button.disabled = false; setBusy(false); render(); }
  });
  one('.skip-link').addEventListener('click', event => {
    event.preventDefault(); one('#main').focus(); one('#main').scrollIntoView();
  });
  host.addEventListener('hashchange', render);
  const interval = host.setInterval(updateRole, 3000);
  host.addEventListener('pagehide', event => {
    if (event.persisted) return;
    disposed = true; host.clearInterval(interval); client.clear();
  });
  setBusy(true); render();
  const ready = (async () => {
    try { await client.restore(); await client.role(); }
    catch (error) {
      if (error.status === 403) host.location.hash = '#/restringido';
      announce('No se pudo recuperar tu sesión. Puedes volver a iniciar sesión.');
    } finally { setBusy(false); render(); }
  })();
  return { client, ready, render, updateRole };
}
