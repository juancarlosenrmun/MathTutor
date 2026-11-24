/**
 * @brief Compila y enlaza un par GLSL, liberando los shaders en cualquier resultado.
 * @param gl Contexto WebGL2 propietario del programa.
 * @param vertexSource Código GLSL de vértices.
 * @param fragmentSource Código GLSL de fragmentos.
 * @return Programa enlazado que el llamador debe liberar.
 */
export function linkProgram(gl, vertexSource, fragmentSource) {
  const shaders = [];
  let program;
  try {
    for (const [type, source] of [[gl.VERTEX_SHADER, vertexSource], [gl.FRAGMENT_SHADER, fragmentSource]]) {
      const shader = gl.createShader(type);
      shaders.push(shader);
      gl.shaderSource(shader, source);
      gl.compileShader(shader);
      if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) throw new Error('No se pudo compilar el shader');
    }
    program = gl.createProgram();
    for (const shader of shaders) gl.attachShader(program, shader);
    gl.linkProgram(program);
    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) throw new Error('No se pudo enlazar el programa gráfico');
    return program;
  } catch (error) {
    if (program) gl.deleteProgram(program);
    throw error;
  } finally {
    for (const shader of shaders) gl.deleteShader(shader);
  }
}
