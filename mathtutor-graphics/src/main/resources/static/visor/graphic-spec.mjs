import { curve } from './curves.mjs';
import { surface } from './surfaces.mjs';

/** UC-35: solo datos validados; nunca eval, Function ni código procedente del servidor. */
export function drawSpecification(viewer, spec) {
  if (!viewer) throw new Error('No está disponible WebGL 2. Puedes seguir usando el tutor y consultar la función como texto.');
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
  const base = `${isSurface ? 'z = f(x,y)' : 'f(x)'} = ${spec.function}. Dominio [${d.min}, ${d.max}]${isSurface ? ' en ambos ejes' : ''}.`;
  const point = spec.points[0];
  if (!isSurface) {
    const graph = curve(source, d.min, d.max, Math.max(16, d.steps), isLine ? point.x : undefined);
    viewer.setGraph(graph);
    if (isLine) return `${base} Recta tangente rosa en x = ${point.x}. Pendiente aproximada: ${graph.tangent.slope.toLocaleString('es-ES')}.`;
  } else {
    if (source.length > 150 || d.min < -100 || d.max > 100 || d.steps > 128) {
      throw new Error('El visor de superficies admite hasta 150 caracteres, dominio de −100 a 100 y 128 divisiones.');
    }
    const mesh = surface(source, d.min, d.max, d.min, d.max, d.steps, isPlane ? [point.x, point.y] : undefined);
    viewer.setSurface(mesh);
    if (isPlane) return `${base} Plano tangente rosa en (${point.x}, ${point.y}). Pendientes aproximadas: ${mesh.tangent.dx.toLocaleString('es-ES')} en x y ${mesh.tangent.dy.toLocaleString('es-ES')} en y.`;
  }
  return `${base} Los huecos indican valores no representables o discontinuidades.`;
}
