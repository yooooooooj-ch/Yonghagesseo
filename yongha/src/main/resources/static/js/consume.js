// 날짜 기본값: 페이지 로드 시 현재 시각으로 채워두되, 사용자가 비우면 서버에서 SYSDATE 처리
(function presetNow() {
	const el = document.getElementById('cons-date');
	if (!el) return;
	const pad = n => String(n).padStart(2, '0');
	const d = new Date();
	// YYYY-MM-DDThh:mm
	const val = d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
		+ 'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
	el.value = val;
})();

document.addEventListener('DOMContentLoaded', () => {
	// 금액 버튼 누적 로직
	document.querySelectorAll('.amount-buttons button').forEach(btn => {
		btn.addEventListener('click', () => {
			const amt = parseInt(btn.getAttribute('data-amt'), 10);
			const input = document.getElementById('amount');
			if (!input || !Number.isFinite(amt)) return;
			let current = parseInt(input.value || '0', 10);
			if (isNaN(current)) current = 0;
			input.value = current + amt;
			input.dispatchEvent(new Event('change'));
		});
	});

	// 금액 초기화 버튼
	document.getElementById('amount-init')?.addEventListener('click', () => {
		const input = document.getElementById('amount');
		if (!input) return;
		input.value = '';
		input.focus();
	});

	// ===== 소비 등록/수정 폼 유효성 (SweetAlert 사용) =====
	const form = document.getElementById('consume-form');
	if (form) {
		// 다이얼로그 안에서 쓰면 SweetAlert가 뒤로 깔리지 않도록 target 자동 지정
		const swalTarget = (() => {
			const dlg = form.closest('dialog');
			return dlg ? { target: dlg } : {};
		})();

		form.addEventListener('submit', async (e) => {
			const amount = document.getElementById('amount');
			const type = document.getElementById('cons-type');

			// 금액 검증
			const v = Number(amount?.value);
			if (!Number.isFinite(v) || v < 0) {
				e.preventDefault();
				await showAlert({
					title: '입력값 확인',
					text: '금액을 0 이상 숫자로 입력하세요.',
					icon: 'warning',
					...swalTarget
				});
				amount?.focus();
				return;
			}

			// 타입 검증
			if (!type?.value) {
				e.preventDefault();
				await showAlert({
					title: '입력값 확인',
					text: '소비 타입을 선택하세요.',
					icon: 'warning',
					...swalTarget
				});
				type?.focus();
				return;
			}

			// 날짜가 비어있다면 서버에서 SYSDATE를 쓰도록 name 제거
			const dt = document.getElementById('cons-date');
			if (dt && !dt.value) dt.name = ''; // 서버 바인딩 제외
		});
	}

	// ===== 삭제 확인 (showConfirm) 병합 =====
	// 단일 폼(id="delete-form")도 지원, 다수 폼(.js-delete-form)도 지원
	const attachDeleteConfirm = (f) => {
		const submitBtn = f.querySelector('button[type="submit"]');
		f.addEventListener('submit', async (e) => {
			e.preventDefault(); // 기본 제출 차단
			submitBtn?.setAttribute('disabled', 'disabled'); // 중복 클릭 방지

			// 다이얼로그 안이면 target 지정
			const dlg = f.closest('dialog');
			const { isConfirmed } = await showConfirm({
				title: '삭제',
				text: '해당 소비목록을 삭제하시겠습니까?',
				confirmButtonText: '네, 삭제합니다',
				cancelButtonText: '아니요',
				...(dlg ? { target: dlg } : {})
			});

			if (isConfirmed) {
				f.submit(); // 실제 제출
			} else {
				submitBtn?.removeAttribute('disabled');
			}
		});
	};

	const deleteFormById = document.getElementById('delete-form');
	if (deleteFormById) attachDeleteConfirm(deleteFormById);
	document.querySelectorAll('form.js-delete-form').forEach(attachDeleteConfirm);

	// 포인트 전액 사용
	const pointEl = document.getElementById('point-available');
	const availablePoint = (() => {
		if (!pointEl) return 0;
		const raw = pointEl.dataset.raw ?? pointEl.textContent ?? '0';
		const num = Number(String(raw).replace(/[^\d.-]/g, ''));
		return Number.isFinite(num) ? num : 0;
	})();

	const btnUseAll = document.getElementById("use-all-point");
	const inputPoint = document.getElementById("use-point");
	if (btnUseAll && inputPoint) {
		btnUseAll.addEventListener("click", () => {
			const amtInput = document.getElementById("amount");
			const amount = Number(amtInput?.value || 0);

			if (amount > 0 && availablePoint > 0) {
				// 금액 < 보유포인트
				if (amount < availablePoint) {
					inputPoint.value = amount;
				} else {
					// 금액 >= 보유포인트
					inputPoint.value = availablePoint;
				}
			} else {
				// 금액이 없으면 그냥 전액 사용
				inputPoint.value = availablePoint;
			}
		});
	}
});
