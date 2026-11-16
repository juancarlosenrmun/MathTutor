/** Cámara orbital en coordenadas de mundo; matrices columna mayor para GLSL. */
export class Camera {
  constructor() { this.reset(); }

  reset() {
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

  matrix(aspect) {
    if (!Number.isFinite(aspect) || aspect <= 0) throw new Error('Relación de aspecto inválida');
    const cy = Math.cos(this.yaw), sy = Math.sin(this.yaw);
    const cp = Math.cos(this.pitch), sp = Math.sin(this.pitch);
    const right = [cy, 0, -sy];
    const up = [-sy * sp, cp, -cy * sp];
    const back = [sy * cp, sp, cy * cp];
    const dot = v => v.reduce((sum, value, i) => sum + value * this.target[i], 0);
    const view = [right[0], up[0], back[0], 0, right[1], up[1], back[1], 0,
      right[2], up[2], back[2], 0, -dot(right), -dot(up), -dot(back) - this.distance, 1];
    const f = 1 / Math.tan(Math.PI / 8), near = 0.1, far = 200;
    const projection = [f / aspect, 0, 0, 0, 0, f, 0, 0, 0, 0,
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
