// 요소
const fileInput = document.getElementById('bn-file-input');   // 숨겨진 input
const dropZone = document.getElementById('dropZone');        // 드롭존
const dropContent = document.getElementById('dropContent');     // 드롭존 안 표시부
const trigger = document.getElementById('bn-file-trigger'); // 🖼️ 트리거

// 유효성
const MAX_MB = 10;
function validateFile(file) {
	if (!file) return '파일이 없습니다.';
	if (!file.type.startsWith('image/')) return '이미지 파일만 업로드할 수 있습니다.';
	if (file.size > MAX_MB * 1024 * 1024) return `파일 용량은 ${MAX_MB}MB 이하이어야 합니다.`;
	return null;
}

// 프리뷰를 "드롭존 내부"에 표시
function renderPreviewIntoDrop(file) {
	const err = validateFile(file);
	if (err) {
		alert(err);
		fileInput.value = '';
		// 초기 상태로 복구
		dropContent.classList.add('empty');
		dropContent.innerHTML = `
      <div id="bn-file-trigger" class="bn-file clickable" style="font-size:28px" role="button" tabindex="0" aria-label="배너 이미지 선택">🖼️</div>
      <div class="help">파일을 끌어놓거나 클릭하여 업로드</div>
    `;
		wireTrigger(); // 트리거 재바인딩
		return;
	}

	const reader = new FileReader();
	reader.onload = (e) => {
		dropContent.classList.remove('empty');
		dropContent.innerHTML = `
      <img src="${e.target.result}" alt="미리보기 이미지" id="dropPreviewImg">
    `;
		// 프리뷰 클릭해도 다시 파일 선택 열림
		const img = document.getElementById('dropPreviewImg');
		img.addEventListener('click', () => fileInput.click());
	};
	reader.readAsDataURL(file);
}

// 🖼️ 클릭 → 파일 선택 열기
function wireTrigger() {
	const t = document.getElementById('bn-file-trigger');
	if (!t) return;
	t.addEventListener('click', () => fileInput.click());
	t.addEventListener('keydown', e => {
		if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); fileInput.click(); }
	});
}
wireTrigger();

// input change → 드롭존에 프리뷰
fileInput.addEventListener('change', () => {
	const file = fileInput.files?.[0];
	if (file) renderPreviewIntoDrop(file);
});

// 드래그&드롭
['dragenter', 'dragover', 'dragleave', 'drop'].forEach(evt => {
	dropZone.addEventListener(evt, e => { e.preventDefault(); e.stopPropagation(); });
});
['dragenter', 'dragover'].forEach(evt => {
	dropZone.addEventListener(evt, () => dropZone.classList.add('dragover'));
});
['dragleave', 'drop'].forEach(evt => {
	dropZone.addEventListener(evt, () => dropZone.classList.remove('dragover'));
});
dropZone.addEventListener('drop', e => {
	const file = e.dataTransfer?.files?.[0];
	if (!file) return;

	// form 제출이 가능하도록 input.files에 주입
	const dt = new DataTransfer();
	dt.items.add(file);
	fileInput.files = dt.files;

	renderPreviewIntoDrop(file);
});

// 날짜 제약
const startInput = document.getElementById('startDate');
const endInput = document.getElementById('endDate');
startInput.addEventListener('change', () => { endInput.min = startInput.value; });
endInput.addEventListener('change', () => { startInput.max = endInput.value; });
