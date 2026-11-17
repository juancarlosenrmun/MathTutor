// Construye nodos DOM sin interpretar HTML procedente del tutor.
function inline(document, parent, source) {
  const tokens = /(`[^`\n]+`|\*\*[^*\n]+\*\*|\*[^*\n]+\*|\[[^\]\n]+\]\([^\s)]+\))/g;
  let cursor = 0;
  for (const match of source.matchAll(tokens)) {
    if (match.index > cursor) {
      const span = document.createElement('span');
      span.textContent = source.slice(cursor, match.index);
      parent.append(span);
    }
    const token = match[0];
    let node;
    if (token.startsWith('`')) {
      node = document.createElement('code'); node.textContent = token.slice(1, -1);
    } else if (token.startsWith('**')) {
      node = document.createElement('strong'); node.textContent = token.slice(2, -2);
    } else if (token.startsWith('*')) {
      node = document.createElement('em'); node.textContent = token.slice(1, -1);
    } else {
      const [, label, href] = /^\[([^\]]+)\]\(([^)]+)\)$/.exec(token);
      if (/^(https?:\/\/|mailto:)/i.test(href)) {
        node = document.createElement('a');
        node.setAttribute('href', href);
        node.setAttribute('rel', 'noopener noreferrer');
        node.textContent = label;
      } else {
        node = document.createElement('span'); node.textContent = label;
      }
    }
    parent.append(node);
    cursor = match.index + token.length;
  }
  const tail = document.createElement('span');
  tail.textContent = source.slice(cursor);
  parent.append(tail);
}

/** Renderiza Markdown frecuente del tutor, también durante el streaming. */
export function renderTutorMarkdown(document, target, markdown) {
  const lines = markdown.replace(/\r\n?/g, '\n').split('\n');
  const nodes = [];
  let index = 0;
  while (index < lines.length) {
    const line = lines[index];
    if (!line.trim()) { index++; continue; }
    const fence = /^\s*```/.exec(line);
    if (fence) {
      const pre = document.createElement('pre'), code = document.createElement('code');
      index++;
      const body = [];
      while (index < lines.length && !/^\s*```/.test(lines[index])) body.push(lines[index++]);
      if (index < lines.length) index++;
      code.textContent = body.join('\n'); pre.append(code); nodes.push(pre);
      continue;
    }
    const heading = /^(#{1,6})\s+(.+)$/.exec(line);
    if (heading) {
      const node = document.createElement(`h${heading[1].length}`);
      inline(document, node, heading[2]); nodes.push(node); index++; continue;
    }
    const list = /^\s*(?:([-*+])|(\d+)\.)\s+(.+)$/.exec(line);
    if (list) {
      const ordered = Boolean(list[2]), node = document.createElement(ordered ? 'ol' : 'ul');
      while (index < lines.length) {
        const item = /^\s*(?:([-*+])|(\d+)\.)\s+(.+)$/.exec(lines[index]);
        if (!item || Boolean(item[2]) !== ordered) break;
        const li = document.createElement('li'); inline(document, li, item[3]); node.append(li); index++;
      }
      nodes.push(node); continue;
    }
    if (/^>\s?/.test(line)) {
      const node = document.createElement('blockquote'), body = [];
      while (index < lines.length && /^>\s?/.test(lines[index])) body.push(lines[index++].replace(/^>\s?/, ''));
      inline(document, node, body.join(' ')); nodes.push(node); continue;
    }
    const paragraph = [];
    while (index < lines.length && lines[index].trim() && !/^(#{1,6}\s|\s*```|\s*[-*+]\s|\s*\d+\.\s|>\s?)/.test(lines[index])) paragraph.push(lines[index++]);
    if (!paragraph.length) { paragraph.push(lines[index++]); }
    const node = document.createElement('p'); inline(document, node, paragraph.join('\n')); nodes.push(node);
  }
  target.replaceChildren(...nodes);
}
