const vertexSource = `#version 300 es
layout(location = 0) in vec3 position;
layout(location = 1) in vec3 color;
uniform mat4 viewProjection;
out vec3 vertexColor;
void main() {
  gl_Position = viewProjection * vec4(position, 1.0);
  vertexColor = color;
}`;
const fragmentSource = `#version 300 es
precision mediump float;
in vec3 vertexColor;
out vec4 fragmentColor;
void main() { fragmentColor = vec4(vertexColor, 1.0); }`;

/** Pipeline WebGL2 propio. El VAO mantiene aislado el formato de los ejes. */
export class ShaderPipeline {
  constructor(gl) {
    this.gl = gl;
    const shaders = [];
    try {
      for (const [type, source] of [[gl.VERTEX_SHADER, vertexSource], [gl.FRAGMENT_SHADER, fragmentSource]]) {
        const shader = gl.createShader(type);
        shaders.push(shader);
        gl.shaderSource(shader, source);
        gl.compileShader(shader);
        if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) throw new Error('No se pudo compilar el shader');
      }
      this.program = gl.createProgram();
      for (const shader of shaders) gl.attachShader(this.program, shader);
      gl.linkProgram(this.program);
      if (!gl.getProgramParameter(this.program, gl.LINK_STATUS)) throw new Error('No se pudo enlazar el programa gráfico');
      this.matrixLocation = gl.getUniformLocation(this.program, 'viewProjection');
      this.vao = gl.createVertexArray();
      this.buffer = gl.createBuffer();
      gl.bindVertexArray(this.vao);
      gl.bindBuffer(gl.ARRAY_BUFFER, this.buffer);
      // Pares posición/color: X rojo, Y verde y Z azul; no anticipa gráficas de otros sprints.
      gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([
        -2, 0, 0, 0.9, 0.25, 0.3, 2, 0, 0, 0.9, 0.25, 0.3,
        0, -2, 0, 0.2, 0.75, 0.45, 0, 2, 0, 0.2, 0.75, 0.45,
        0, 0, -2, 0.3, 0.6, 1, 0, 0, 2, 0.3, 0.6, 1,
      ]), gl.STATIC_DRAW);
      for (let index = 0; index < 2; index++) {
        gl.enableVertexAttribArray(index);
        gl.vertexAttribPointer(index, 3, gl.FLOAT, false, 24, index * 12);
      }
      gl.bindVertexArray(null);
    } catch (error) {
      this.dispose();
      throw error;
    } finally {
      for (const shader of shaders) gl.deleteShader(shader);
    }
  }

  draw(matrix, width, height) {
    const gl = this.gl;
    gl.viewport(0, 0, width, height);
    gl.enable(gl.DEPTH_TEST);
    gl.clearColor(0.035, 0.065, 0.1, 1);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
    gl.useProgram(this.program);
    gl.bindVertexArray(this.vao);
    gl.uniformMatrix4fv(this.matrixLocation, false, matrix);
    gl.drawArrays(gl.LINES, 0, 6);
    gl.bindVertexArray(null);
  }

  dispose() {
    this.gl.deleteBuffer(this.buffer);
    this.gl.deleteVertexArray(this.vao);
    this.gl.deleteProgram(this.program);
  }
}
