/**
 * consume-rcpt.js (초간단 + Swal 로딩/경고)
 * - 파일 선택 → /api/receipt 업로드 → 응답(totalAmount, paidAt)로 폼 자동기입
 * - '인식 중'은 Swal 로딩 모달로 표시
 * - 아무 항목도 인식 못하면 1.5초 경고 Swal
 */
window.addEventListener('load', function () {
  const btn       = document.getElementById('rcpt-btn');
  const input     = document.getElementById('rcpt-input');
  const statusEl  = document.getElementById('rcpt-status');

  const amountEl  = document.getElementById('amount');     // 금액 input
  const dateEl    = document.getElementById('cons-date');  // datetime-local input

  // 선택 버튼 → 파일 선택창
  btn?.addEventListener('click', () => input?.click());

  // 파일 변경 → 업로드 바로 실행
  input?.addEventListener('change', () => {
    const file = input.files?.[0];
    if (!file) return;
    uploadAndFill(file);
  });

  function setStatus(msg, isOk = true) {
    if (!statusEl) return;
    statusEl.textContent = msg || '';
    statusEl.style.color = isOk ? '#2563eb' : '#b91c1c'; // 파랑/빨강
  }

  // Swal helpers
  function showLoading() {
    if (!window.Swal) return;
    Swal.fire({
      title: '영수증 인식 중…',
      html: '<small>잠시만 기다려주세요.</small>',
      allowOutsideClick: false,
      showConfirmButton: false,
      didOpen: () => Swal.showLoading()
    });
  }
  function closeLoading() {
    if (window.Swal && Swal.isVisible()) Swal.close();
  }
  function warnNoData() {
    if (!window.Swal) return;
    Swal.fire({
      icon: 'warning',
      title: '항목을 찾지 못했어요',
      text: '결제 금액/일시가 응답에 없어요. 수동으로 입력해 주세요.',
      showConfirmButton: true
    });
  }
  function showError(msg) {
    if (!window.Swal) return;
    Swal.fire({
      icon: 'error',
      title: '인식 실패',
      text: msg || '처리 중 오류가 발생했습니다.',
      timer: 1500,
      showConfirmButton: false
    });
  }

  // LocalDateTime or LocalDate → datetime-local(YYYY-MM-DDTHH:mm)
  function toDatetimeLocalFromPaidAt(paidAt) {
    if (!paidAt) return null;
    const s = String(paidAt).trim();
    if (s.includes('T')) return s.slice(0, 16);         // "YYYY-MM-DDTHH:mm[:ss]" → 앞 16
    if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return `${s}T00:00`;
    return null;
  }

  async function uploadAndFill(file) {
    // 버튼 비활성 + Swal 로딩 표시
    btn.disabled = true;
    showLoading();

    try {
      const fd = new FormData();
      fd.append('file', file); // 서버 요구 키 이름: file

      // Spring Security CSRF 메타가 있으면 자동첨부
      const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content;
      const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
      const headers = {};
      if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

      const res = await fetch('/api/receipt', {
        method: 'POST',
        body: fd,
        headers,
        credentials: 'same-origin'
      });
      if (!res.ok) {
        const text = await res.text().catch(()=> '');
        throw new Error(text || `HTTP ${res.status}`);
      }

      const data = await res.json();
      console.log('[OCR] /api/receipt response:', data);
      if (data?.rawText) console.log('[OCR] rawText:\n' + data.rawText);

      // 고정 키 사용
      const amt    = data?.totalAmount;   // number or string
      const paidAt = data?.paidAt;        // "YYYY-MM-DDTHH:mm[:ss]" or "YYYY-MM-DD"

      // 금액 채우기 (정수로 반올림)
      let filledAmt = false;
      if (amountEl && amt !== null && amt !== undefined && amt !== '') {
        const n = Number(amt);
        if (Number.isFinite(n)) {
          amountEl.value = String(Math.round(n));
          filledAmt = true;
        }
      }

      // 일시 채우기
      const dtLocal = toDatetimeLocalFromPaidAt(paidAt);
      const filledDt = !!(dateEl && dtLocal && (dateEl.value = dtLocal));

      // 로딩 종료
      closeLoading();

      // 아무 것도 인식 못했으면 경고
      if (!filledAmt && !filledDt) {
        warnNoData();
      }

      // 최종 상태 문구 표시(완료 only)
      const msgAmt = filledAmt ? `금액 ${Number(amt).toLocaleString()}원` : '';
      const msgDt  = filledDt ? `일시 ${dtLocal.replace('T',' ')}` : '';
      const join   = msgAmt && msgDt ? ' · ' : '';
      setStatus(`완료 ${msgAmt}${join}${msgDt}`.trim());

    } catch (e) {
      console.error(e);
      closeLoading();
      showError(e?.message);
    } finally {
      btn.disabled = false;
      // 같은 파일 재선택 허용
      if (input) input.value = '';
    }
  }
});
