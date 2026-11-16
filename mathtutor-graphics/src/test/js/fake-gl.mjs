/** Doble del contexto WebGL para comprobar contratos sin GPU en pruebas unitarias. */
export function fakeGl(overrides = {}) {
  const calls = [];
  const gl = { calls, VERTEX_SHADER: 1, FRAGMENT_SHADER: 2, COMPILE_STATUS: 3, LINK_STATUS: 4,
    ARRAY_BUFFER: 5, STATIC_DRAW: 6, FLOAT: 7, DEPTH_TEST: 8, COLOR_BUFFER_BIT: 16,
    DEPTH_BUFFER_BIT: 32, LINES: 9,
    createShader: () => ({}), getShaderParameter: () => true,
    createProgram: () => ({}), getProgramParameter: () => true,
    createBuffer: () => ({}), createVertexArray: () => ({}), getUniformLocation: () => ({}),
  };
  for (const name of ['shaderSource', 'compileShader', 'attachShader', 'linkProgram', 'deleteShader',
    'deleteProgram', 'deleteBuffer', 'deleteVertexArray', 'bindBuffer', 'bufferData', 'bindVertexArray',
    'enableVertexAttribArray', 'vertexAttribPointer', 'viewport', 'clearColor', 'clear', 'enable',
    'useProgram', 'uniformMatrix4fv', 'drawArrays']) gl[name] = (...args) => calls.push([name, ...args]);
  return Object.assign(gl, overrides);
}
