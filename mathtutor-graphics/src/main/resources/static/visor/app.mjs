import { WebGLViewer } from './viewer.mjs';
import { SessionClient } from './session.mjs';

/** Conecta los controles sin conservar contraseñas ni tokens en disco. */
export function mount(document, host = globalThis) {
  const status = document.querySelector('#graphic-status');
  let viewer;
  try { viewer = new WebGLViewer(document.querySelector('canvas'), status, host); }
  catch (error) { status.textContent = error.message; }
  for (const button of document.querySelectorAll('[data-camera]')) {
    button.disabled = !viewer;
    button.addEventListener('click', () => viewer.command(button.dataset.camera));
  }
  const client = new SessionClient(host.fetch.bind(host));
  const role = document.querySelector('#role');
  let latestUpdate = 0;
  async function updateRole() {
    const update = ++latestUpdate;
    try {
      const currentRole = await client.role();
      if (update === latestUpdate && currentRole !== undefined) role.textContent = currentRole || 'Sin sesión';
    }
    catch { if (update === latestUpdate) role.textContent = 'No se pudo comprobar el permiso actual'; }
  }
  document.querySelector('form').addEventListener('submit', async event => {
    event.preventDefault();
    latestUpdate++;
    const form = event.currentTarget;
    try {
      await client.login(form.elements.email.value, form.elements.password.value);
      await updateRole();
    } catch (error) { role.textContent = error.message; }
    finally { form.elements.password.value = ''; }
  });
  // RNF-04: vuelve a consultar el rol vigente cada 3 s, sin reautenticar al usuario.
  const interval = host.setInterval(updateRole, 3000);
  host.addEventListener('pagehide', event => {
    // El navegador puede conservar esta página viva en su caché de navegación.
    if (event.persisted) return;
    host.clearInterval(interval); client.clear(); viewer?.dispose();
  });
}
