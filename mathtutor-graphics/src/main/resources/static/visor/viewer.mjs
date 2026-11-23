import { Camera } from './camera.mjs';
import { ShaderPipeline } from './pipeline.mjs';
import { raySurface, visiblePlane } from './intersections.mjs';

/** Dibuja bajo demanda: no consume GPU mientras la cámara permanece inmóvil. */
export class WebGLViewer {
  constructor(canvas, status, host = globalThis) {
    this.canvas = canvas;
    this.status = status;
    this.host = host;
    this.camera = new Camera();
    this.gl = canvas.getContext('webgl2');
    if (!this.gl) throw new Error('Este navegador no dispone de WebGL 2. Prueba otro navegador o activa la aceleración gráfica.');
    this.pipeline = new ShaderPipeline(this.gl);
    this.listeners = new Map();
    const on = (name, fn, options) => {
      canvas.addEventListener(name, fn, options);
      this.listeners.set(name, fn);
    };
    on('pointerdown', event => {
      if (event.button !== undefined && event.button !== 0) return;
      this.stopAnimation();
      canvas.setPointerCapture(event.pointerId);
      this.drag = { id: event.pointerId, x: event.clientX, y: event.clientY, moved: false };
    });
    on('pointermove', event => {
      if (!this.drag || event.pointerId !== this.drag.id) return;
      const dx = event.clientX - this.drag.x, dy = event.clientY - this.drag.y;
      if (!this.drag.moved && Math.hypot(dx, dy) < 4) return;
      this.drag.moved = true;
      if (event.shiftKey) this.camera.pan(-dx * 0.02, dy * 0.02);
      else this.camera.orbit(-dx * 0.01, dy * 0.01);
      this.drag.x = event.clientX;
      this.drag.y = event.clientY;
      this.render();
    });
    on('pointerup', event => {
      if (!event || this.drag?.id !== event.pointerId) return;
      if (!this.drag.moved) this.select(event.clientX, event.clientY);
      this.drag = null;
    });
    on('pointercancel', () => { this.drag = null; });
    on('wheel', event => {
      event.preventDefault();
      this.stopAnimation();
      this.camera.zoom(event.deltaY);
      this.render();
    }, { passive: false });
    on('keydown', event => {
      const direction = { ArrowLeft: [-0.1, 0], ArrowRight: [0.1, 0], ArrowUp: [0, -0.1], ArrowDown: [0, 0.1] }[event.key];
      if (direction) {
        this.stopAnimation();
        if (event.shiftKey) this.camera.pan(...direction);
        else this.camera.orbit(...direction);
      } else if (event.key === '+' || event.key === '-') {
        this.stopAnimation();
        this.camera.zoom(event.key === '+' ? -100 : 100);
      } else if (event.key === 'Home') {
        event.preventDefault();
        this.command('reset');
        return;
      } else if (event.key === 'Enter') {
        event.preventDefault();
        this.stopAnimation();
        const { left, top, width, height } = canvas.getBoundingClientRect();
        this.select(left + width / 2, top + height / 2);
        return;
      }
      else return;
      event.preventDefault();
      this.render();
    });
    on('webglcontextlost', event => {
      event.preventDefault();
      this.lost = true;
      this.stopAnimation();
      this.drag = null;
      status.textContent = 'Contexto gráfico perdido. Esperando recuperación.';
      this.onAnimationChange?.();
    });
    on('webglcontextrestored', () => {
      try {
        this.pipeline = new ShaderPipeline(this.gl);
        if (this.contrast) this.pipeline.setContrast(true);
        if (this.graph) this.pipeline.setGraph(this.graph);
        if (this.surface) this.pipeline.setSurface(this.surface, this.selection);
        this.lost = false;
        this.onAnimationChange?.();
        this.render();
      } catch (error) { status.textContent = error.message; }
    });
    this.resize = () => this.render();
    host.addEventListener('resize', this.resize);
    this.render();
  }

  command(action) {
    this.stopAnimation();
    if (action === 'reset') {
      this.camera.reset();
      if (this.graph) { this.setGraph(this.graph); return; }
      if (this.surface) { this.setSurface(this.surface); return; }
    }
    else this.camera.zoom(action === 'in' ? -150 : 150);
    this.render();
  }

  /** @brief Orienta el visor hacia el plano XY y conserva la geometría al recuperar contexto. */
  clear() {
    this.stopAnimation();
    this.selection = null;
    this.graph=null; this.surface=null;
    if (!this.lost) this.pipeline.setGraph({points:[],area:null});
    this.camera.reset(); this.render();
    this.onAnimationChange?.();
  }

  setGraph(graph) {
    this.stopAnimation();
    this.selection = null;
    this.graph = graph;
    this.surface = null;
    if (!this.lost) this.pipeline.setGraph(graph);
    this.camera.yaw = 0; this.camera.pitch = 0;
    const values = [0, ...graph.points.flatMap(p => [p.y,p.derivative]).filter(v => v !== null)];
    const minY = Math.min(...values), maxY = Math.max(...values);
    this.camera.target = [(graph.points[0].x + graph.points.at(-1).x) / 2, (minY+maxY)/2, 0];
    this.camera.orthographicSpan = [graph.points.at(-1).x - graph.points[0].x, Math.max(2,maxY-minY)];
    this.camera.distance = 7;
    this.render();
    this.onAnimationChange?.();
  }

  /** @brief Centra una malla de superficie y permite recuperar el contexto WebGL. */
  setSurface(mesh) {
    this.stopAnimation();
    this.graph = null;
    if (this.surface !== mesh) this.selection = null;
    this.surface = mesh;
    if (!this.lost) this.pipeline.setSurface(mesh, this.selection);
    const [xMin,xMax,yMin,yMax] = mesh.bounds;
    const heights = mesh.triangles.flatMap(t => t.vertices.map(v => v[2]));
    const [minZ,maxZ] = heights.reduce(([min,max], z) => [Math.min(min,z),Math.max(max,z)], [0,0]);
    this.camera.target = [(xMin+xMax)/2, (yMin+yMax)/2, (minZ+maxZ)/2];
    this.camera.orthographicSpan = [xMax-xMin, Math.max(yMax-yMin,maxZ-minZ,2)];
    this.camera.yaw = 0.6;
    this.camera.pitch = 0.4;
    this.render();
    this.onAnimationChange?.();
  }

  /** @brief Indica qué recorrido corresponde a la geometría que se está mostrando. */
  canAnimate(kind) {
    if (this.lost || !this.host.requestAnimationFrame) return false;
    return kind === 'secant' ? Boolean(this.graph?.secantAt) : kind === 'camera';
  }

  /** @brief Reproduce por tiempo real y ajusta el movimiento a la preferencia del navegador. */
  playAnimation(kind) {
    if (!this.canAnimate(kind)) return;
    this.stopAnimation();
    const { yaw, pitch, distance } = this.camera;
    const secant = kind === 'secant';
    const travel = (horizontal, vertical) => {
      if (this.graph) {
        this.camera.distance = distance;
        this.camera.zoom(220 * vertical);
      } else {
        this.camera.yaw = yaw;
        this.camera.pitch = pitch;
        this.camera.orbit(0.65 * horizontal, 0.18 * vertical);
      }
    };
    if (this.host.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches) {
      if (!secant) travel(1, 1);
      this.render();
      this.onAnimationChange?.(kind, 'reduced');
      return;
    }

    const duration = secant ? 2000 : 4000;
    let started;
    const frame = now => {
      started ??= now;
      const progress = Math.min(1, (now - started) / duration);
      if (secant) {
        this.pipeline.updateSecant(this.graph.secantAt(progress));
      } else if (progress < 1) {
        travel(Math.sin(2 * Math.PI * progress), Math.sin(Math.PI * progress));
      } else {
        travel(0, 0);
      }
      this.render();
      if (progress < 1) {
        this.animation.frame = this.host.requestAnimationFrame(frame);
      } else {
        this.animation = null;
        this.onAnimationChange?.(kind, 'finished');
      }
    };
    this.animation = { kind, frame: this.host.requestAnimationFrame(frame) };
    this.onAnimationChange?.(kind, 'playing');
  }

  /** @brief Cancela el fotograma programado y vuelve a mostrar la tangente del modelo. */
  stopAnimation() {
    if (!this.animation) return;
    const { kind, frame } = this.animation;
    this.host.cancelAnimationFrame(frame);
    this.animation = null;
    if (kind === 'secant' && !this.lost) {
      this.pipeline.updateSecant(this.graph.tangent);
      this.render();
    }
    this.onAnimationChange?.(kind, 'stopped');
  }

  /**
   * @brief Selecciona un punto del objeto activo a partir de un clic en el lienzo.
   * @param clientX Coordenada horizontal del puntero en la ventana.
   * @param clientY Coordenada vertical del puntero en la ventana.
   */
  select(clientX, clientY) {
    if (this.lost || !this.surface || !this.selectionMode) return;
    const {left, top, width, height} = this.canvas.getBoundingClientRect();
    if (!width || !height) return;
    const x = 2 * (clientX - left) / width - 1;
    const y = 1 - 2 * (clientY - top) / height;
    if (Math.abs(x) > 1 || Math.abs(y) > 1) return;
    const ray = this.camera.ray(x, y, this.canvas.width / this.canvas.height);
    this.selection = this.selectionMode.value === 'plane'
      ? visiblePlane(ray, this.surface.tangent)
      : raySurface(ray.origin, ray.direction, this.surface.triangles, ray.limit);
    this.pipeline.setSurface(this.surface, this.selection);
    this.render();
    if (!this.selection) this.status.textContent = 'No hay intersección visible en ese punto.';
  }

  /** @brief Configura el contraste del visor y conserva la preferencia al recuperar WebGL. */
  setContrast(enabled) {
    if (!this.lost && !this.pipeline.setContrast(enabled)) return false;
    this.contrast = enabled;
    this.render();
    return true;
  }

  render() {
    if (this.lost) return;
    const ratio = Math.min(this.host.devicePixelRatio || 1, 2);
    const width = Math.max(1, Math.round(this.canvas.clientWidth * ratio));
    const height = Math.max(1, Math.round(this.canvas.clientHeight * ratio));
    if (this.canvas.width !== width || this.canvas.height !== height) {
      this.canvas.width = width;
      this.canvas.height = height;
    }
    this.pipeline.draw(this.camera.matrix(width / height), width, height);
    const status = this.selection
      ? `Punto seleccionado (x, y, z) = (${this.selection.map(value => value.toLocaleString('es-ES', {maximumFractionDigits: 4})).join('; ')}).`
      : 'Visor listo. Ejes X rojo, Y verde y Z azul.';
    if (this.status.textContent !== status) this.status.textContent = status;
  }

  dispose() {
    this.stopAnimation();
    for (const [name, fn] of this.listeners) this.canvas.removeEventListener(name, fn);
    this.host.removeEventListener('resize', this.resize);
    this.pipeline.dispose();
  }
}
