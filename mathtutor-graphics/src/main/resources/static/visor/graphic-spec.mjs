import { curve, describeCurve } from './curves.mjs';
import { surface, describeSurface } from './surfaces.mjs';

/** UC-35: solo datos validados; nunca eval, Function ni código procedente del servidor. */
export function drawSpecification(viewer, spec) {
  const d=spec?.domain;
  if (!spec || !['CURVA_2D','SUPERFICIE','RECTA_TANGENTE','PLANO_TANGENTE'].includes(spec.type) || typeof spec.function!=='string'
      || !spec.function.trim() || spec.function.length>200 || !d || !Number.isFinite(d.min) || !Number.isFinite(d.max)
      || d.min>=d.max || d.min < -10000 || d.max>10000 || !Number.isInteger(d.steps) || d.steps<2 || d.steps>200
      || !Array.isArray(spec.points) || spec.points.length>100 || spec.points.some(p=>!p || ![p.x,p.y,p.z].every(Number.isFinite)))
    throw new Error('La especificación gráfica no es válida. Revisa la función y el dominio.');
  const source=spec.function.replace(/E(?=[+-]?\d)/g,'e');
  const isPlane = spec.type === 'PLANO_TANGENTE';
  const isLine = spec.type === 'RECTA_TANGENTE';
  const isSurface = isPlane || spec.type === 'SUPERFICIE';
  if ((isPlane || isLine) && spec.points.length !== 1) {
    throw new Error('Indica un único punto de tangencia.');
  }
  const point = spec.points[0];
  if (!isSurface) {
    const graph = curve(source, d.min, d.max, Math.max(16, d.steps), isLine ? point.x : undefined);
    viewer?.setGraph(graph);
    return describeCurve(spec.function, graph);
  }
  if (source.length > 150 || d.min < -100 || d.max > 100 || d.steps > 128) {
    throw new Error('El visor de superficies admite hasta 150 caracteres, dominio de −100 a 100 y 128 divisiones.');
  }
  const mesh = surface(source, d.min, d.max, d.min, d.max, d.steps, isPlane ? [point.x, point.y] : undefined);
  viewer?.setSurface(mesh);
  return describeSurface(spec.function, mesh);
}
