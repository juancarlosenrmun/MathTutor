/**
 * @file
 * @brief Tangentes numéricas sobre el analizador de expresiones del visor.
 */

const undefinedTangent = 'No se puede estimar una tangente en ese punto.';

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
