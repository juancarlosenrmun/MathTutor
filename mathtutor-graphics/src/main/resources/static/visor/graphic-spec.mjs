import { curve } from './curves.mjs';
import { surface } from './surfaces.mjs';

/** UC-35: solo datos validados; nunca eval, Function ni código procedente del servidor. */
export function drawSpecification(viewer, spec) {
  if (!viewer) throw new Error('No está disponible WebGL 2. Puedes seguir usando el tutor y consultar la función como texto.');
  const d=spec?.domain;
  if (!spec || !['CURVA_2D','SUPERFICIE'].includes(spec.type) || typeof spec.function!=='string'
      || !spec.function.trim() || spec.function.length>200 || !d || !Number.isFinite(d.min) || !Number.isFinite(d.max)
      || d.min>=d.max || d.min < -10000 || d.max>10000 || !Number.isInteger(d.steps) || d.steps<2 || d.steps>200
      || !Array.isArray(spec.points) || spec.points.length>100 || spec.points.some(p=>!p || ![p.x,p.y,p.z].every(Number.isFinite)))
    throw new Error('La especificación gráfica no es válida. Revisa la función y el dominio.');
  const source=spec.function.replace(/E(?=[+-]?\d)/g,'e');
  if (spec.type==='CURVA_2D') viewer.setGraph(curve(source,d.min,d.max,Math.max(16,d.steps)));
  else {
    if (source.length>150 || d.min < -100 || d.max>100 || d.steps>128)
      throw new Error('El visor de superficies admite hasta 150 caracteres, dominio de −100 a 100 y 128 divisiones.');
    viewer.setSurface(surface(source,d.min,d.max,d.min,d.max,d.steps));
  }
  return `${spec.type==='CURVA_2D' ? 'f(x)' : 'z = f(x,y)'} = ${spec.function}. Dominio [${d.min}, ${d.max}]${spec.type==='SUPERFICIE' ? ' en ambos ejes' : ''}. Los huecos indican valores no representables o discontinuidades.`;
}
