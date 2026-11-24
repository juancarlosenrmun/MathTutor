import { expression } from './curves.mjs';
import { configureTangencyRange, planeTangent } from './tangents.mjs';

/**
 * @brief Mallado determinista de z=f(x,y). Cada celda válida produce dos triángulos.
 * @param {string} source Expresión aritmética acotada; nunca se evalúa como JavaScript.
 * @param {number} xMin Límite inferior horizontal.
 * @param {number} xMax Límite superior horizontal.
 * @param {number} yMin Límite inferior vertical.
 * @param {number} yMax Límite superior vertical.
 * @param {number} divisions Número de celdas por eje, entre 2 y 128.
 * @returns {{triangles:Array<{vertices:number[][],normal:number[]}>,bounds:number[]}}
 */
export function surface(source, xMin, xMax, yMin, yMax, divisions = 32, tangentPoint) {
  if (typeof source !== 'string' || !source.trim() || source.length > 150 ||
      ![xMin,xMax,yMin,yMax].every(Number.isFinite) || xMin >= xMax || yMin >= yMax ||
      Math.max(Math.abs(xMin),Math.abs(xMax),Math.abs(yMin),Math.abs(yMax)) > 100 ||
      !Number.isInteger(divisions) || divisions < 2 || divisions > 128) {
    throw new Error('Expresión, dominio o resolución de superficie inválidos.');
  }
  const bounds = [xMin, xMax, yMin, yMax];
  const formulaAt = y => expression(source.replace(/\by\b/g, `(${y})`));
  const rows = [], formulas = [];
  for (let j = 0; j <= divisions; j++) {
    const y = yMin + (yMax-yMin)*j/divisions;
    const formula = formulaAt(y);
    formulas.push(formula);
    rows.push(Array.from({length: divisions+1}, (_, i) => {
      const x = xMin + (xMax-xMin)*i/divisions;
      const z = formula(x);
      return Number.isFinite(z) && Math.abs(z) <= 1000 ? [x,y,z] : null;
    }));
  }
  const triangles = [];
  const add = (a,b,c) => {
    const u = b.map((v,i) => v-a[i]), v = c.map((value,i) => value-a[i]);
    const normal = [u[1]*v[2]-u[2]*v[1], u[2]*v[0]-u[0]*v[2], u[0]*v[1]-u[1]*v[0]];
    const length = Math.hypot(...normal);
    if (length > 0 && Number.isFinite(length)) triangles.push({vertices:[a,b,c],normal:normal.map(n => n/length)});
  };
  for (let j = 0; j < divisions; j++) for (let i = 0; i < divisions; i++) {
    const x0 = xMin + (xMax-xMin)*i/divisions;
    const x1 = xMin + (xMax-xMin)*(i+1)/divisions;
    if (![...formulas[j].bounds(x0,x1), ...formulas[j+1].bounds(x0,x1)].every(Number.isFinite)) continue;
    const a = rows[j][i], b = rows[j][i+1], c = rows[j+1][i], d = rows[j+1][i+1];
    if (a && b && c && d) { add(a,b,c); add(b,d,c); }
  }
  const mesh = {triangles, bounds, tangentAt(point) {
    return {...mesh, tangent: planeTangent((x, y) => formulaAt(y)(x), point, bounds)};
  }};
  return tangentPoint === undefined ? mesh : mesh.tangentAt(tangentPoint);
}

/** @brief Describe el dominio, los valores representables y el plano tangente de una superficie. */
export function describeSurface(source, mesh) {
  const format = value => value.toLocaleString('es-ES', { maximumFractionDigits: 6 });
  const [xMin, xMax, yMin, yMax] = mesh.bounds.map(format);
  let lo = Infinity, hi = -Infinity;
  for (const triangle of mesh.triangles) for (const [, , z] of triangle.vertices) {
    lo = Math.min(lo, z);
    hi = Math.max(hi, z);
  }
  const range = mesh.triangles.length ? `Valores de z muestreados entre ${format(lo)} y ${format(hi)}.`
    : 'No hay superficie representable en ese dominio.';
  const tangent = mesh.tangent ? ` Plano tangente en (${mesh.tangent.point.map(format).join('; ')}). Pendientes aproximadas: ${format(mesh.tangent.dx)} en x y ${format(mesh.tangent.dy)} en y.` : '';
  return `z = f(x,y) = ${source}. Dominio de ${xMin} a ${xMax} en x y de ${yMin} a ${yMax} en y. ${range}${tangent}`;
}

/** @brief Conecta el formulario de superficies y ofrece una descripción independiente de WebGL. */
export function mountSurfaceControls(document, viewer) {
  const one = id => document.querySelector('#' + id);
  const form = one('surface-form');
  const tangentX = one('surface-tangent-x'), tangentY = one('surface-tangent-y');
  let plotted;
  const moveTangent = () => {
    if (!plotted) return;
    const {source, mesh} = plotted;
    try {
      const updated = mesh.tangentAt([Number(tangentX.value), Number(tangentY.value)]);
      viewer?.updateTangency(updated);
      one('surface-result').textContent = describeSurface(source, updated);
    } catch (error) {
      viewer?.updateTangency(mesh);
      one('surface-result').textContent = error.message;
    }
  };
  tangentX?.addEventListener('input', moveTangent);
  tangentY?.addEventListener('input', moveTangent);
  form.addEventListener('submit', event => {
    event.preventDefault();
    try {
      const source = one('surface-function').value;
      const xMin = Number(one('surface-x-min').value), xMax = Number(one('surface-x-max').value);
      const yMin = Number(one('surface-y-min').value), yMax = Number(one('surface-y-max').value);
      const mesh = surface(source, xMin, xMax, yMin, yMax, Number(one('surface-resolution').value));
      plotted = {source, mesh};
      if (tangentX && tangentY) {
        configureTangencyRange(tangentX, xMin, xMax);
        configureTangencyRange(tangentY, yMin, yMax);
      }
      viewer?.setSurface(mesh);
      one('surface-result').textContent = describeSurface(source, mesh);
    } catch (error) { one('surface-result').textContent = error.message; }
  });
}
