export function replaceSearchParams(update: (params: URLSearchParams) => void) {
  const params = new URLSearchParams(window.location.search);
  update(params);
  const query = params.toString();
  window.history.replaceState(null, "", query ? `${window.location.pathname}?${query}` : window.location.pathname);
}
