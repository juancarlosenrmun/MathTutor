// Solo se muestran validaciones conocidas; los errores de cuenta e internos siguen siendo genéricos.
const registrationMessages = new Set([
  'La contraseña debe tener al menos 12 caracteres',
  'La contraseña debe contener al menos una mayúscula',
  'La contraseña debe contener al menos una minúscula',
  'La contraseña debe contener al menos un dígito',
  'La contraseña debe contener al menos un carácter especial',
]);

/** Token solo en memoria: nunca se persiste en localStorage ni sessionStorage. */
export class SessionClient {
  constructor(fetcher = globalThis.fetch, locks = globalThis.navigator?.locks) {
    this.fetch = fetcher; this.locks = locks; this.token = null; this.query = 0; this.profile = null;
  }

  async post(action, body) {
    // La cookie es compartida por pestañas: serializar su rotación evita falsos reusos.
    const request = async () => {
      const csrf = await this.fetch('/api/csrf-token', { credentials: 'same-origin', signal: AbortSignal.timeout(4000) });
      if (!csrf.ok) throw new Error('No se pudo preparar la sesión. Vuelve a intentarlo.');
      const { csrfToken } = await csrf.json();
      return this.fetch(`/api/auth/${action}`, {
        method: 'POST', credentials: 'same-origin', signal: AbortSignal.timeout(4000),
        headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrfToken,
          ...(this.token ? { Authorization: `Bearer ${this.token}` } : {}) },
        body: JSON.stringify(body),
      });
    };
    return this.locks ? this.locks.request('mathtutor-auth-cookie', request) : request();
  }

  failure(response, fallback) {
    const message = response.status === 429 ? 'Demasiados intentos. Espera unos minutos y vuelve a intentarlo.'
      : response.status >= 500 ? 'No se pudo contactar con el servidor. Vuelve a intentarlo.' : fallback;
    const error = new Error(message);
    error.status = response.status;
    return error;
  }

  async authenticate(action, body) {
    this.clear();
    const generation = this.query;
    const response = await this.post(action, body);
    if (!response.ok) {
      const failure = this.failure(response, action === 'register'
        ? 'No se pudo completar el registro. Revisa los datos o inicia sesión si ya tienes cuenta.'
        : 'Credenciales inválidas o acceso temporalmente limitado. Revisa el correo y la contraseña.');
      if (action === 'register' && response.status === 400) {
        try {
          const reason = (await response.json())?.error;
          if (registrationMessages.has(reason)) failure.message = reason;
        } catch { /* Una respuesta ilegible conserva el mensaje seguro. */ }
      }
      throw failure;
    }
    const profile = (await response.json()).data;
    if (generation === this.query) { this.token = profile.accessToken; this.profile = profile; }
  }

  login(email, password) { return this.authenticate('login', { email, password }); }
  register(email, password) { return this.authenticate('register', { email, password, role: 'ALUMNO' }); }

  async restore() {
    const generation = this.query;
    const response = await this.post('refresh');
    if (generation !== this.query) return;
    if (response.status === 401) { this.clear(); return null; }
    if (!response.ok) throw this.failure(response, 'No se pudo renovar la sesión. Vuelve a iniciar sesión.');
    const profile = (await response.json()).data;
    if (generation === this.query) { this.token = profile.accessToken; this.profile = profile; }
  }

  async logout() {
    this.query++;
    try {
      const response = await this.post('logout');
      if (!response.ok) throw this.failure(response, 'No se pudo cerrar la sesión en el servidor. Reintenta el cierre.');
    } finally { this.clear(); }
  }

  async role(retry = true) {
    if (!this.token) return null;
    const token = this.token;
    const query = ++this.query;
    const response = await this.fetch('/api/auth/me', {
      headers: { Authorization: `Bearer ${token}` }, signal: AbortSignal.timeout(2000),
    });
    // Una respuesta atrasada nunca puede revocar el token de un login posterior.
    if (token !== this.token || query !== this.query) return undefined;
    if (!response.ok) {
      if (response.status === 401 && retry) { await this.restore(); return this.role(false); }
      if (response.status >= 500) throw this.failure(response);
      this.clear();
      if (response.status === 403) throw this.failure(response, 'No tienes permiso para acceder a este recurso.');
      return null;
    }
    const profile = (await response.json()).data;
    if (token !== this.token || query !== this.query) return undefined;
    this.profile = profile;
    return profile.role;
  }

  clear() { this.token = null; this.profile = null; this.query++; }
}
