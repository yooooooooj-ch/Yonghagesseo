/* ===================================================================
   Yongha Admin JS (Server-driven list)
   - 서버 GET 파라미터로 검색/정렬/페이징 처리
   - 프론트 정렬/필터 제거
   - 체크박스 선택 상태는 localStorage로 페이지 이동 후에도 유지
=================================================================== */

/* =========================
   Helpers
========================== */
const isAdminRoute = () => location.pathname.startsWith('/admin');
const pathNow = location.pathname.toLowerCase();

/* =========================
   Topbar support (상단 메뉴 활성화 + 검색폼 Enter)
========================== */
(function topbarInit() {
	if (!isAdminRoute()) return;

	// 링크 활성화 (.atb-link)
	document.querySelectorAll('.atb-link').forEach(a => {
		const m = (a.getAttribute('data-match') || a.getAttribute('href') || '').toLowerCase();
		if (m && pathNow.startsWith(m)) a.classList.add('active');
	});

	// 검색 인풋 Enter → 폼 제출 (상단 통합 검색이 있는 경우)
	const searchInput = document.querySelector('.atb-search input[type="search"]');
	searchInput?.addEventListener('keydown', (e) => {
		if (e.key === 'Enter') e.target.form?.submit();
	});
})();

/* =========================
   Admin 페이지에서 마케팅 히어로/탭 제거
========================== */
(function nukeMarketingHero() {
	if (!isAdminRoute()) return;

	const KILL = [
		'.site-top',
		'.site-hero',
		'.hero',
		'.top-tabs',
		'#pageHead',
		'.page-head.header-hero',
		'.logo-white-circle',
		'.logo-gradient-glow'
	].join(',');

	const kill = () => {
		document.querySelectorAll(KILL).forEach(el => {
			if (el.closest('.admin-topbar')) return; // 우리 Topbar 내부면 건너뜀
			el.remove();
		});
	};

	// 즉시 1회
	kill();

	// DOM 변동 감시(최대 5초)
	const mo = new MutationObserver(kill);
	mo.observe(document.documentElement, { childList: true, subtree: true });
	setTimeout(() => mo.disconnect(), 5000);

	// 폴링(주입 지연 대비)
	let tries = 30; // 3초
	const it = setInterval(() => { kill(); if (--tries <= 0) clearInterval(it); }, 100);
})();

/* =========================
   Sidebar active / toggle (있을 때만 동작)
========================== */
(function sidebarActive() {
	document.querySelectorAll('.as-link').forEach(a => {
		const m = (a.getAttribute('data-match') || a.getAttribute('href') || '').toLowerCase();
		if (m && pathNow.startsWith(m)) a.classList.add('active');
	});
})();
(function sidebarToggle() {
	const sidebar = document.getElementById('adminSidebar');
	const openBtn = document.getElementById('sidebarToggle');
	const closeBtn = document.getElementById('sidebarClose');
	if (sidebar && openBtn) openBtn.addEventListener('click', () => sidebar.classList.add('open'));
	if (sidebar && closeBtn) closeBtn.addEventListener('click', () => sidebar.classList.remove('open'));
})();

/* =========================
   선택 상태(localStorage) + 벌크바 + 삭제
   - 프론트 필터/정렬 제거됨
   - 서버에서 검색/정렬/페이징
========================== */
(function selectionAndActions() {
	const STORAGE_KEY = 'adminSelectedUsers';

	function loadSelected() {
		try { return JSON.parse(localStorage.getItem(STORAGE_KEY)) || []; }
		catch { return []; }
	}
	function saveSelected(arr) {
		localStorage.setItem(STORAGE_KEY, JSON.stringify(arr));
	}

	let selectedUserNos = loadSelected();

	// DOM refs
	const checkAll = document.getElementById('checkAll');
	const bulkBar = document.getElementById('bulkBar');
	const selCount = document.getElementById('selCount');

	// 현재 페이지 체크박스들
	function pageRowChecks() {
		return [...document.querySelectorAll('.row-check')];
	}

	// Storage → DOM 반영
	function syncChecksFromStorage() {
		pageRowChecks().forEach(cb => {
			if (!cb.value) return;
			cb.checked = selectedUserNos.includes(String(cb.value));
		});
	}

	// DOM → Storage 반영
	function updateSelectedFromDOM() {
		pageRowChecks().forEach(cb => {
			const val = String(cb.value || '');
			if (!val) return;
			const has = selectedUserNos.includes(val);
			if (cb.checked && !has) selectedUserNos.push(val);
			if (!cb.checked && has) selectedUserNos = selectedUserNos.filter(x => x !== val);
		});
		saveSelected(selectedUserNos);
	}

	function refreshBulk() {
		const n = selectedUserNos.length;
		if (selCount) selCount.textContent = n;
		if (bulkBar) bulkBar.hidden = n === 0;

		// 현재 페이지 전체선택 체크 여부
		const rows = pageRowChecks();
		const allOnThisPage = rows.length > 0 && rows.every(cb => selectedUserNos.includes(String(cb.value)));
		if (checkAll) checkAll.checked = allOnThisPage;
	}

	// 초기 복원
	syncChecksFromStorage();
	refreshBulk();

	// 개별 체크 변경
	pageRowChecks().forEach(cb => {
		cb.addEventListener('change', () => {
			updateSelectedFromDOM();
			refreshBulk();
		});
	});

	// 전체 선택
	checkAll?.addEventListener('change', e => {
		const on = e.target.checked;
		pageRowChecks().forEach(cb => (cb.checked = on));
		updateSelectedFromDOM();
		refreshBulk();
	});

	// 선택 해제
	const btnUnselect = document.querySelector('.bb-right .btn:not(.danger)'); // "선택 해제"
	btnUnselect?.addEventListener('click', () => {
		selectedUserNos = [];
		saveSelected(selectedUserNos);
		pageRowChecks().forEach(cb => (cb.checked = false));
		if (checkAll) checkAll.checked = false;
		refreshBulk();
	});


	// 삭제 API
	async function deleteUsers(userNo) {
		return fetch('/rest/users/admin', {
			method: 'DELETE',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ userNo })
		});
	}

	function removeFromSelected(removedIds = []) {
		removedIds = removedIds.map(String);
		selectedUserNos = selectedUserNos.filter(x => !removedIds.includes(x));
		saveSelected(selectedUserNos);
	}

	// 벌크 삭제
	const bulkDeleteBtn = document.querySelector('.bb-right .btn.danger');
	bulkDeleteBtn?.addEventListener('click', async () => {
		const userNo = [...selectedUserNos]; // 전체 선택 목록 기준
		if (!userNo.length) return await showAlert({ title: '실패', text: '선택된 항목이 없습니다', icon: 'warning' });

		const result = await showConfirm({
			title: '회원 삭제',
			text: `${userNo.length}명을 삭제하시겠습니까?`,
			confirmButtonText: '네, 삭제합니다',
			cancelButtonText: '아니요'
		});
		if (!result.isConfirmed) return;

		try {
			const res = await deleteUsers(userNo);
			const data = await res.json().catch(() => ({}));
			if (!res.ok) throw new Error(data.message || `HTTP ${res.status}`);

			// UI 반영(현재 페이지에 있는 행만 제거)
			userNo.forEach(id => document.querySelector(`tr[data-user-no="${id}"]`)?.remove());
			// 선택목록에서 제거
			removeFromSelected(userNo);
			refreshBulk();

			await showAlert({ title: '완료', text: data.message || '삭제되었습니다', icon: 'success' });
		} catch (e) {
			await showAlert({ title: '실패', text: e.message || '삭제 실패', icon: 'warning' });
		}
	});

	// 행별 삭제(위임)
	const table = document.getElementById('userTable');
	const tbody = table?.tBodies?.[0];
	tbody?.addEventListener('click', async (e) => {
		const btn = e.target.closest('.row-actions .btn.danger');
		if (!btn) return;
		const tr = btn.closest('tr');
		const userNo = tr?.dataset.userNo || tr?.querySelector('.row-check')?.value;
		if (!userNo) return;

		const result = await showConfirm({
			title: '회원 삭제',
			text: '이 사용자를 삭제하시겠습니까?',
			confirmButtonText: '네, 삭제합니다',
			cancelButtonText: '아니요'
		});
		if (!result.isConfirmed) return;

		try {
			const res = await deleteUsers([Number(userNo)]);
			const data = await res.json().catch(() => ({}));
			if (!res.ok) throw new Error(data.message || `HTTP ${res.status}`);

			tr.remove();
			removeFromSelected([userNo]);
			refreshBulk();

			await showAlert({ title: '완료', text: data.message || '삭제되었습니다', icon: 'success' });
		} catch (e) {
			await showAlert({ title: '실패', text: e.message || '삭제 실패', icon: 'warning' });
		}
	});
})();
