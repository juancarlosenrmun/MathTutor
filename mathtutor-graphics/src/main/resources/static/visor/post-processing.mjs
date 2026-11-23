import { linkProgram } from './shader-program.mjs';

const vertexSource = `#version 300 es
out vec2 uv;
void main() {
  uv = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
  gl_Position = vec4(uv * 2.0 - 1.0, 0.0, 1.0);
}`;
const fragmentSource = `#version 300 es
precision mediump float;
uniform sampler2D scene;
in vec2 uv;
out vec4 color;
void main() {
  vec3 base = texture(scene, uv).rgb;
  color = vec4(mix(base, smoothstep(0.0, 1.0, base), 0.65), 1.0);
}`;

/** @brief Conserva una textura de escena y compone una pasada de contraste a pantalla completa. */
export class PostProcessing {
  constructor(gl) {
    this.gl = gl;
    this.program = linkProgram(gl, vertexSource, fragmentSource);
    this.sceneLocation = gl.getUniformLocation(this.program, 'scene');
    this.framebuffer = gl.createFramebuffer();
    this.color = gl.createTexture();
    this.depth = gl.createRenderbuffer();
  }

  /**
   * @brief Prepara color y profundidad del tamaño del lienzo.
   * @return true si la escena se puede dibujar a textura. Si falla, deja enlazada la pantalla.
   */
  begin(width, height) {
    const gl = this.gl;
    gl.bindFramebuffer(gl.FRAMEBUFFER, this.framebuffer);
    if (this.width !== width || this.height !== height) {
      gl.activeTexture(gl.TEXTURE0);
      gl.bindTexture(gl.TEXTURE_2D, this.color);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
      gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA8, width, height, 0, gl.RGBA, gl.UNSIGNED_BYTE, null);
      gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, this.color, 0);
      gl.bindTexture(gl.TEXTURE_2D, null);
      gl.bindRenderbuffer(gl.RENDERBUFFER, this.depth);
      gl.renderbufferStorage(gl.RENDERBUFFER, gl.DEPTH_COMPONENT16, width, height);
      gl.framebufferRenderbuffer(gl.FRAMEBUFFER, gl.DEPTH_ATTACHMENT, gl.RENDERBUFFER, this.depth);
      gl.bindRenderbuffer(gl.RENDERBUFFER, null);
      this.width = width;
      this.height = height;
      this.ready = Boolean(this.framebuffer && this.color && this.depth &&
        gl.checkFramebufferStatus(gl.FRAMEBUFFER) === gl.FRAMEBUFFER_COMPLETE);
    }
    if (!this.ready) gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    return this.ready;
  }

  /** @brief Presenta la escena procesada y deja libre la unidad de textura. */
  compose() {
    const gl = this.gl;
    gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    gl.disable(gl.DEPTH_TEST);
    gl.useProgram(this.program);
    gl.bindVertexArray(null);
    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, this.color);
    gl.uniform1i(this.sceneLocation, 0);
    gl.drawArrays(gl.TRIANGLES, 0, 3);
    gl.bindTexture(gl.TEXTURE_2D, null);
  }

  /** @brief Libera los adjuntos del framebuffer y el programa de composición. */
  dispose() {
    const gl = this.gl;
    gl.deleteFramebuffer(this.framebuffer);
    gl.deleteRenderbuffer(this.depth);
    gl.deleteTexture(this.color);
    gl.deleteProgram(this.program);
  }
}
