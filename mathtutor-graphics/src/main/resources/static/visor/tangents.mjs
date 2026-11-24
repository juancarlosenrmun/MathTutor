/**
 * @file
 * @brief Tangentes numéricas sobre el analizador de expresiones del visor.
 */

const undefinedTangent = 'No se puede estimar una tangente en ese punto.';

/** Sincroniza el recorrido del control con el dominio actualmente dibujado. */
export function configureTangencyRange(field, min, max, point = 0) {
  field.min = String(min);
  field.max = String(max);
  field.value = String(Math.max(min, Math.min(max, point)));
  field.disabled = false;
}

/** Muestra la coordenada y permite refrescarla también al cambiar el dominio. */
export function bindTangencyValue(field, output) {
  const update = () => {
    if (output) output.textContent = Number(field.value).toLocaleString('es-ES', {maximumFractionDigits: 6});
  };
  field?.addEventListener('input', update);
  return update;
}

function checkPoint(value, min, max) {
  if (!Number.isFinite(value) || value < min || value > max) {
    throw new Error('El punto de tangencia debe estar dentro del dominio.');
  }
}

function checkValue(value) {
  if (!Number.isFinite(value) || Math.abs(value) > 1000) throw new Error(undefinedTangent);
}

/** @brief Estima una dirección mediante cocientes laterales de segundo orden. */
function slope(evaluate, origin, h) {
  const [left2, left1, right1, right2] = [-2, -1, 1, 2].map(offset => evaluate(offset * h));
  const left = (4 * (origin - left1) - (origin - left2)) / (2 * h);
  const right = (4 * (right1 - origin) - (right2 - origin)) / (2 * h);
  if (!similar(left, right)) throw new Error(undefinedTangent);
  return (left + right) / 2;
}

function similar(a, b) {
  return Number.isFinite(a) && Number.isFinite(b)
    && Math.abs(a - b) <= 1e-5 * Math.max(1, Math.abs(a), Math.abs(b));
}

function derivative(evaluate, origin, scale) {
  const h = 1e-5 * Math.min(scale, 10);
  const coarse = slope(evaluate, origin, h);
  const fine = slope(evaluate, origin, h / 2);
  if (!similar(coarse, fine)) throw new Error(undefinedTangent);
  return fine;
}

function extent(value, min, max) {
  const radius = (max - min) / 4;
  return [Math.max(min, value - radius), Math.min(max, value + radius)];
}

/**
 * @brief Calcula los dos vértices visibles de la recta que pasa por (x, f(x)).
 * @param evaluate Función de una variable ya analizada.
 * @param x Abscisa del punto de tangencia.
 * @param min Extremo inferior dibujado.
 * @param max Extremo superior dibujado.
 */
export function lineTangent(evaluate, x, min, max) {
  checkPoint(x, min, max);
  const value = evaluate(x);
  checkValue(value);
  const m = derivative(offset => evaluate(x + offset), value, Math.max(1, Math.abs(x)));
  const lines = extent(x, min, max).map(u => [u, value + m * (u - x), 0]);
  return { x, value, slope: m, lines };
}

/**
 * @brief Prepara el acercamiento de un segundo punto a la tangencia dentro de un tramo continuo.
 * @param evaluate Función analizada con cotas por intervalo.
 * @param tangent Tangente validada por la estimación bilateral.
 * @param min Extremo inferior dibujado.
 * @param max Extremo superior dibujado.
 * @return Función del progreso normalizado, si existe una secante representable.
 */
export function secantTrajectory(evaluate, tangent, min, max) {
  const { x, value, lines } = tangent;
  const [left, right] = extent(x, min, max);
  const offsets = [left - x, right - x].sort((a, b) => Math.abs(b) - Math.abs(a));
  let start;
  for (const offset of offsets) {
    for (let step = 0; step < 16; step++) {
      const candidate = x + offset / 2 ** step;
      if (candidate === x) break;
      const bounds = evaluate.bounds(Math.min(x, candidate), Math.max(x, candidate));
      if (bounds.every(bound => Number.isFinite(bound) && Math.abs(bound) <= 1000)) {
        start = candidate - x;
        break;
      }
    }
    if (start !== undefined) break;
  }
  if (start === undefined) return undefined;

  return progress => {
    const second = x + start * (1 - progress);
    if (second === x) return tangent;
    const height = evaluate(second);
    const slope = (height - value) / (second - x);
    return { slope, point: [second, height, 0], lines: lines.map(([u]) => [u, value + slope * (u - x), 0]) };
  };
}

/**
 * @brief Aproxima el plano por sus dos parciales y comprueba también las direcciones diagonales.
 * @param evaluate Función de dos variables ya analizada.
 * @param point Coordenadas x e y del punto de apoyo.
 * @param bounds Límites xMin, xMax, yMin, yMax.
 */
export function planeTangent(evaluate, point, bounds) {
  const [x, y] = point;
  const [xMin, xMax, yMin, yMax] = bounds;
  checkPoint(x, xMin, xMax);
  checkPoint(y, yMin, yMax);
  const value = evaluate(x, y);
  checkValue(value);
  const scale = Math.max(1, Math.abs(x), Math.abs(y));
  const dx = derivative(offset => evaluate(x + offset, y), value, scale);
  const dy = derivative(offset => evaluate(x, y + offset), value, scale);
  for (const direction of [-1, 1]) {
    const diagonal = derivative(offset => evaluate(x + offset, y + direction * offset), value, scale);
    if (!similar(diagonal, dx + direction * dy)) throw new Error(undefinedTangent);
  }
  const [a, b] = extent(x, xMin, xMax);
  const [c, d] = extent(y, yMin, yMax);
  const corners = [[a, c], [b, c], [b, d], [a, d]];
  const lines = [0, 1, 1, 2, 2, 3, 3, 0, 0, 2, 1, 3].map(i => {
    const [u, v] = corners[i];
    return [u, v, value + dx * (u - x) + dy * (v - y)];
  });
  return { point, value, dx, dy, lines };
}
