/** Token solo en memoria: nunca se persiste en localStorage ni sessionStorage. */
export class SessionClient {
  constructor(fetcher = globalThis.fetch) { this.fetch = fetcher; this.token = null; this.query = 0; }

  async login(email, password) {
    this.clear();
    const generation = this.query;
    const csrf = await this.fetch('/api/csrf-token', { credentials: 'same-origin', signal: AbortSignal.timeout(4000) });
    const { csrfToken } = await csrf.json();
    const response = await this.fetch('/api/auth/login', {
      method: 'POST', credentials: 'same-origin', signal: AbortSignal.timeout(4000),
      headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrfToken },
      body: JSON.stringify({ email, password }),
    });
    if (!response.ok) throw new Error('Credenciales inválidas o acceso temporalmente limitado.');
    const token = (await response.json()).data.accessToken;
    if (generation === this.query) this.token = token;
  }

  async role() {
    if (!this.token) return null;
    const token = this.token;
    const query = ++this.query;
    const response = await this.fetch('/api/auth/me', {
      headers: { Authorization: `Bearer ${token}` }, signal: AbortSignal.timeout(2000),
    });
    // Una respuesta atrasada nunca puede revocar el token de un login posterior.
    if (token !== this.token || query !== this.query) return undefined;
    if (!response.ok) { this.clear(); return null; }
    const role = (await response.json()).data.role;
    return token === this.token && query === this.query ? role : undefined;
  }

  clear() { this.token = null; this.query++; }
}
