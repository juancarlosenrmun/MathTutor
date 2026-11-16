/**
 * @brief Parser aritmético acotado para UC-28. Solo números, x y funciones explícitas.
 * @param {string} source Expresión; nunca se ejecuta como JavaScript ni GLSL.
 * @return {(x:number)=>number} Evaluador puro con precedencia matemática.
 */
const unknown = [-Infinity, Infinity];
const node = (at, bounds) => Object.assign(at, { bounds });
const constant = value => node(() => value, () => [value, value]);
const operations = { '+': (a,b) => a+b, '-': (a,b) => a-b, '*': (a,b) => a*b,
  '/': (a,b) => a/b, '^': (a,b) => a**b };

// Intervalos conservadores: una división que puede atravesar cero no se dibuja ni integra.
function combine(op, left, right) {
  return node(x => operations[op](left(x), right(x)), (min,max) => {
    const a = left.bounds(min,max), b = right.bounds(min,max);
    if (![...a,...b].every(Number.isFinite)) return unknown;
    if (op === '+') return [a[0]+b[0],a[1]+b[1]];
    if (op === '-') return [a[0]-b[1],a[1]-b[0]];
    if (op === '/' && b[0] <= 0 && b[1] >= 0) return unknown;
    if (op === '^') {
      const integer = b[0] === b[1] && Number.isInteger(b[0]);
      if ((a[0] < 0 && !integer) || (a[0] <= 0 && a[1] >= 0 && b[0] < 0)) return unknown;
      const values = a.flatMap(v => b.map(p => v**p));
      if (integer && b[0] > 0 && b[0] % 2 === 0 && a[0] <= 0 && a[1] >= 0) values.push(0);
      return [Math.min(...values),Math.max(...values)];
    }
    const values = a.flatMap(v => b.map(w => operations[op](v,w)));
    return [Math.min(...values),Math.max(...values)];
  });
}

function functionBounds(name, fn, range) {
  const [lo,hi] = range;
  if (!range.every(Number.isFinite)) return unknown;
  if ((name === 'sqrt' && lo < 0) || (name === 'log' && lo <= 0)) return unknown;
  if (name === 'tan' && Math.floor((lo+Math.PI/2)/Math.PI) !== Math.floor((hi+Math.PI/2)/Math.PI)) return unknown;
  if (name === 'sin' || name === 'cos') {
    if (hi-lo >= 2*Math.PI) return [-1,1];
    const values = [fn(lo),fn(hi)], shift = name === 'sin' ? Math.PI/2 : 0;
    for (let k = Math.ceil((lo-shift)/Math.PI); k*Math.PI+shift <= hi; k++) values.push(fn(k*Math.PI+shift));
    return [Math.min(...values),Math.max(...values)];
  }
  const values = [fn(lo),fn(hi)];
  if (name === 'abs' && lo <= 0 && hi >= 0) values.push(0);
  return [Math.min(...values),Math.max(...values)];
}

export function expression(source) {
  if (!source || source.length > 200) throw new Error('Escribe una expresión de hasta 200 caracteres.');
  const tokens = source.match(/(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?|[a-z]+|[^\s]/g) || [];
  let cursor = 0, depth = 0;
  const functions = { sin: Math.sin, cos: Math.cos, tan: Math.tan, exp: Math.exp,
    log: Math.log, sqrt: Math.sqrt, abs: Math.abs };
  const error = () => { throw new Error('Expresión inválida. Usa x, números, + − * / ^ y sin, cos, tan, exp, log, sqrt o abs.'); };
  const take = token => tokens[cursor] === token && (++cursor > 0);
  function sum() {
    let left = product();
    while (tokens[cursor] === '+' || tokens[cursor] === '-') {
      const op = tokens[cursor++], a = left, b = product();
      left = combine(op,a,b);
    }
    return left;
  }
  function product() {
    let left = unary();
    while (tokens[cursor] === '*' || tokens[cursor] === '/') {
      const op = tokens[cursor++], a = left, b = unary();
      left = combine(op,a,b);
    }
    return left;
  }
  function unary() {
    if (++depth > 32) error();
    let result;
    if (take('+')) result = unary();
    else if (take('-')) { const value = unary(); result = combine('-',constant(0),value); }
    else {
      result = atom();
      if (take('^')) { const base = result, exponent = unary(); result = combine('^',base,exponent); }
    }
    depth--; return result;
  }
  function atom() {
    const token = tokens[cursor++];
    if (token === 'x') return node(x => x, (min,max) => [min,max]);
    if (token === 'pi') return constant(Math.PI);
    if (token === 'e') return constant(Math.E);
    if (/^(?:\d+(?:\.\d*)?|\.\d+)(?:e[+-]?\d+)?$/.test(token || '')) return constant(Number(token));
    if (token === '(') { const value = sum(); if (!take(')')) error(); return value; }
    if (Object.hasOwn(functions, token)) {
      if (!take('(')) error();
      const value = sum(); if (!take(')')) error();
      return node(x => functions[token](value(x)), (min,max) => functionBounds(token,functions[token],value.bounds(min,max)));
    }
    return error();
  }
  const evaluate = sum();
  if (cursor !== tokens.length) error();
  return evaluate;
}

/**
 * @brief Muestreo finito; derivada central e integral con signo por Simpson con control de error.
 * @return {{points:Array, area:number|null}} Área nula si hay discontinuidades detectadas.
 */
export function curve(source, min, max, samples = 256) {
  if (!Number.isFinite(min) || !Number.isFinite(max) || min >= max || min < -10000 || max > 10000
      || !Number.isInteger(samples) || samples < 16 || samples > 2048) throw new Error('Dominio o muestreo inválido.');
  const f = expression(source), step = (max - min) / samples;
  const finite = value => Number.isFinite(value) && Math.abs(value) <= 1000 ? value : null;
  const points = [];
  let area = 0;
  for (let i = 0; i <= samples; i++) {
    const x = min + step * i, y = finite(f(x)), h = 1e-4 * Math.max(1, Math.abs(x));
    const derivative = finite((f(x + h) - f(x - h)) / (2 * h));
    let connected = false;
    if (i > 0) {
      const previous = points[i - 1], middle = finite(f(x - step / 2));
      connected = previous.y !== null && y !== null && middle !== null && Math.abs(y - previous.y) < 50
        && f.bounds(previous.x,x).every(Number.isFinite);
      if (!connected) area = null;
      else if (area !== null) {
        const coarse = step * (previous.y + 4*middle + y) / 6;
        const q1 = finite(f(x - 3*step/4)), q3 = finite(f(x - step/4));
        const fine = step * (previous.y + 4*q1 + 2*middle + 4*q3 + y) / 12;
        if (q1 === null || q3 === null || Math.abs(coarse-fine) > 1e-6*Math.max(1,Math.abs(fine))) area = null;
        else area += fine;
      }
    }
    points.push({ x, y, derivative, connected });
  }
  return { points, area };
}

/** @brief Controles con salida textual: los cálculos son aproximaciones numéricas. */
export function mountCurveControls(document, viewer) {
  const one = id => document.querySelector('#' + id);
  one('curve-draw').disabled = !viewer;
  one('curve-form').addEventListener('submit', event => {
    event.preventDefault();
    try {
      const source = one('curve-function').value;
      const graph = curve(source, Number(one('curve-min').value), Number(one('curve-max').value));
      viewer.setGraph(graph);
      const integral = graph.area === null ? 'No se calcula la integral: hay discontinuidades, muestreo insuficiente o valores fuera del rango representable.'
        : 'Integral aproximada con signo: ' + graph.area.toLocaleString('es-ES', {maximumFractionDigits:6}) + '.';
      one('curve-result').textContent = `f(x) = ${source}. Función amarilla, derivada azul y área sombreada. ${integral} Derivada e integral numéricas; comprueba el dominio antes de interpretar el resultado.`;
    } catch (error) { one('curve-result').textContent = error.message; }
  });
}
