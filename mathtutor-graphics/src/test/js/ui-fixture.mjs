export function element(extra = {}) {
  return { textContent: '', hidden: false, disabled: false, value: '', type: 'password', attributes: {}, listeners: {},
    addEventListener(name, fn) { this.listeners[name] = fn; }, removeEventListener() {},
    setAttribute(k, v) { this.attributes[k] = v; }, removeAttribute(k) { delete this.attributes[k]; },
    toggleAttribute(k, value) { if (value) this.attributes[k] = ''; else delete this.attributes[k]; },
    focus() { this.focused = true; }, scrollIntoView() {},
    append(...nodes) { (this.children ||= []).push(...nodes); },
    replaceChildren(...nodes) { this.children = nodes; }, ...extra };
}
export function fixture() {
  const nodes = Object.fromEntries(['notice', 'retry-logout', 'role', 'account-email', 'permission-description', 'main', 'logout', 'graphic-status'].map(id => ['#' + id, element()]));
  nodes['.skip-link'] = element();
  for (const id of ['workspace-role', 'workspace-title', 'workspace-description']) nodes['#' + id] = element();
  const views = ['inicio','acceso','registro','espacio','laboratorio','cuenta','restringido'].map(view => element({ dataset: { view } }));
  const navs = ['inicio', 'laboratorio'].map(nav => element({ dataset: { nav } }));
  const forms = ['login', 'register'].map(auth => {
    const error = element(), button = element({ textContent: auth });
    return element({ dataset: { auth }, elements: { email: element({ value: 'a@test.es' }), password: element({ value: 'UnaClaveSegura12!' }), ...(auth === 'register' ? { confirm: element({ value: 'UnaClaveSegura12!' }) } : {}) },
      querySelector: selector => selector === '[data-error]' ? error : button, error, button });
  });
  const passwords = forms.map((form, i) => { nodes['#password' + i] = form.elements.password; return element({ dataset: { password: 'password' + i } }); });
  const guests = [element()], members = [element()];
  const doc = { querySelector: key => nodes[key], getElementById: id => nodes['#' + id],
    querySelectorAll(selector) {
      return { '[data-view]': views, '[data-nav]': navs, '[data-guest]': guests, '[data-member]': members,
        '[data-auth]': forms, '[data-password]': passwords, '[data-error]': forms.map(f => f.error),
        'input[type="password"], [data-visible-password]': forms.flatMap(f => [f.elements.password, ...(f.elements.confirm ? [f.elements.confirm] : [])]),
        '[data-auth] button[type="submit"], [data-logout]': [...forms.map(f => f.button), nodes['#logout'], nodes['#retry-logout']],
        '[data-logout]': [nodes['#logout'], nodes['#retry-logout']], '[data-camera]': [] }[selector] || [];
    } };
  const state = { role: 'ALUMNO', status: 200, online: true, restore: false, calls: [] };
  const host = element({ location: { hash: '#/inicio' }, scrollTo() {}, setInterval(fn, ms) { this.poll = fn; this.delay = ms; return 7; }, clearInterval(id) { this.cleared = id; },
    async fetch(path, options) {
      state.calls.push([path, options]);
      if (!state.online) throw new Error('Sin conexión');
      const status = path.endsWith('csrf-token') ? 200 : path.endsWith('/refresh') && !state.restore ? 401 : state.status;
      return { ok: status < 400, status, json: async () => path.endsWith('csrf-token') ? { csrfToken: 'csrf' }
        : { data: { accessToken: 'jwt', role: state.role, email: 'a@test.es' } } };
    } });
  const submit = async (form, password = 'UnaClaveSegura12!') => {
    form.elements.password.value = password;
    if (form.elements.confirm) form.elements.confirm.value = password;
    await form.listeners.submit({ preventDefault() {}, currentTarget: form });
  };
  return { doc, nodes, forms, passwords, guests, members, views, navs, host, state, submit };
}
