import test from 'node:test';
import assert from 'node:assert/strict';
import { curve } from '../../main/resources/static/visor/curves.mjs';
import { surface } from '../../main/resources/static/visor/surfaces.mjs';
import { drawSpecification } from '../../main/resources/static/visor/graphic-spec.mjs';
import { ShaderPipeline } from '../../main/resources/static/visor/pipeline.mjs';
import { fakeGl } from './fake-gl.mjs';

const close = (value, expected) => assert.ok(Math.abs(value - expected) < 1e-5, `${value} ≠ ${expected}`);

test('la recta pasa por el punto exacto y su pendiente cambia al desplazarlo', () => {
  for (const x of [-1, 0.25, 1]) {
    const tangent = curve('x^2', -1, 1, 16, x).tangent;
    close(tangent.value, x * x);
    close(tangent.slope, 2 * x);
    for (const [u, v] of tangent.lines) close(v, x * x + 2 * x * (u - x));
  }
  close(curve('sin(x)', 4999, 5001, 16, 5000).tangent.slope, Math.cos(5000));
});

test('el plano se apoya en la superficie y tiene dos pendientes independientes', () => {
  const tangent = surface('x^2+2*y^2', -2, 2, -2, 2, 4, [1, -0.5]).tangent;
  close(tangent.value, 1.5);
  close(tangent.dx, 2);
  close(tangent.dy, -2);
  for (const [u, v, z] of tangent.lines) close(z, 1.5 + 2 * (u - 1) - 2 * (v + 0.5));
});

test('rechaza puntos fuera del dominio, esquinas, polos y superficies no diferenciables', () => {
  assert.throws(() => curve('x', -1, 1, 16, 2), /dominio/);
  for (const source of ['abs(x)', '1/x', 'sqrt(x)']) {
    assert.throws(() => curve(source, -1, 1, 16, 0), /tangente/);
  }
  assert.throws(() => surface('sqrt(abs(x*y))', -1, 1, -1, 1, 4, [0, 0]), /tangente/);
  assert.throws(() => surface('x*y', -1, 1, -1, 1, 4, [0, NaN]), /dominio/);
});

test('una especificación válida dibuja una tangente diferenciada en WebGL', () => {
  const gl = fakeGl({ LINES: 11 });
  const pipeline = new ShaderPipeline(gl);
  const domain = { min: -2, max: 2, steps: 16 };
  const points = [{ x: 1, y: -0.5, z: 0 }];
  const viewer = {
    setGraph: graph => pipeline.setGraph(graph),
    setSurface: mesh => pipeline.setSurface(mesh),
  };
  for (const [type, fn, label, geometryCount] of [
    ['RECTA_TANGENTE', 'x^2', /Recta tangente/, 2],
    ['PLANO_TANGENTE', 'x^2+y^2', /Plano tangente/, 12],
  ]) {
    assert.match(drawSpecification(viewer, { type, function: fn, domain, points }), label);
    pipeline.draw(new Float32Array(16), 400, 300);
    assert.ok(gl.calls.some(call => call[0] === 'drawArrays' && call[1] === 11 && call[3] === geometryCount));
  }
});

test('el relleno del plano sigue el punto y desaparece al quitar la tangente', () => {
  const gl = fakeGl();
  const pipeline = new ShaderPipeline(gl);
  const mesh = surface('x^2+y^2', -2, 2, -2, 2, 16, [0, 0]);
  const draw = () => {
    gl.calls.length = 0;
    pipeline.draw(new Float32Array(16), 400, 300);
    return gl.calls.filter(([call, mode]) => call === 'drawArrays' && mode === gl.TRIANGLES);
  };
  pipeline.setSurface(mesh);
  assert.equal(draw().at(-1)[3], 6);
  const fill = gl.calls.findIndex(([call, mode,, count]) => call === 'drawArrays' && mode === gl.TRIANGLES && count === 6);
  assert.ok(gl.calls.slice(0, fill).some(([call, mode]) => call === 'enable' && mode === gl.BLEND));
  assert.ok(gl.calls.slice(fill + 1).some(([call, mode]) => call === 'disable' && mode === gl.BLEND));

  const moved = mesh.tangentAt([1, -0.5]).tangent;
  pipeline.updateTangent(moved);
  const vertices = gl.calls.findLast(([call]) => call === 'bufferSubData').at(-1);
  assert.equal(vertices.length, (12 + 6) * 6);
  for (let i = 0; i < 36; i += 6) {
    const [x, y, z] = vertices.subarray(i, i + 3);
    close(z, moved.value + moved.dx * (x - 1) + moved.dy * (y + 0.5));
  }
  assert.equal(draw().at(-1)[3], 6);
  pipeline.updateTangent(null);
  assert.equal(draw().length, 1);
});
