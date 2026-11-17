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
      // Pares posición/color: X rojo, Y verde y Z azul.
      this.axes = [
        -2, 0, 0, 0.9, 0.25, 0.3, 2, 0, 0, 0.9, 0.25, 0.3,
        0, -2, 0, 0.2, 0.75, 0.45, 0, 2, 0, 0.2, 0.75, 0.45,
        0, 0, -2, 0.3, 0.6, 1, 0, 0, 2, 0.3, 0.6, 1,
      ];
      gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(this.axes), gl.STATIC_DRAW);
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

  /** @brief Añade segmentos 2D y trapecios triangulados; no conecta puntos inválidos. */
  setGraph(graph) {
    this.surfaceCount = 0;
    const lines = [], area = [];
    const vertex = (out, x, y, color, z = 0) => out.push(x, y, z, ...color);
    for (let i = 1; i < graph.points.length; i++) {
      const a = graph.points[i - 1], b = graph.points[i];
      if (!b.connected) continue;
      for (const [field, color] of [['y', [0.95, 0.7, 0.25]], ['derivative', [0.4, 0.8, 1]]]) {
        if (a[field] === null || b[field] === null || Math.abs(a[field] - b[field]) >= 50) continue;
        vertex(lines, a.x, a[field], color); vertex(lines, b.x, b[field], color);
      }
      if (graph.area !== null) {
        const color = [0.12, 0.28, 0.24];
        for (const [x, y] of [[a.x, 0], [a.x, a.y], [b.x, b.y], [a.x, 0], [b.x, b.y], [b.x, 0]]) {
          vertex(area, x, y, color, -0.005);
        }
      }
    }
    this.lineCount = lines.length / 6; this.areaCount = area.length / 6;
    this.gl.bindBuffer(this.gl.ARRAY_BUFFER, this.buffer);
    this.gl.bufferData(this.gl.ARRAY_BUFFER, new Float32Array([...this.axes, ...lines, ...area]), this.gl.STATIC_DRAW);
  }

  /** @brief Dibuja la triangulación y conserva las normales calculadas por la malla. */
  setSurface(mesh) {
    const vertices = [];
    for (const triangle of mesh.triangles) {
      for (const vertex of triangle.vertices) vertices.push(...vertex, 0.25, 0.72, 0.62);
    }
    this.surfaceCount = vertices.length / 6;
    this.lineCount = 0;
    this.areaCount = 0;
    this.gl.bindBuffer(this.gl.ARRAY_BUFFER, this.buffer);
    this.gl.bufferData(this.gl.ARRAY_BUFFER, new Float32Array([...this.axes, ...vertices]), this.gl.STATIC_DRAW);
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
    if (this.lineCount) gl.drawArrays(gl.LINES, 6, this.lineCount);
    if (this.areaCount) gl.drawArrays(gl.TRIANGLES, 6 + this.lineCount, this.areaCount);
    if (this.surfaceCount) gl.drawArrays(gl.TRIANGLES, 6, this.surfaceCount);
    gl.bindVertexArray(null);
  }

  dispose() {
    this.gl.deleteBuffer(this.buffer);
    this.gl.deleteVertexArray(this.vao);
    this.gl.deleteProgram(this.program);
  }
}
