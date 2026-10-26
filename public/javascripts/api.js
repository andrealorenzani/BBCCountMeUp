// Thin wrapper around fetch for the JSON API: failures surface as Errors carrying the server's message.
export async function api(url, { method = 'GET', body } = {}) {
  const response = await fetch(url, {
    method,
    headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(payload.error ?? `Request failed (${response.status})`);
  }
  return payload;
}

export function showFeedback(element, message, kind) {
  element.className = `mt-3 alert alert-${kind}`;
  element.textContent = message;
}

export function el(tag, attributes = {}, ...children) {
  const node = document.createElement(tag);
  Object.entries(attributes).forEach(([key, value]) => node.setAttribute(key, value));
  node.append(...children);
  return node;
}
