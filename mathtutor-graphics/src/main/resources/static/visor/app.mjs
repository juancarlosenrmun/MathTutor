import { WebGLViewer } from './viewer.mjs';
import { mountInterface } from './interface.mjs';
import { mountTutor } from './tutor.mjs';
import { mountCurveControls } from './curves.mjs';

/** Conecta el visor al laboratorio y conserva sus recursos entre vistas. */
export function mount(document, host = globalThis) {
  const status = document.querySelector('#graphic-status');
  let viewer;
  try { viewer = new WebGLViewer(document.querySelector('canvas'), status, host); }
  catch (error) { status.textContent = error.message; }
  if (document.querySelector('#curve-form')) mountCurveControls(document, viewer);
  for (const button of document.querySelectorAll('[data-camera]')) {
    button.disabled = !viewer;
    button.addEventListener('click', () => viewer.command(button.dataset.camera));
  }
  host.addEventListener('pagehide', event => {
    if (!event.persisted) viewer?.dispose();
  });
  let tutor;
  return mountInterface(document, host, (view, client) => {
    if (!tutor && document.querySelector('#tutor-form')) tutor = mountTutor(document, client);
    tutor?.sync(view);
    if (view === 'laboratorio') viewer?.render();
  });
}
