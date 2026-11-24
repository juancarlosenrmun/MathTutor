import { test } from 'node:test';
import assert from 'node:assert/strict';
import { renderTutorMarkdown } from '../../main/resources/static/visor/tutor-markdown.mjs';
import { appendTurn, renderTurns } from '../../main/resources/static/visor/tutor-history.mjs';

function documentFixture() {
  const createElement = tagName => ({
    tagName, children: [], attributes: {}, style: {}, className: '', value: '',
    get textContent() { return this.value + this.children.map(child => child.textContent).join(''); },
    set textContent(value) { this.value = value; this.children = []; },
    append(...nodes) { this.children.push(...nodes); },
    appendChild(node) { this.children.push(node); return node; },
    replaceChildren(...nodes) { this.children = nodes; },
    setAttribute(name, value) { this.attributes[name] = value; },
  });
  return { createElement, createElementNS: (_, name) => createElement(name),
    createTextNode: text => ({ tagName: '#text', textContent: text }) };
}

function descendants(node) { return [node, ...(node.children || []).flatMap(descendants)]; }

function mathFixture(t) {
  const previous = globalThis.document, doc = documentFixture();
  globalThis.document = doc;
  t.after(() => { globalThis.document = previous; });
  return { doc, target: doc.createElement('div') };
}

test('continuidad: renderiza variables y límite en línea y en bloque sin delimitadores visibles', t => {
  const { doc, target } = mathFixture(t);
  renderTutorMarkdown(doc, target, String.raw`Una función $f$ es continua en $a$ si $f(a)$ existe y
$$\lim_{x\to a}f(x) = f(a)$$`);
  const nodes = descendants(target);
  assert.equal(nodes.filter(node => node.tagName === 'math').length, 4);
  assert.equal(nodes.some(node => node.tagName === 'munder'), true);
  assert.equal(nodes.some(node => node.tagName === 'math' && node.attributes.display === 'block'), true);
  // KaTeX conserva la fuente como anotación accesible; los delimitadores no forman parte de ella.
  assert.equal(nodes.some(node => node.tagName === '#text' && node.textContent.includes('$')), false);
});

test('el producto vectorial sin delimitadores se representa en el envío y en el historial', t => {
  const {doc, target} = mathFixture(t);
  const message = 'Calcula e_1 \\times e_2';
  appendTurn(doc, target, message, 'Identifica primero los ejes de e_1 y e_2.');
  const check = () => {
    const expression = descendants(target.children[0]).find(node => node.tagName === 'math');
    assert.ok(expression);
    assert.equal(descendants(expression).filter(node => node.tagName === 'msub').length, 2);
    assert.ok(descendants(expression).some(node => node.tagName === 'mo' && node.textContent === '×'));
    assert.equal(descendants(target.children[1]).filter(node => node.tagName === 'math').length, 2);
  };
  check();
  renderTurns(doc, target, [{message, response: 'Identifica primero los ejes de e_1 y e_2.'}]);
  check();
});

test('admite delimitadores LaTeX alternativos, fracciones, raíces y bloques con saltos de línea', t => {
  const { doc, target } = mathFixture(t);
  renderTutorMarkdown(doc, target, String.raw`Calcula \(\frac{1}{2}\) y $\sqrt{x^2+1}$.

\[
\int_0^1 x^2\,dx = \frac{1}{3}
\]`);
  const nodes = descendants(target);
  assert.equal(nodes.filter(node => node.tagName === 'math').length, 3);
  assert.equal(nodes.filter(node => node.tagName === 'mfrac').length, 2);
  assert.equal(nodes.some(node => node.tagName === 'msqrt'), true);
});

test('streaming conserva fórmulas abiertas y renderiza al completar el delimitador', t => {
  const { doc, target } = mathFixture(t);
  for (const partial of ['$f', '$$\\lim_{x', '\\(x', '\\[x']) {
    assert.doesNotThrow(() => renderTutorMarkdown(doc, target, partial));
    assert.equal(descendants(target).some(node => node.tagName === 'math'), false);
    assert.equal(target.textContent, partial);
  }
  renderTutorMarkdown(doc, target, '$f(a)$');
  assert.equal(descendants(target).filter(node => node.tagName === 'math').length, 1);
});

test('código y dólares escapados permanecen literales; énfasis y listas conservan fórmulas', t => {
  const { doc, target } = mathFixture(t);
  renderTutorMarkdown(doc, target, String.raw`Precio: \$20. Código: \`$f$\`.

- Calcula $x^2$ y **comprueba** el resultado.

\`\`\`
$$x$$
\`\`\``.replaceAll('\\`', '`'));
  const nodes = descendants(target);
  assert.equal(nodes.filter(node => node.tagName === 'math').length, 1);
  assert.equal(nodes.some(node => node.tagName === 'code' && node.textContent === '$f$'), true);
  assert.equal(nodes.some(node => node.tagName === 'code' && node.textContent === '$$x$$'), true);
});

test('LaTeX inválido, comandos peligrosos y expansión excesiva no bloquean ni inyectan HTML', t => {
  const { doc, target } = mathFixture(t);
  for (const formula of [String.raw`\frac{`, String.raw`\href{javascript:alert(1)}{pulsa}`,
    String.raw`\includegraphics{https://example.org/track}`, String.raw`\def\x{\x}\x`, 'x'.repeat(4097)]) {
    assert.doesNotThrow(() => renderTutorMarkdown(doc, target, `$${formula}$`));
    const nodes = descendants(target);
    assert.equal(nodes.some(node => ['script', 'img', 'a'].includes(node.tagName)), false);
  }
  renderTutorMarkdown(doc, target, '$\\gdef\\foo{1}$ $\\foo$');
  assert.equal(descendants(target).some(node => node.className?.includes('math-error')), true);
});

test('conserva bloques matemáticos con líneas vacías y fórmulas dentro de énfasis', t => {
  const { doc, target } = mathFixture(t);
  renderTutorMarkdown(doc, target, '**Valor $f(a)$** y *punto $a$*.\n$$\nx^2\n\n+1\n$$\n\nFin.');
  const nodes = descendants(target);
  assert.equal(nodes.filter(node => node.tagName === 'math').length, 3);
  assert.equal(target.children.at(-1).textContent, 'Fin.');
});

test('renderiza encabezados, énfasis, listas y código de la respuesta', () => {
  const doc = documentFixture(), target = doc.createElement('div');
  renderTutorMarkdown(doc, target, '## Derivadas\n\nLa **derivada** de *x²* es `2x`.\n\n- Primera regla\n- Segunda regla\n\n```js\nf(x)\n```');
  assert.deepEqual(target.children.map(node => node.tagName), ['h2', 'p', 'ul', 'pre']);
  assert.equal(target.children[0].children[0].textContent, 'Derivadas');
  assert.deepEqual(target.children[1].children.map(node => node.tagName), ['span', 'strong', 'span', 'em', 'span', 'code', 'span']);
  assert.equal(target.children[2].children.length, 2);
  assert.equal(target.children[3].children[0].textContent, 'f(x)');
});

test('escapa HTML remoto y rechaza enlaces peligrosos sin perder texto', () => {
  const doc = documentFixture(), target = doc.createElement('div');
  renderTutorMarkdown(doc, target, '<img src=x onerror=alert(1)> [seguro](https://example.org) [peligro](javascript:alert(1))');
  assert.equal(target.children[0].children[0].textContent, '<img src=x onerror=alert(1)> ');
  const links = target.children[0].children.filter(node => node.tagName === 'a');
  assert.equal(links.length, 1);
  assert.equal(links[0].attributes.href, 'https://example.org');
  assert.equal(target.children[0].children.some(node => node.textContent.includes('peligro')), true);
});
