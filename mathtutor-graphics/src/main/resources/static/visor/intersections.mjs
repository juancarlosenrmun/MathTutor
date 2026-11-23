const epsilon = 1e-9;
const subtract = (a, b) => a.map((value, i) => value - b[i]);
const dot = (a, b) => a.reduce((sum, value, i) => sum + value * b[i], 0);
const cross = (a, b) => [
  a[1] * b[2] - a[2] * b[1],
  a[2] * b[0] - a[0] * b[2],
  a[0] * b[1] - a[1] * b[0],
];
const at = (origin, direction, t) => origin.map((value, i) => value + t * direction[i]);

/**
 * @brief Selecciona el primer triángulo atravesado por el rayo, por ambas caras.
 * @param origin Origen en coordenadas del mundo.
 * @param direction Vector director.
 * @param triangles Triángulos de la malla dibujada.
 * @param limit Parámetro máximo visible sobre el rayo.
 * @return Coordenadas de la intersección o null.
 */
export function raySurface(origin, direction, triangles, limit = Infinity) {
  let nearest = limit;
  let hit = null;
  for (const {vertices: [a, b, c]} of triangles) {
    const ab = subtract(b, a), ac = subtract(c, a);
    const p = cross(direction, ac);
    const determinant = dot(ab, p);
    if (Math.abs(determinant) <= epsilon * Math.hypot(...ab) * Math.hypot(...p)) continue;
    const offset = subtract(origin, a);
    const q = cross(offset, ab);
    const u = dot(offset, p) / determinant, v = dot(direction, q) / determinant;
    if (u < -epsilon || v < -epsilon || u + v > 1 + epsilon) continue;
    const distance = dot(ac, q) / determinant;
    if (distance >= 0 && distance <= nearest) {
      nearest = distance;
      hit = at(origin, direction, distance);
    }
  }
  return hit;
}

/**
 * @brief Calcula la intersección única de una recta y un plano.
 * @param origin Punto de la recta.
 * @param direction Vector director de la recta.
 * @param point Punto del plano.
 * @param normal Vector normal al plano.
 * @return Coordenadas de la intersección o null para rectas paralelas o contenidas.
 */
export function linePlane(origin, direction, point, normal) {
  const denominator = dot(direction, normal);
  if (Math.abs(denominator) <= epsilon * Math.hypot(...direction) * Math.hypot(...normal)) return null;
  return at(origin, direction, dot(subtract(point, origin), normal) / denominator);
}

/**
 * @brief Limita la intersección recta–plano al tramo visible y al plano tangente dibujado.
 * @param ray Recta de visión acotada por los planos de recorte.
 * @param tangent Plano tangente con las aristas de su rectángulo visible.
 * @return Punto seleccionado o null.
 */
export function visiblePlane({origin, direction, limit}, tangent) {
  if (!tangent) return null;
  const lower = tangent.lines[0], upper = tangent.lines[3];
  const point = linePlane(origin, direction, lower, [-tangent.dx, -tangent.dy, 1]);
  if (!point) return null;
  const distance = dot(subtract(point, origin), direction) / dot(direction, direction);
  return distance >= 0 && distance <= limit
    && point[0] >= lower[0] - epsilon && point[0] <= upper[0] + epsilon
    && point[1] >= lower[1] - epsilon && point[1] <= upper[1] + epsilon ? point : null;
}
