import { WebGLViewer } from './viewer.mjs';
import { mountInterface } from './interface.mjs';
import { mountTutor } from './tutor.mjs';
import { mountCurveControls } from './curves.mjs';
import { mountSurfaceControls } from './surfaces.mjs';
import { mountPrivacy } from './privacy.mjs';

/** Conecta el visor al laboratorio y conserva sus recursos entre vistas. */
export function mount(document, host = globalThis) {
  if (document.querySelector('#privacy-banner')) mountPrivacy(document, host);
  const status = document.querySelector('#graphic-status');
  let viewer;
  try { viewer = new WebGLViewer(document.querySelector('canvas'), status, host); }
  catch (error) { status.textContent = error.message; }
  if (document.querySelector('#curve-form')) mountCurveControls(document, viewer);
  if (document.querySelector('#surface-form')) mountSurfaceControls(document, viewer);
  for (const button of document.querySelectorAll('[data-camera]')) {
    button.disabled = !viewer;
    button.addEventListener('click', () => viewer.command(button.dataset.camera));
  }
  host.addEventListener('pagehide', event => {
    if (!event.persisted) viewer?.dispose();
  });
  let tutor;
  const panel = document.querySelector('.viewer-panel');
  const lab = panel?.parentElement;
  const next = panel?.nextSibling;
  return mountInterface(document, host, (view, client) => {
    if (!tutor && document.querySelector('#tutor-form')) tutor = mountTutor(document, client, {viewer});
    tutor?.sync(view);
    const slot = document.querySelector('#tutor-viewer-slot');
    if (panel && slot && lab) {
      if (view === 'tutor' && panel.parentElement !== slot) slot.append(panel);
      if (view !== 'tutor' && panel.parentElement !== lab) lab.insertBefore(panel, next);
    }
    if (view === 'laboratorio' || view === 'tutor') viewer?.render();
  });
}
