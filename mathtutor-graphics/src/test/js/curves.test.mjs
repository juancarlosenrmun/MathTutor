import { test } from 'node:test';
import assert from 'node:assert/strict';
import { expression, curve, mountCurveControls } from '../../main/resources/static/visor/curves.mjs';
import { element } from './ui-fixture.mjs';
import { spawnSync } from 'node:child_process';

test('precedencia, potencias, signos y funciones permitidas', () => {
  for (const [source, x, expected] of [['x^2', 3, 9], ['-x^2', 3, -9], ['2^-2', 0, .25],
    ['2^3^2', 0, 512], ['sin(pi/2)+cos(0)', 0, 2], ['sqrt(4)+abs(-2)', 0, 4],
    ['log(e)+exp(0)', 0, 2], ['tan(0)', 0, 0], ['(x+1)*2/4-1', 3, 1], ['.5 + 1e-2', 0, .51]]) {
    assert.ok(Math.abs(expression(source)(x) - expected) < 1e-8, source);
  }
});
test('rechaza código, identificadores ajenos, sintaxis y complejidad sin eval', () => {
  for (const source of ['', 'x;', 'globalThis.alert(1)', 'x.constructor', 'sin x', 'sin()', 'x x',
    'y', '2**3', '(', 'x+', '1..2', 'a'.repeat(201), '('.repeat(40)+'x'+')'.repeat(40)]) {
    assert.throws(() => expression(source), Error, source);
  }
});
test('función, derivada y área definida con tolerancia numérica', () => {
  const graph = curve('x^2', -1, 1, 512);
  assert.equal(graph.points.length, 513);
  assert.ok(Math.abs(graph.area - 2/3) < 1e-4);
  assert.ok(Math.abs(graph.points[512].derivative - 2) < 1e-5);
  assert.equal(graph.points[256].y, 0);
  assert.ok(curve('-2', 0, 1).area < 0);
});
test('discontinuidades y raíces fuera del dominio no producen áreas falsas', () => {
  assert.equal(curve('1/x', -1, 1).area, null);
  assert.equal(curve('sqrt(x)', -1, 1).area, null);
  assert.ok(curve('sqrt(x)', -1, 1).points.some(p => p.y === null));
  assert.equal(curve('exp(1000)', -1, 1).area, null);
  assert.equal(curve('0.001/(x-0.123)', -1, 1).area, null);
  assert.ok(curve('0.001/(x-0.123)', -1, 1).points.some(p => !p.connected && p.x > -1));
  assert.equal(curve('cos(256*pi*x)', -1, 1).area, null);
  assert.equal(curve('cos(1024*pi*x)', -1, 1).area, null);
});

test('argumentos trigonométricos grandes terminan con trabajo acotado', () => {
  const module = new URL('../../main/resources/static/visor/curves.mjs',import.meta.url).href;
  const result = spawnSync(process.execPath,['--input-type=module','-e',
    `import {curve} from ${JSON.stringify(module)}; curve('sin(1e20)',-1,1); curve('cos(1e20+x*1e20)',-1,1);`],
    {timeout:2000,windowsHide:true});
  assert.ifError(result.error); assert.equal(result.status,0);
});
test('valores límite de dominio y muestras', () => {
  for (const [a,b,n] of [[0,0,128], [2,1,128], [NaN,1,128], [-10001,1,128],
    [0,10001,128], [0,1,15], [0,1,2049], [0,1,16.5]]) assert.throws(() => curve('x', a,b,n));
  assert.equal(curve('x', 0, 1, 16).points.length, 17);
  assert.equal(curve('x', -10000, 10000, 2048).points.length, 2049);
});

test('controles conectados al visor y alternativa textual muestran área y errores', () => {
  const nodes = Object.fromEntries(['curve-form','curve-function','curve-min','curve-max','curve-result','curve-draw'].map(k => [k,element()]));
  const document = {querySelector:s => nodes[s.slice(1)]};
  let graph;
  mountCurveControls(document,{setGraph:g => graph = g});
  nodes['curve-function'].value = 'x^2'; nodes['curve-min'].value = '-1'; nodes['curve-max'].value = '1';
  nodes['curve-form'].listeners.submit({preventDefault(){}});
  assert.ok(graph.area > .66); assert.match(nodes['curve-result'].textContent,/0,666/);
  nodes['curve-function'].value = '1/x'; nodes['curve-form'].listeners.submit({preventDefault(){}});
  assert.match(nodes['curve-result'].textContent,/discontinuidades/);
  nodes['curve-function'].value = 'alert(1)'; nodes['curve-form'].listeners.submit({preventDefault(){}});
  assert.match(nodes['curve-result'].textContent,/inválida/);
  mountCurveControls(document,null);
  nodes['curve-function'].value = 'x^2';
  nodes['curve-form'].listeners.submit({preventDefault(){}});
  assert.equal(nodes['curve-draw'].disabled,false);
  assert.match(nodes['curve-result'].textContent,/Valores de f\(x\) muestreados entre 0 y 1/);
});
