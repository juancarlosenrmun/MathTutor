const VERSION = 'v1';

/** @brief Lee exclusivamente la elección vigente de cookies de este sitio. */
export function cookieChoice(cookieHeader, version = VERSION) {
  const match = cookieHeader.split(';').map(part => part.trim())
    .find(part => part.startsWith('mathtutor_cookies='));
  const value = match?.slice('mathtutor_cookies='.length);
  return value === `accept.${version}` ? 'accept' : value === `reject.${version}` ? 'reject' : null;
}

/** @brief Presenta Aceptar/Rechazar al mismo nivel y permite cambiar la elección. */
export function mountPrivacy(document, host = globalThis) {
  const one = id => document.querySelector('#' + id);
  const banner = one('privacy-banner');
  banner.hidden = cookieChoice(document.cookie) !== null;
  const choose = value => {
    const secure = host.location.protocol === 'https:' ? '; Secure' : '';
    document.cookie = `mathtutor_cookies=${value}.${VERSION}; Path=/; Max-Age=15552000; SameSite=Lax${secure}`;
    banner.hidden = true;
  };
  one('privacy-accept').addEventListener('click', () => choose('accept'));
  one('privacy-reject').addEventListener('click', () => choose('reject'));
  one('privacy-manage').addEventListener('click', () => { banner.hidden = false; });
}
