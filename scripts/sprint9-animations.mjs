import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { createServer } from 'node:http';
import { createRequire } from 'node:module';
import { resolve } from 'node:path';

const deps = process.env.MATHTUTOR_BROWSER_DEPS;
const require = createRequire(deps ? resolve(deps, 'package.json') : import.meta.url);
const { chromium } = require('playwright');
const { source: axe } = require('axe-core');
const root = new URL('../mathtutor-graphics/src/main/resources/static/', import.meta.url);
const types = { html: 'text/html', mjs: 'text/javascript', css: 'text/css' };
const server = createServer(async (request, response) => {
  const path = new URL(request.url, 'http://127.0.0.1').pathname;
  const file = new URL('.' + path, root);
  if (!file.href.startsWith(root.href)) return response.writeHead(404).end();
  try {
    const content = await readFile(file);
    response.writeHead(200, { 'Content-Type': types[path.split('.').at(-1)] ?? 'application/octet-stream' });
    response.end(content);
  } catch { response.writeHead(404).end(); }
});

async function laboratory(browser, base, reducedMotion) {
  const page = await browser.newPage({ viewport: { width: 390, height: 844 }, reducedMotion });
  await page.route('**/api/**', route => route.fulfill({ status: 401 }));
  await page.goto(base + '#/laboratorio');
  await page.getByText(/Visor listo/).waitFor();
  return page;
}

async function animate(browser, base) {
  const page = await laboratory(browser, base, 'no-preference');
  await page.clock.install();
  const clock = page.clock;
  const canvas = page.getByLabel(/Visor de funciones y coordenadas/);
  const secant = page.getByRole('button', { name: 'Animar secante a tangente' });
  const camera = page.getByRole('button', { name: 'Recorrer con la cámara' });
  assert.equal(await secant.isDisabled(), true);
  await page.getByRole('button', { name: 'Dibujar función, derivada y área' }).click();
  await page.locator('#curve-tangent-x').press('ArrowRight');
  assert.equal(await camera.isEnabled(), true);
  const tangent = await canvas.screenshot();
  await secant.press('Enter');
  await page.getByText(/observa cómo se acerca/).waitFor();
  await clock.runFor(800);
  assert.notDeepEqual(await canvas.screenshot(), tangent);
  await clock.runFor(2200);
  await page.getByText(/La secante ha alcanzado/).waitFor();
  assert.deepEqual(await canvas.screenshot(), tangent);
  await page.locator('summary').filter({ hasText: 'Superficies y planos tangentes' }).click();
  await page.getByRole('button', { name: 'Dibujar superficie' }).click();
  const surface = await canvas.screenshot();
  await camera.press('Enter');
  await clock.runFor(1000);
  assert.notDeepEqual(await canvas.screenshot(), surface);
  await page.getByRole('button', { name: 'Detener animación' }).press('Enter');
  await page.getByText('Animación detenida.').waitFor();
  assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
  await page.addScriptTag({ content: axe });
  const results = await page.evaluate(() => globalThis.axe.run({
    runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'] },
  }));
  assert.deepEqual(results.violations.map(({ id }) => id), []);
  await page.close();
}

async function reduceMotion(browser, base) {
  const page = await laboratory(browser, base, 'reduce');
  await page.getByRole('button', { name: 'Recorrer con la cámara' }).click();
  await page.getByText('Perspectiva actualizada sin animación.').waitFor();
  await page.getByRole('button', { name: 'Dibujar función, derivada y área' }).click();
  await page.locator('#curve-tangent-x').press('ArrowRight');
  await page.getByRole('button', { name: 'Animar secante a tangente' }).click();
  await page.getByText('Tangente mostrada sin animación.').waitFor();
  assert.equal(await page.getByRole('button', { name: 'Detener animación' }).isDisabled(), true);
  await page.close();
}

try {
  await new Promise(done => server.listen(0, '127.0.0.1', done));
  const base = `http://127.0.0.1:${server.address().port}/visor/index.html`;
  const browser = await chromium.launch({ channel: 'chrome',
    args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader'] });
  try {
    await animate(browser, base);
    await reduceMotion(browser, base);
    console.log(`Chrome ${browser.version()} · secante, recorrido de cámara, teclado, móvil y movimiento reducido`);
  } finally { await browser.close(); }
} finally { server.close(); }
