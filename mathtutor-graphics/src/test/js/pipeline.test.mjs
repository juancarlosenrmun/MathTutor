import test from 'node:test';
import assert from 'node:assert/strict';
import { ShaderPipeline } from '../../main/resources/static/visor/pipeline.mjs';
import { fakeGl } from './fake-gl.mjs';
import { curve } from '../../main/resources/static/visor/curves.mjs';

test('UC-28 dibuja función, derivada y triángulos de área sin puentear discontinuidades', () => {
  const gl = fakeGl({ TRIANGLES: 10 });
  const pipeline = new ShaderPipeline(gl);
  pipeline.setGraph(curve('x^2', -1, 1, 16));
  pipeline.draw(new Float32Array(16), 800, 600);
  assert.ok(gl.calls.some(c => c[0] === 'drawArrays' && c[1] === 10 && c[3] === 96));
  const bad = curve('1/x', -1, 1, 16);
  pipeline.setGraph(bad);
  assert.equal(pipeline.areaCount, 0);
  assert.ok(pipeline.lineCount < 64);
  pipeline.setGraph(curve('sqrt(x)', -1, 1, 16));
  assert.ok(pipeline.lineCount > 0);
});

test('compila GLSL 300, dibuja seis vértices y libera recursos', () => {
  const gl = fakeGl();
  const pipeline = new ShaderPipeline(gl);
  pipeline.draw(new Float32Array(16), 800, 600);
  assert.ok(gl.calls.some(([name, , src]) => name === 'shaderSource' && src.includes('#version 300 es')));
  assert.ok(gl.calls.some(call => call[0] === 'drawArrays' && call[3] === 6));
  assert.ok(gl.calls.some(call => call[0] === 'viewport' && call[3] === 800 && call[4] === 600));
  pipeline.dispose();
  for (const name of ['deleteBuffer', 'deleteVertexArray', 'deleteProgram']) {
    assert.ok(gl.calls.some(call => call[0] === name));
  }
});

test('falla de forma controlada si el compilador o el enlazador rechazan los shaders', () => {
  for (const overrides of [{ getShaderParameter: () => false }, { getProgramParameter: () => false }]) {
    const gl = fakeGl(overrides);
    assert.throws(() => new ShaderPipeline(gl), /shader|programa/);
    assert.ok(gl.calls.some(call => call[0] === 'deleteShader'));
  }
});
