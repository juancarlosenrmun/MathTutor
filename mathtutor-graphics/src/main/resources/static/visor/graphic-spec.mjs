import { curve, describeCurve, expression } from './curves.mjs';
import { surface, describeSurface } from './surfaces.mjs';

const GRAPH_REQUEST = /\b(?:dibuj\w*|grafic\w*|represent\w*|traz\w*|visualiz\w*)\b/i;
const TANGENCY = /\b(recta|plano)\s+tangente\b/i;
const EXAMPLE_REQUEST = /^(?:dame|ponme|proponme|genera|sugiere)\b[^.!?\n]*\b(?:ejercicio|ejemplo)\b/i;
const FUNCTION_LABEL = /(?:\bf\s*\(\s*x\s*(?:,\s*(y)\s*)?\)|\b([yz]))\s*=\s*/i;
const FORMULA_PREFIX = /^(?:(?:sin|cos|tan|sqrt|abs|exp|log|pi|e)\b|[\dxy+\-*/^(). \t])+/i;
const EXERCISE_PREFIX = /^(?:deriva(?:r)?|grafica(?:r)?|dibuja(?:r)?|(?:calcula|halla|determina) la (?:derivada|integral) de)\s*:?\s+/i;

/** Toma la función del enunciado o el ejemplo que el alumno pidió dibujar. */
export function specificationFromChat(message, answer) {
  const normalized = message.trim().normalize('NFD').replace(/\p{M}/gu, '');
  const requested = GRAPH_REQUEST.test(normalized) || EXAMPLE_REQUEST.test(normalized) || TANGENCY.test(normalized);
  const read = (source, unlabeled = false) => {
    const label = FUNCTION_LABEL.exec(source);
    if (!label && !unlabeled) return null;
    const start = label ? label.index + label[0].length : 0;
    const candidate = source.slice(start).trim().replace(/\.$/, '');
    const prefix = FORMULA_PREFIX.exec(candidate)?.[0];
    const fn = prefix?.trim().replace(/\.$/, '').toLowerCase();
    if (!fn || fn.length > 150) return null;
    if (!label && (prefix !== candidate || !/[xy]/.test(fn))) return null;
    const isSurface = Boolean(label?.[1]) || label?.[2]?.toLowerCase() === 'z' || /\by\b/.test(fn);
    try { expression(isSurface ? fn.replace(/\by\b/g, '(0)') : fn); }
    catch { return null; }
    const tangent = TANGENCY.exec(normalized)?.[1];
    const min = -2, max = 2;
    const point = axis => {
      const coordinate = new RegExp(`\\b${axis}\\s*=\\s*(-?\\d+(?:\\.\\d+)?)`, 'i').exec(normalized)?.[1];
      return coordinate === undefined ? 0 : Number(coordinate);
    };
    const points = tangent === (isSurface ? 'plano' : 'recta')
      ? [{x: point('x'), y: isSurface ? point('y') : 0, z: 0}] : [];
    return {type: points.length ? (isSurface ? 'PLANO_TANGENTE' : 'RECTA_TANGENTE') : isSurface ? 'SUPERFICIE' : 'CURVA_2D', function: fn,
      domain: {min: Math.min(min, ...points.map(p => Math.min(p.x, p.y))),
        max: Math.max(max, ...points.map(p => Math.max(p.x, p.y))), steps: 32}, points};
  };
  const input = read(message) ?? read(message.replace(EXERCISE_PREFIX, ''), true);
  if (input || !requested) return input;
  for (const formula of answer.matchAll(/[\x60$]([^\x60$\r\n]+)[\x60$]/g)) {
    const spec = read(formula[1]);
    if (spec) return spec;
  }
  return null;
}

function moveTangent(viewer, model, point, describe) {
  let updated;
  try {
    updated = model.tangentAt(point);
  } catch (error) {
    viewer?.updateTangency({...model, tangent: undefined, secantAt: undefined});
    throw error;
  }
  viewer?.updateTangency(updated);
  return describe(updated);
}

/** UC-35: solo datos validados; nunca eval, Function ni código procedente del servidor. */
export function drawSpecification(viewer, spec, liveTangency) {
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
    if (isLine) liveTangency?.([d.min, d.max], point.x, undefined, x =>
      moveTangent(viewer, graph, x, model => describeCurve(spec.function, model)));
    return describeCurve(spec.function, graph);
  }
  if (source.length > 150 || d.min < -100 || d.max > 100 || d.steps > 128) {
    throw new Error('El visor de superficies admite hasta 150 caracteres, dominio de −100 a 100 y 128 divisiones.');
  }
  const mesh = surface(source, d.min, d.max, d.min, d.max, d.steps, isPlane ? [point.x, point.y] : undefined);
  viewer?.setSurface(mesh);
  if (isPlane) liveTangency?.([d.min, d.max], point.x, point.y, (x, y) =>
    moveTangent(viewer, mesh, [x, y], model => describeSurface(spec.function, model)));
  return describeSurface(spec.function, mesh);
}
