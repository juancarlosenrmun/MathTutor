import { WebGLViewer } from './viewer.mjs';
import { mountInterface } from './interface.mjs';
import { mountTutor } from './tutor.mjs';
import { mountCurveControls } from './curves.mjs';
import { mountSurfaceControls } from './surfaces.mjs';
import { mountPrivacy } from './privacy.mjs';
import { mountAdministration } from './administration.mjs';
import { mountTeacherStatistics } from './teacher-statistics.mjs';

/** Conecta el visor al laboratorio y conserva sus recursos entre vistas. */
export function mount(document, host = globalThis) {
  if (document.querySelector('#privacy-banner')) mountPrivacy(document, host);
  const status = document.querySelector('#graphic-status');
  let viewer;
  try { viewer = new WebGLViewer(document.querySelector('canvas'), status, host); }
  catch (error) {
    status.textContent = error.message;
    document.querySelector('canvas')?.removeAttribute?.('tabindex');
  }
  const selectionMode = document.querySelector('#selection-mode');
  if (selectionMode) {
    selectionMode.disabled = !viewer;
    if (viewer) viewer.selectionMode = selectionMode;
  }
  if (document.querySelector('#curve-form')) mountCurveControls(document, viewer);
  if (document.querySelector('#surface-form')) mountSurfaceControls(document, viewer);
  for (const button of document.querySelectorAll('[data-camera]')) {
    button.disabled = !viewer;
    button.addEventListener('click', () => viewer.command(button.dataset.camera));
  }
  const animationStatus = document.querySelector('#animation-status');
  if (viewer && animationStatus) mountAnimationControls(document, viewer, animationStatus);
  host.addEventListener('pagehide', event => {
    if (event.persisted) viewer?.stopAnimation();
    else viewer?.dispose();
  });
  let tutor;
  let administration;
  let statistics;
  const panel = document.querySelector('.viewer-panel');
  const lab = panel?.parentElement;
  const next = panel?.nextSibling;
  return mountInterface(document, host, (view, client) => {
    if (!administration && document.querySelector('#admin-panel')) {
      administration = mountAdministration(document, host, client);
    }
    administration?.sync(view);
    if (!statistics && document.querySelector('#teacher-panel')) statistics = mountTeacherStatistics(document, client);
    statistics?.sync(view);
    if (!tutor && document.querySelector('#tutor-form')) tutor = mountTutor(document, client, {viewer});
    tutor?.sync(view);
    const slot = document.querySelector('#tutor-viewer-slot');
    if (panel && slot && lab) {
      if (view === 'tutor' && panel.parentElement !== slot) slot.append(panel);
      if (view !== 'tutor' && panel.parentElement !== lab) lab.insertBefore(panel, next);
    }
    if (view === 'laboratorio' || view === 'tutor') viewer?.render();
    else viewer?.stopAnimation();
  });
}

/** @brief Sincroniza los botones y solo anuncia los cambios de estado del recorrido. */
function mountAnimationControls(document, viewer, status) {
  const buttons = [...document.querySelectorAll('[data-animation]')];
  const stop = buttons.find(button => button.dataset.animation === 'stop');
  const feedback = {
    secant: {
      playing: 'Secante en movimiento: observa cómo se acerca el segundo punto al punto de tangencia.',
      finished: 'La secante ha alcanzado la posición de la tangente.',
      reduced: 'Tangente mostrada sin animación.',
    },
    camera: {
      playing: 'Recorrido de cámara en marcha.',
      finished: 'Recorrido terminado. La cámara ha vuelto a su posición inicial.',
      reduced: 'Perspectiva actualizada sin animación.',
    },
  };
  viewer.onAnimationChange = (kind, state) => {
    for (const button of buttons) {
      if (button !== stop) button.disabled = !viewer.canAnimate(button.dataset.animation);
    }
    if (state && state !== 'playing' && document.activeElement === stop) {
      buttons.find(button => button.dataset.animation === kind)?.focus();
    }
    stop.disabled = !viewer.animation;
    if (state) status.textContent = state === 'stopped' ? 'Animación detenida.' : feedback[kind][state];
    else if (viewer.canAnimate('secant') || !viewer.canAnimate('camera')) status.textContent = '';
    else status.textContent = 'Para animar una secante, dibuja una curva con un punto de tangencia.';
  };
  for (const button of buttons) {
    button.addEventListener('click', () => {
      if (button === stop) viewer.stopAnimation();
      else viewer.playAnimation(button.dataset.animation);
    });
  }
  viewer.onAnimationChange();
}
