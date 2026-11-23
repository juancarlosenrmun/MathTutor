import { writeFile } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { resolve } from 'node:path';

const [executablePath, pageUrl, evidence] = process.argv.slice(2);
const deps = process.env.MATHTUTOR_BROWSER_DEPS;
const require = createRequire(deps ? resolve(deps, 'package.json') : import.meta.url);
const { chromium } = require('playwright');
const browser = await chromium.launch({ executablePath, args: ['--enable-gpu'] });
try {
  const page = await browser.newPage({ viewport: { width: 1280, height: 720 }, deviceScaleFactor: 1 });
  await page.goto(pageUrl);
  await page.waitForFunction(() => document.body.dataset.result, undefined, { timeout: 30000 });
  const benchmark = await page.locator('#results').textContent();
  const report = { browser: browser.version(), ...JSON.parse(benchmark) };
  await writeFile(resolve(evidence, 'performance.json'), JSON.stringify(report, null, 2) + '\n');
  console.log(JSON.stringify(report, null, 2));
  if (await page.locator('body').getAttribute('data-result') !== 'passed') {
    process.exitCode = 1;
  }
} finally {
  await browser.close();
}
