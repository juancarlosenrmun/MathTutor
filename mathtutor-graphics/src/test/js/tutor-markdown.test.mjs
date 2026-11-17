import { test } from 'node:test';
import assert from 'node:assert/strict';
import { renderTutorMarkdown } from '../../main/resources/static/visor/tutor-markdown.mjs';

function documentFixture() {
  const createElement = tagName => ({
    tagName, children: [], attributes: {}, textContent: '',
    append(...nodes) { this.children.push(...nodes); },
    replaceChildren(...nodes) { this.children = nodes; },
    setAttribute(name, value) { this.attributes[name] = value; },
  });
  return { createElement };
}

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
