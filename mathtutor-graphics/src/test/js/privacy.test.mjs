import test from 'node:test';
import assert from 'node:assert/strict';
import { cookieChoice, mountPrivacy } from '../../main/resources/static/visor/privacy.mjs';

test('UC-04 interpreta solo decisiones válidas y exige renovar tras otra versión', () => {
  assert.equal(cookieChoice('mathtutor_cookies=reject.v1', 'v1'), 'reject');
  assert.equal(cookieChoice('mathtutor_cookies=accept.v1', 'v1'), 'accept');
  assert.equal(cookieChoice('mathtutor_cookies=accept.v0', 'v1'), null);
  assert.equal(cookieChoice('mathtutor_cookies=maybe.v1', 'v1'), null);
});

test('UC-04 permite rechazar, aceptar y reabrir preferencias sin almacenamiento no esencial', () => {
  const nodes = new Map();
  for (const id of ['privacy-banner','privacy-accept','privacy-reject','privacy-manage']) {
    nodes.set(id,{hidden:false,listeners:{},addEventListener(type,fn){this.listeners[type]=fn;}});
  }
  const doc = {cookie:'',querySelector:selector=>nodes.get(selector.slice(1))};
  mountPrivacy(doc,{location:{protocol:'http:'}});
  assert.equal(nodes.get('privacy-banner').hidden,false);
  nodes.get('privacy-reject').listeners.click();
  assert.equal(nodes.get('privacy-banner').hidden,true);
  assert.match(doc.cookie,/mathtutor_cookies=reject.v1/);
  nodes.get('privacy-manage').listeners.click();
  assert.equal(nodes.get('privacy-banner').hidden,false);
  nodes.get('privacy-accept').listeners.click();
  assert.match(doc.cookie,/mathtutor_cookies=accept.v1/);
});
