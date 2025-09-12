// 1) 전역 프리셋
const YHSwal = Swal.mixin({
  customClass: {
    container: 'yh-container',
    popup: 'yh-popup',
    title: 'yh-title',
    htmlContainer: 'yh-html',
    icon: 'no-circle-icon',
    confirmButton: 'my-confirm-button',
    cancelButton: 'yh-cancel'
  },
  buttonsStyling: false, // 버튼 기본 스타일 제거 (우리 스타일 적용)
  backdrop: true,        // overlay 활성화
  allowOutsideClick: false
});

// 2) Alert
window.showAlert = function({
  title = '알림',
  text = '',
  html = null,           // <== 옵션 추가: 리치 콘텐츠 쓸 때
  icon = 'info',
  imageUrl = null
} = {}) {
  return YHSwal.fire({
    title,
    ...(html != null ? { html } : { text }),
    icon: imageUrl ? undefined : icon,
    iconHtml: imageUrl
      ? `<img src="${imageUrl}" style="width:100px;height:100px;border-radius:16px;">`
      : undefined
  });
};

// 3) Confirm
window.showConfirm = function({
  title = '확인',
  text = '',
  html = null,           // <== 옵션 추가
  icon = 'warning',
  imageUrl = null,
  confirmButtonText = '확인',
  cancelButtonText = '취소'
} = {}) {
  return YHSwal.fire({
    title,
    ...(html != null ? { html } : { text }),
    icon: imageUrl ? undefined : icon,
    iconHtml: imageUrl
      ? `<img src="${imageUrl}" style="width:100px;height:100px;border-radius:16px;">`
      : undefined,
    showCancelButton: true,
    confirmButtonText,
    cancelButtonText
  });
};
