import test from 'node:test';
import assert from 'node:assert/strict';
import { ShaderPipeline } from '../../main/resources/static/visor/pipeline.mjs';
import { fakeGl } from './fake-gl.mjs';
import { curve } from '../../main/resources/static/visor/curves.mjs';
import { surface } from '../../main/resources/static/visor/surfaces.mjs';

test('UC-29 envía triángulos de superficie al pipeline WebGL2', () => {
  const gl = fakeGl({ TRIANGLES: 10 });
  const pipeline = new ShaderPipeline(gl);
  pipeline.setSurface(surface('x*y', -1, 1, -1, 1, 2));
  pipeline.draw(new Float32Array(16), 800, 600);
  assert.ok(gl.calls.some(c => c[0] === 'drawArrays' && c[1] === 10 && c[3] === 24));
});

test('UC-30 carga las normales de la malla y aísla el material de la tangente', () => {
  const gl = fakeGl({ TRIANGLES: 10 });
  const pipeline = new ShaderPipeline(gl);
  const mesh = surface('x+2*y', -1, 1, -1, 1, 2, [0, 0]);
  pipeline.setSurface(mesh);
  const data = gl.calls.findLast(([name]) => name === 'bufferData')[2];
  assert.deepEqual([...data.slice(39, 42)], mesh.triangles[0].normal.map(Math.fround));
  gl.calls.length = 0;
  pipeline.draw(new Float32Array(16), 800, 600);
  assert.deepEqual(gl.calls.filter(([name]) => ['uniform1i', 'drawArrays'].includes(name))
    .map(([name, , value]) => [name, value]), [
    ['uniform1i', 0], ['drawArrays', 0], ['uniform1i', 1], ['drawArrays', 6],
    ['uniform1i', 0], ['drawArrays', 30],
  ]);
});

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

function effectsGl(overrides = {}) {
  const buffers = [];
  return fakeGl({
    TRIANGLES: 10, POINTS: 11, FRAMEBUFFER: 12, FRAMEBUFFER_COMPLETE: 13,
    createFramebuffer: () => ({}), createTexture: () => ({}), createRenderbuffer: () => ({}),
    checkFramebufferStatus: () => 13,
    ...Object.fromEntries(['bindFramebuffer', 'bindTexture', 'texImage2D', 'texParameteri',
      'framebufferTexture2D', 'bindRenderbuffer', 'renderbufferStorage', 'framebufferRenderbuffer',
      'activeTexture', 'deleteFramebuffer', 'deleteRenderbuffer', 'deleteTexture']
      .map(name => [name, (...args) => buffers.push([name, ...args])])),
    buffers, ...overrides,
  });
}

test('UC-34 compone la textura antes de dibujar guías y reutiliza el framebuffer hasta redimensionar', () => {
  const gl = effectsGl();
  const pipeline = new ShaderPipeline(gl);
  pipeline.setSurface(surface('x+y', -1, 1, -1, 1, 2, [0, 0]), [0, 0, 0]);
  const draw = (width = 800) => pipeline.draw(new Float32Array(16), width, 600);
  draw();
  assert.equal(gl.buffers.length, 0);
  assert.equal(pipeline.setContrast(true), true);
  gl.calls.length = 0;
  draw();
  assert.deepEqual(gl.calls.filter(([name]) => name === 'drawArrays').map(call => call.slice(1)), [
    [gl.LINES, 0, 6], [gl.TRIANGLES, 6, 24], [gl.TRIANGLES, 0, 3],
    [gl.LINES, 30, 12], [gl.POINTS, 42, 1],
  ]);
  draw();
  assert.equal(gl.buffers.filter(([name]) => name === 'texImage2D').length, 1);
  draw(801);
  assert.equal(gl.buffers.filter(([name]) => name === 'texImage2D').length, 2);
  assert.equal(pipeline.setContrast(false), true);
  assert.equal(gl.buffers.filter(([name]) => name === 'deleteFramebuffer').length, 1);
  pipeline.setContrast(true);
  draw();
  pipeline.dispose();
  for (const resource of ['Framebuffer', 'Renderbuffer', 'Texture']) {
    assert.equal(gl.buffers.filter(([name]) => name === 'delete' + resource).length, 2);
  }
});

test('UC-34 libera los recursos parciales si no se puede crear la textura', () => {
  const gl = effectsGl({ createTexture: () => null });
  const pipeline = new ShaderPipeline(gl);
  assert.equal(pipeline.setContrast(true), false);
  assert.ok(gl.buffers.some(([name]) => name === 'deleteFramebuffer'));
  assert.ok(gl.buffers.some(([name]) => name === 'deleteRenderbuffer'));
  pipeline.draw(new Float32Array(16), 800, 600);
  assert.equal(gl.buffers.some(([name]) => name === 'bindFramebuffer'), false);
});

test('UC-34 dibuja directamente si el framebuffer es incompleto o falla la compilación del efecto', () => {
  const gl = effectsGl({ checkFramebufferStatus: () => 0 });
  const pipeline = new ShaderPipeline(gl);
  pipeline.setContrast(true);
  pipeline.draw(new Float32Array(16), 800, 600);
  assert.ok(gl.calls.some(([name, mode, start, count]) => name === 'drawArrays' && mode === 9 && start === 0 && count === 6));
  assert.equal(gl.calls.filter(([name]) => name === 'drawArrays').length, 1);
  assert.deepEqual(gl.buffers.findLast(([name]) => name === 'bindFramebuffer').slice(2), [null]);
  gl.getProgramParameter = () => false;
  pipeline.setContrast(false);
  assert.equal(pipeline.setContrast(true), false);
});
