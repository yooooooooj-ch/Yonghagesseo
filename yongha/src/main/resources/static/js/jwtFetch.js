// /js/jwtFetch.js

export async function jwtFetch(url, options = {}) {
  const token = localStorage.getItem('access_token');
  const headers = { ...(options.headers || {}) };
  if (token) headers.Authorization = 'Bearer ' + token;

  // same-origin이면 쿠키가 기본 전송되지만, 명시적으로 두면 안전
  const base = { credentials: 'same-origin' };

  let res = await fetch(url, { ...base, ...options, headers });

  if (res.status === 401) {
    // 1) 코드 파싱 (없어도 동작하도록)
    let code = '';
    try { code = (await res.clone().json()).code; } catch {}

    // 2) 리프레시를 시도해야 하는 경우?
    const shouldTryRefresh =
      code === 'TOKEN_EXPIRED' ||
      // 서버가 code를 안 주는 경우에도 access_token이 있었다면 "만료"로 간주하여 1회 시도
      (!code && !!token);

    if (shouldTryRefresh) {
      const r = await fetch('/api/user/refresh', {
        method: 'POST',
        credentials: 'same-origin'
      });

      if (r.ok) {
        const newAccess = await r.text();
        if (newAccess) {
          localStorage.setItem('access_token', newAccess);
          const retryHeaders = { ...headers, Authorization: 'Bearer ' + newAccess };
          return await fetch(url, { ...base, ...options, headers: retryHeaders });
        }
      }

      // 3) 리프레시 실패 → 서버 세션도 정리 (중요!)
      try {
        await fetch('/api/user/logout', { method: 'POST', credentials: 'same-origin' });
      } catch {}
    }

    // 4) 여기까지 왔으면 무조건 클라 정리 후 이동
    localStorage.removeItem('access_token');
    window.location.href = '/';
    throw new Error('Unauthorized');
  }

  return res;
}

// ---------------------
// 전역 등록 + 자동 preflight
// ---------------------
window.jwtFetch = jwtFetch;

(async () => {
  const token = localStorage.getItem('access_token');
  if (token) {
    try { await jwtFetch('/api/user/me'); } catch {}
  }
})();
