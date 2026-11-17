import { spawn } from 'node:child_process';
import { mkdtemp, readFile, realpath, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { basename, dirname, join } from 'node:path';
import { setTimeout as delay } from 'node:timers/promises';

// CDP nativo: los locks entre pestañas necesitan reloj real, no tiempo virtual.
const [browser, base, evidence] = process.argv.slice(2);
const profile = await mkdtemp(join(tmpdir(), 'mathtutor-frontend-'));
const processHandle = spawn(browser, ['--headless', '--no-sandbox', '--no-first-run',
  '--no-default-browser-check', '--use-angle=swiftshader', '--enable-unsafe-swiftshader',
  '--disable-background-networking', '--remote-debugging-port=0', `--user-data-dir=${profile}`],
{ windowsHide: true, stdio: 'ignore' });
let socket;
try {
  let port;
  for (let attempt = 0; attempt < 200 && !port; attempt++) {
    try { port = (await readFile(join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]; }
    catch { await delay(50); }
  }
  if (!port) throw new Error('Chromium no publica su puerto de depuración');
  const target = await (await fetch(`http://127.0.0.1:${port}/json/new?about:blank`, { method: 'PUT' })).json();
  socket = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
  let sequence = 0;
  const pending = new Map();
  socket.onmessage = event => {
    const message = JSON.parse(event.data);
    const waiter = pending.get(message.id);
    if (!waiter) return;
    pending.delete(message.id); clearTimeout(waiter.timer);
    if (message.error) waiter.reject(new Error(message.error.message));
    else waiter.resolve(message.result);
  };
  const command = (method, params = {}) => new Promise((resolve, reject) => {
    const id = ++sequence;
    const timer = setTimeout(() => { pending.delete(id); reject(new Error(`CDP sin respuesta: ${method}`)); }, 15000);
    pending.set(id, { resolve, reject, timer }); socket.send(JSON.stringify({ id, method, params }));
  });
  const evaluate = async expression => {
    const value = await command('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
    if (value.exceptionDetails) throw new Error(value.exceptionDetails.text);
    return value.result.value;
  };
  await command('Page.enable');
  await command('Page.navigate', { url: `${base}/frontend.html` });
  let result;
  for (let attempt = 0; attempt < 300; attempt++) {
    result = await evaluate('document.body?.dataset.result');
    if (result) break;
    await delay(50);
    const metrics = await evaluate(`(() => {
      const doc = document.querySelector('iframe').contentDocument;
      return { viewport: doc.documentElement.clientWidth, content: doc.documentElement.scrollWidth };
    })()`);
    if (metrics.content > metrics.viewport + 1) throw new Error(`Desbordamiento en captura ${name}: ${JSON.stringify(metrics)}`);
  }
  await writeFile(join(evidence, 'frontend.html'), await evaluate('document.documentElement.outerHTML'));
  if (result !== 'passed') throw new Error(await evaluate('document.querySelector("#result")?.textContent') || 'Contrato frontend sin finalizar');
  console.log('Frontend real: rutas, móvil, sesión, permisos y cámara correctos');
  // Capturas autenticadas con el backend simulado del contrato, sin crear usuarios reales.
  await evaluate(`(async () => {
    const frame = document.querySelector('iframe'), win = frame.contentWindow;
    document.body.style.margin = '0'; document.querySelector('#result').hidden = true;
    frame.style.cssText = 'display:block;width:100%;height:100vh;border:0';
    win.testState.status = 200;
    await win.testApp.client.login('alumno@ejemplo.es', 'UnaClaveSegura12!');
    await win.testApp.client.role(); win.location.hash = '#/espacio'; win.testApp.render();
    win.document.querySelector('#notice').hidden = true;
  })()`);
  for (const [role, name, width, height, view = 'espacio'] of [['ALUMNO', 'espacio', 1440, 1000],
    ['PROFESOR', 'espacio-profesor', 1440, 1000], ['ADMIN', 'espacio-admin', 1440, 1000],
    ['ALUMNO', 'espacio-movil', 390, 1000],
    ['ALUMNO', 'tutor', 1440, 1100, 'tutor'], ['ALUMNO', 'tutor-movil', 390, 1200, 'tutor']]) {
    await command('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: width < 700 });
    await evaluate(`(async () => {
      const win = document.querySelector('iframe').contentWindow;
      win.testState.role = '${role}'; await win.testApp.updateRole();
      win.location.hash = '#/${view}'; win.testApp.render();
      win.document.querySelector('#main').blur();
    })()`);
    await delay(50);
    const shot = await command('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false });
    await writeFile(join(evidence, `${name}.png`), Buffer.from(shot.data, 'base64'));
  }
  for (const [name, view, width, height] of [['inicio', 'inicio', 1440, 1000],
    ['laboratorio', 'laboratorio', 1440, 1000], ['acceso', 'acceso', 1440, 1000],
    ['registro', 'registro', 1440, 1000], ['inicio-movil', 'inicio', 390, 1000],
    ['laboratorio-movil', 'laboratorio', 390, 1100]]) {
    await command('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: width < 700 });
    await command('Page.navigate', { url: `${base}/visor/index.html#/${view}` });
    // Esperar al montaje real antes de capturar, también en dispositivos móviles.
    for (let attempt = 0; attempt < 100; attempt++) {
      if (await evaluate(`document.querySelector('[data-view="${view}"]')?.hidden === false && document.querySelector('#graphic-status')?.textContent.includes('Visor listo.')`)) break;
      await delay(30);
    }
    const shot = await command('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false });
    await writeFile(join(evidence, `${name}.png`), Buffer.from(shot.data, 'base64'));
  }
  await command('Browser.close');
} finally {
  socket?.close();
  processHandle.kill();
  await delay(200);
  const resolvedProfile = await realpath(profile), resolvedTemp = await realpath(tmpdir());
  if (dirname(resolvedProfile).toLowerCase() !== resolvedTemp.toLowerCase()
      || !basename(resolvedProfile).startsWith('mathtutor-frontend-')) throw new Error('Directorio temporal fuera del alcance esperado');
  await rm(resolvedProfile, { recursive: true, force: true, maxRetries: 10, retryDelay: 100 });
}
