import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { createServer } from 'node:http';
import { createRequire } from 'node:module';
import { resolve } from 'node:path';

const dependencies = process.env.MATHTUTOR_BROWSER_DEPS;
const require = createRequire(dependencies ? resolve(dependencies, 'package.json') : import.meta.url);
const { chromium, firefox, webkit } = require('playwright');
const { source: axe } = require('axe-core');
const root = new URL('../mathtutor-graphics/src/main/resources/static/', import.meta.url);
const types = { html: 'text/html', mjs: 'text/javascript', css: 'text/css' };
const server = createServer(async (request, response) => {
  const path = new URL(request.url, 'http://localhost').pathname;
  const file = new URL('.' + path, root);
  if (!file.href.startsWith(root.href)) return response.writeHead(404).end();
  try {
    const content = await readFile(file);
    response.writeHead(200, { 'Content-Type': types[path.split('.').at(-1)] || 'application/octet-stream' });
    response.end(content);
  } catch { response.writeHead(404).end(); }
});

const answer = 'data: {"type":"TOKEN","content":"Piensa en la pendiente."}\n\ndata: {"type":"DONE"}\n\n';

async function fixture(page, role = 'ALUMNO') {
  const requests = [];
  const profile = { id: 1, accessToken: 'sintetico', role, email: 'cuenta@example.test' };
  await page.route('**/api/**', route => {
    const path = new URL(route.request().url()).pathname;
    requests.push([path, route.request().method(), route.request().postDataJSON()]);
    if (path.endsWith('/messages')) return route.fulfill({ contentType: 'text/event-stream', body: answer });
    if (path === '/api/privacy/account' || path.endsWith('/role')) return route.fulfill({ status: 204 });
    const data = {
      '/api/csrf-token': { csrfToken: 'sintetico' },
      '/api/auth/me': { data: profile },
      '/api/auth/refresh': { data: profile },
      '/api/privacy/export': { profile },
      '/api/admin/users': [{ id: 2, role: 'ALUMNO', active: true, email: 'alumno@example.test' }],
      '/api/teacher/statistics': [{ module: 'funciones', topic: null, started: 3, active: 1, completed: 1,
        abandoned: 1, abandonedEarly: 0, abandonedNearSolution: 1 }],
      '/api/tutor/modules': [{ code: 'funciones', name: 'Funciones' }],
      '/api/tutor/sessions': { id: 4, moduleCode: 'funciones', status: 'ACTIVA' },
    }[path] ?? [];
    return route.fulfill({ json: data });
  });
  return requests;
}

async function audit(page, view) {
  await page.addScriptTag({ content: axe });
  const results = await page.evaluate(() => globalThis.axe.run({
    runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'] },
  }));
  assert.deepEqual(results.violations.map(issue => [issue.id, issue.nodes.map(node => node.target)]), [], view);
}

async function disableWebGl(page) {
  await page.addInitScript(() => {
    const original = HTMLCanvasElement.prototype.getContext;
    HTMLCanvasElement.prototype.getContext = function (kind, ...args) {
      return kind === 'webgl2' ? null : original.call(this, kind, ...args);
    };
  });
}

async function verifyAccessibleViewer(browser, base, name) {
  const context = await browser.newContext({ viewport: { width: 390, height: 844 } });
  const page = await context.newPage();
  await fixture(page);
  await page.goto(base + '#/laboratorio');
  if (name === 'WebKit') await page.locator('.skip-link').focus();
  else await page.keyboard.press('Tab');
  assert.equal(await page.evaluate(() => document.activeElement.className), 'skip-link');
  await page.keyboard.press('Enter');
  assert.equal(await page.evaluate(() => document.activeElement.id), 'main');
  await page.getByRole('button', { name: 'Dibujar función, derivada y área' }).press('Enter');
  assert.match(await page.locator('#curve-result').textContent(), /Integral aproximada/);
  await page.getByRole('button', { name: 'Dibujar superficie' }).press('Enter');
  assert.match(await page.locator('#surface-result').textContent(), /Valores de z muestreados/);
  const webgl = await page.evaluate(() => Boolean(document.querySelector('canvas').getContext('webgl2')));
  if (webgl) {
    await page.locator('canvas').press('Enter');
    assert.match(await page.locator('#graphic-status').textContent(), /seleccionado|intersección/);
  }
  assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
  await audit(page, 'laboratorio a 390 px');
  await page.goto(base + '#/tutor');
  await page.getByRole('button', { name: 'Funciones', exact: true }).press('Enter');
  await page.getByRole('textbox', { name: 'Tu pregunta' }).fill('¿Cómo se interpreta la pendiente?');
  await page.getByRole('button', { name: 'Enviar pregunta' }).press('Enter');
  const log = page.getByRole('log', { name: 'Conversación' });
  await log.locator('[class="tutor-answer-body"]', { hasText: 'Piensa en la pendiente.' }).waitFor();
  await page.waitForFunction(() => document.querySelector('[role="log"]').getAttribute('aria-busy') === 'false');
  assert.match(await log.ariaSnapshot(), /Piensa en la pendiente/);
  await audit(page, 'chat');
  await context.close();
  const fallback = await browser.newPage();
  await disableWebGl(fallback);
  await fallback.goto(base + '#/laboratorio');
  await fallback.getByRole('button', { name: 'Dibujar superficie' }).press('Enter');
  assert.match(await fallback.locator('#surface-result').textContent(), /Valores de z muestreados/);
  await fallback.close();
  console.log(`${name} ${browser.version()} · WebGL 2 ${webgl ? 'activo' : 'no disponible'} · teclado, vista móvil, chat y alternativa textual verificados`);
}

async function verifyAccount(browser, base, name) {
  const admin = await browser.newPage({ viewport: { width: 390, height: 844 } });
  const requests = await fixture(admin, 'ADMIN');
  await admin.goto(base + '#/espacio');
  await admin.getByRole('button', { name: 'Cargar perfiles' }).click();
  await admin.locator('#admin-user option[value="2"]').waitFor({ state: 'attached' });
  assert.equal(await admin.locator('#admin-user').inputValue(), '2');
  await admin.getByRole('button', { name: 'Consultar estadísticas' }).click();
  await admin.getByRole('cell', { name: 'Sin tema' }).waitFor();
  assert.equal(await admin.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
  await audit(admin, 'administración y estadísticas');
  await admin.locator('#admin-role').selectOption('PROFESOR');
  admin.once('dialog', dialog => dialog.accept());
  await admin.getByRole('button', { name: 'Cambiar rol' }).click();
  await admin.getByText('Se ha actualizado el perfil.').waitFor();
  assert.deepEqual(requests.find(([, method]) => method === 'PUT'),
    ['/api/admin/users/2/role', 'PUT', { role: 'PROFESOR' }]);
  await admin.goto(base + '#/cuenta');
  const download = admin.waitForEvent('download');
  await admin.getByRole('button', { name: 'Descargar mis datos (JSON)' }).click();
  assert.equal((await download).suggestedFilename(), 'mathtutor-datos.json');
  await admin.getByText('Suprimir mi cuenta', { exact: true }).click();
  await admin.getByRole('button', { name: 'Confirmar supresión' }).click();
  await admin.getByText('Tu cuenta y tus datos se han suprimido.').waitFor();
  await admin.close();
  console.log(`${name} · administración, estadísticas y derechos sobre la cuenta verificados`);
}

const softwareWebGl = ['--use-angle=swiftshader', '--enable-unsafe-swiftshader'];
const matrix = [
  ['Chrome', chromium, { channel: 'chrome', args: softwareWebGl }],
  ['Edge', chromium, { channel: 'msedge', args: softwareWebGl }],
  ['Firefox', firefox, {}],
  ['WebKit', webkit, {}],
];

try {
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const base = `http://127.0.0.1:${server.address().port}/visor/index.html`;
  const selected = matrix.filter(([name]) => !process.argv[2] || name === process.argv[2]);
  assert.ok(selected.length, 'Navegador desconocido');
  const results = await Promise.allSettled(selected.map(async ([name, engine, options]) => {
    for (const verify of [verifyAccessibleViewer, verifyAccount]) {
      const browser = await engine.launch(options);
      try { await verify(browser, base, name); }
      finally { await browser.close(); }
    }
  }));
  for (const [index, result] of results.entries()) {
    if (result.status === 'rejected') {
      console.error(selected[index][0], result.reason);
      process.exitCode = 1;
    }
  }
} finally { server.close(); }
