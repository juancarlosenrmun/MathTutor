const focal = 1 / Math.tan(Math.PI / 8);
const near = 0.1, far = 200;

/** Cámara orbital en coordenadas de mundo; matrices columna mayor para GLSL. */
export class Camera {
  constructor() { this.reset(); }

  reset() {
    this.orthographicSpan = null;
    this.yaw = 0.6;
    this.pitch = 0.4;
    this.distance = 7;
    this.target = [0, 0, 0];
  }

  orbit(dx, dy) {
    this.yaw += dx;
    // Evita que el vector vertical sea paralelo a la dirección de la vista.
    this.pitch = Math.max(-1.5, Math.min(1.5, this.pitch + dy));
  }

  zoom(delta) {
    this.distance = Math.max(1, Math.min(50, this.distance * Math.exp(delta * 0.001)));
  }

  pan(dx, dy) {
    const scale = this.distance * 0.1;
    this.target[0] += (dx * Math.cos(this.yaw) - dy * Math.sin(this.yaw) * Math.sin(this.pitch)) * scale;
    this.target[1] += dy * Math.cos(this.pitch) * scale;
    this.target[2] += (-dx * Math.sin(this.yaw) - dy * Math.cos(this.yaw) * Math.sin(this.pitch)) * scale;
  }

  basis() {
    const cy = Math.cos(this.yaw), sy = Math.sin(this.yaw);
    const cp = Math.cos(this.pitch), sp = Math.sin(this.pitch);
    const right = [cy, 0, -sy];
    const up = [-sy * sp, cp, -cy * sp];
    const back = [sy * cp, sp, cy * cp];
    return {right, up, back};
  }

  halfHeight(aspect) {
    return Math.max(this.orthographicSpan[1] / 2, this.orthographicSpan[0] / (2 * aspect))
      * 1.15 * this.distance / 7;
  }

  /**
   * @brief Obtiene el rayo visible que atraviesa un punto de la pantalla normalizada.
   * @param x Coordenada horizontal entre -1 y 1.
   * @param y Coordenada vertical entre -1 y 1.
   * @param aspect Relación de aspecto del búfer dibujado.
   * @return Origen en el plano cercano, dirección y límite en el plano lejano.
   */
  ray(x, y, aspect) {
    const {right, up, back} = this.basis();
    const lateral = right.map((value, i) => x * aspect * value + y * up[i]);
    const orthographic = this.orthographicSpan !== null;
    const direction = back.map((value, i) => -value + (orthographic ? 0 : lateral[i] / focal));
    const origin = this.target.map((value, i) => value + this.distance * back[i] + near * direction[i]);
    if (orthographic) {
      const height = this.halfHeight(aspect);
      for (let i = 0; i < origin.length; i++) origin[i] += height * lateral[i];
    }
    return {origin, direction, limit: far - near};
  }

  matrix(aspect) {
    if (!Number.isFinite(aspect) || aspect <= 0) throw new Error('Relación de aspecto inválida');
    const {right, up, back} = this.basis();
    const dot = v => v.reduce((sum, value, i) => sum + value * this.target[i], 0);
    const view = [right[0], up[0], back[0], 0, right[1], up[1], back[1], 0,
      right[2], up[2], back[2], 0, -dot(right), -dot(up), -dot(back) - this.distance, 1];
    let projection;
    if (this.orthographicSpan) {
      const halfHeight = this.halfHeight(aspect);
      projection = [1/(halfHeight*aspect),0,0,0,0,1/halfHeight,0,0,0,0,
        -2/(far-near),0,0,0,-(far+near)/(far-near),1];
    } else projection = [focal / aspect, 0, 0, 0, 0, focal, 0, 0, 0, 0,
      (far + near) / (near - far), -1, 0, 0, 2 * far * near / (near - far), 0];
    const result = new Float32Array(16);
    for (let column = 0; column < 4; column++) {
      for (let row = 0; row < 4; row++) {
        for (let k = 0; k < 4; k++) result[column * 4 + row] += projection[k * 4 + row] * view[column * 4 + k];
      }
    }
    return result;
  }
}
