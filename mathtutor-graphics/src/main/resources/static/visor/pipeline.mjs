import { linkProgram } from './shader-program.mjs';
import { PostProcessing } from './post-processing.mjs';

const vertexSource = `#version 300 es
layout(location = 0) in vec3 position;
layout(location = 1) in vec3 colorOrNormal;
uniform mat4 viewProjection;
out vec3 vertexColor;
out vec2 surfacePosition;
void main() {
  gl_Position = viewProjection * vec4(position, 1.0);
  gl_PointSize = 9.0;
  vertexColor = colorOrNormal;
  surfacePosition = position.xy;
}`;
const fragmentSource = `#version 300 es
precision mediump float;
in vec3 vertexColor;
in vec2 surfacePosition;
uniform bool surfaceMaterial;
out vec4 fragmentColor;
void main() {
  if (!surfaceMaterial) {
    fragmentColor = vec4(vertexColor, 1.0);
    return;
  }
  // Textura de cuadrícula declarada en las coordenadas matemáticas del plano XY.
  vec2 uv = surfacePosition * 2.0;
  vec2 footprint = max(fwidth(uv), vec2(0.0001));
  vec2 distance = abs(fract(uv + 0.5) - 0.5) / footprint;
  float grid = 1.0 - smoothstep(0.25, 1.15, min(distance.x, distance.y));
  grid *= 1.0 - smoothstep(0.25, 0.65, max(footprint.x, footprint.y));
  float diffuse = max(dot(normalize(vertexColor), normalize(vec3(-0.5, 0.35, 1.0))), 0.0);
  vec3 albedo = mix(vec3(0.25, 0.72, 0.62), vec3(0.12, 0.39, 0.34), grid);
  fragmentColor = vec4(albedo * (0.42 + 0.58 * diffuse), 1.0);
}`;

/** Pipeline WebGL2 propio. El VAO mantiene aislado el formato de los ejes. */
export class ShaderPipeline {
  constructor(gl) {
    this.gl = gl;
    try {
      this.program = linkProgram(gl, vertexSource, fragmentSource);
      this.matrixLocation = gl.getUniformLocation(this.program, 'viewProjection');
      this.materialLocation = gl.getUniformLocation(this.program, 'surfaceMaterial');
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
    this.upload([...lines, ...area], graph.tangent, null, Boolean(graph.secantAt));
  }

  /** @brief Dibuja la triangulación y conserva las normales calculadas por la malla. */
  setSurface(mesh, selection) {
    const vertices = [];
    for (const triangle of mesh.triangles) {
      for (const vertex of triangle.vertices) vertices.push(...vertex, ...triangle.normal);
    }
    this.surfaceCount = vertices.length / 6;
    this.lineCount = 0;
    this.areaCount = 0;
    this.upload(vertices, mesh.tangent, selection);
  }

  /** @brief Guarda al final del VBO las aristas de la tangente como guía superpuesta. */
  upload(positions, tangent, selection, reservePoint = false) {
    const overlay = tangent?.lines.flatMap(vertex => [...vertex, 1, 0.38, 0.72]) ?? [];
    this.tangentStart = 6 + positions.length / 6;
    this.tangentCount = overlay.length / 6;
    this.selectionIndex = selection ? this.tangentStart + this.tangentCount : null;
    const buffer = new Float32Array(this.axes.length + positions.length + overlay.length + (selection || reservePoint ? 6 : 0));
    buffer.set(this.axes);
    buffer.set(positions, this.axes.length);
    buffer.set(overlay, this.axes.length + positions.length);
    if (selection) buffer.set([...selection, 1, 0.94, 0.45], buffer.length - 6);
    this.gl.bindBuffer(this.gl.ARRAY_BUFFER, this.buffer);
    this.gl.bufferData(this.gl.ARRAY_BUFFER, buffer, this.gl.STATIC_DRAW);
  }

  /** @brief Actualiza solo los dos vértices de la recta y su punto móvil reservado en el VBO. */
  updateSecant(line) {
    const vertices = line.lines.flatMap(vertex => [...vertex, 1, 0.38, 0.72]);
    if (line.point) vertices.push(...line.point, 1, 0.94, 0.45);
    this.selectionIndex = line.point ? this.tangentStart + this.tangentCount : null;
    this.gl.bindBuffer(this.gl.ARRAY_BUFFER, this.buffer);
    this.gl.bufferSubData(this.gl.ARRAY_BUFFER, this.tangentStart * 6 * Float32Array.BYTES_PER_ELEMENT,
      new Float32Array(vertices));
  }

  /** @brief Activa el contraste bajo demanda. Devuelve false si el shader no se puede usar. */
  setContrast(enabled) {
    if (!enabled) {
      this.postProcessing?.dispose();
      this.postProcessing = null;
      return true;
    }
    try {
      this.postProcessing ??= new PostProcessing(this.gl);
      return true;
    } catch {
      return false;
    }
  }

  draw(matrix, width, height) {
    const gl = this.gl;
    const compose = this.postProcessing?.begin(width, height);
    gl.viewport(0, 0, width, height);
    gl.enable(gl.DEPTH_TEST);
    gl.clearColor(0.035, 0.065, 0.1, 1);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
    gl.useProgram(this.program);
    gl.bindVertexArray(this.vao);
    gl.uniformMatrix4fv(this.matrixLocation, false, matrix);
    gl.uniform1i(this.materialLocation, 0);
    gl.drawArrays(gl.LINES, 0, 6);
    if (this.lineCount) gl.drawArrays(gl.LINES, 6, this.lineCount);
    if (this.areaCount) gl.drawArrays(gl.TRIANGLES, 6 + this.lineCount, this.areaCount);
    if (this.surfaceCount) {
      gl.uniform1i(this.materialLocation, 1);
      gl.drawArrays(gl.TRIANGLES, 6, this.surfaceCount);
      gl.uniform1i(this.materialLocation, 0);
    }
    if (compose) this.postProcessing.compose();
    if (this.tangentCount || this.selectionIndex != null) {
      if (compose) {
        gl.useProgram(this.program);
        gl.bindVertexArray(this.vao);
      }
      gl.disable(gl.DEPTH_TEST);
      if (this.tangentCount) gl.drawArrays(gl.LINES, this.tangentStart, this.tangentCount);
      if (this.selectionIndex != null) gl.drawArrays(gl.POINTS, this.selectionIndex, 1);
      gl.enable(gl.DEPTH_TEST);
    }
    gl.bindVertexArray(null);
  }

  dispose() {
    this.postProcessing?.dispose();
    this.gl.deleteBuffer(this.buffer);
    this.gl.deleteVertexArray(this.vao);
    this.gl.deleteProgram(this.program);
  }
}
