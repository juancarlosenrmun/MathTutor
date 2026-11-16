import { WebGLViewer } from './viewer.mjs';
import { mountInterface } from './interface.mjs';

/** Conecta el visor al laboratorio y conserva sus recursos entre vistas. */
export function mount(document, host = globalThis) {
  const status = document.querySelector('#graphic-status');
  let viewer;
  try { viewer = new WebGLViewer(document.querySelector('canvas'), status, host); }
  catch (error) { status.textContent = error.message; }
  for (const button of document.querySelectorAll('[data-camera]')) {
    button.disabled = !viewer;
    button.addEventListener('click', () => viewer.command(button.dataset.camera));
  }
  host.addEventListener('pagehide', event => {
    if (!event.persisted) viewer?.dispose();
  });
  return mountInterface(document, host, view => {
    if (view === 'laboratorio') viewer?.render();
  });
}
