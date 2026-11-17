import { Camera } from './camera.mjs';
import { ShaderPipeline } from './pipeline.mjs';

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
      canvas.setPointerCapture(event.pointerId);
      this.drag = { id: event.pointerId, x: event.clientX, y: event.clientY };
    });
    on('pointermove', event => {
      if (!this.drag || event.pointerId !== this.drag.id) return;
      const dx = event.clientX - this.drag.x, dy = event.clientY - this.drag.y;
      if (event.shiftKey) this.camera.pan(-dx * 0.02, dy * 0.02);
      else this.camera.orbit(-dx * 0.01, dy * 0.01);
      this.drag.x = event.clientX;
      this.drag.y = event.clientY;
      this.render();
    });
    on('pointerup', () => { this.drag = null; });
    on('pointercancel', () => { this.drag = null; });
    on('wheel', event => {
      event.preventDefault();
      this.camera.zoom(event.deltaY);
      this.render();
    }, { passive: false });
    on('keydown', event => {
      const direction = { ArrowLeft: [-0.1, 0], ArrowRight: [0.1, 0], ArrowUp: [0, -0.1], ArrowDown: [0, 0.1] }[event.key];
      if (direction) {
        if (event.shiftKey) this.camera.pan(...direction);
        else this.camera.orbit(...direction);
      } else if (event.key === '+') this.camera.zoom(-100);
      else if (event.key === '-') this.camera.zoom(100);
      else if (event.key === 'Home') { this.command('reset'); return; }
      else return;
      event.preventDefault();
      this.render();
    });
    on('webglcontextlost', event => {
      event.preventDefault();
      this.lost = true;
      this.drag = null;
      status.textContent = 'Contexto gráfico perdido. Esperando recuperación.';
    });
    on('webglcontextrestored', () => {
      try {
        this.pipeline = new ShaderPipeline(this.gl);
        if (this.graph) this.pipeline.setGraph(this.graph);
        if (this.surface) this.pipeline.setSurface(this.surface);
        this.lost = false;
        this.render();
      } catch (error) { status.textContent = error.message; }
    });
    this.resize = () => this.render();
    host.addEventListener('resize', this.resize);
    this.render();
  }

  command(action) {
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
    this.graph=null; this.surface=null;
    if (!this.lost) this.pipeline.setGraph({points:[],area:null});
    this.camera.reset(); this.render();
  }

  setGraph(graph) {
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
  }

  /** @brief Centra una malla de superficie y permite recuperar el contexto WebGL. */
  setSurface(mesh) {
    this.graph = null;
    this.surface = mesh;
    if (!this.lost) this.pipeline.setSurface(mesh);
    const [xMin,xMax,yMin,yMax] = mesh.bounds;
    const heights = mesh.triangles.flatMap(t => t.vertices.map(v => v[2]));
    const [minZ,maxZ] = heights.reduce(([min,max], z) => [Math.min(min,z),Math.max(max,z)], [0,0]);
    this.camera.target = [(xMin+xMax)/2, (yMin+yMax)/2, (minZ+maxZ)/2];
    this.camera.orthographicSpan = [xMax-xMin, Math.max(yMax-yMin,maxZ-minZ,2)];
    this.camera.yaw = 0.6;
    this.camera.pitch = 0.4;
    this.render();
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
    this.status.textContent = 'Visor listo. Ejes X rojo, Y verde y Z azul.';
  }

  dispose() {
    for (const [name, fn] of this.listeners) this.canvas.removeEventListener(name, fn);
    this.host.removeEventListener('resize', this.resize);
    this.pipeline.dispose();
  }
}
