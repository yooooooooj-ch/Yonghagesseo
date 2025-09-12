let currentIndex = 0;
let childCards = [];

function showAutoTransferInfo(data) {
	const panel = document.getElementById('auto-transfer-panel');
	const info = document.getElementById('auto-transfer-info');
	const form = document.getElementById('auto-transfer-form');
	document.getElementById('auto-current-amount').innerText = data.amount;
	document.getElementById('auto-current-cycle').innerText = data.trans_cycle;
	document.getElementById('auto-amount-input').value = data.amount;
	const radio = document.querySelector(`input[name="auto-edit-cycle"][value="${data.trans_cycle}"]`);
	if (radio)
		radio.checked = true;
	form.style.display = 'none';
	info.style.display = 'block';
	panel.classList.add('show');
	panel.style.display = 'block';
}

function showAutoTransferForm() {
	const panel = document.getElementById('auto-transfer-panel');
	const info = document.getElementById('auto-transfer-info');
	const form = document.getElementById('auto-transfer-form');
	document.getElementById('auto-new-amount').value = 0;
	document.querySelectorAll('input[name="auto-new-cycle"]').forEach(r => (r.checked = false));
	info.style.display = 'none';
	form.style.display = 'block';
	panel.classList.add('show');
	panel.style.display = 'block';
}

function hideAutoTransferPanel() {
	const panel = document.getElementById('auto-transfer-panel');
	panel.classList.remove('show');
	panel.style.display = 'none';
}

function openTransferPanel(card) {
	const childNo = card.dataset.childNo;
	const childName = card.querySelector('.child-name').innerText;
	const imgSrc = card.querySelector('img').src;
	const childAccount = card.dataset.accountNo;

	// 송금폼 정보 업데이트
	document.getElementById('transfer-child-name').innerText = childName;
	document.getElementById('transfer-child-img').src = imgSrc;
	document.getElementById('to_account_no').value = childAccount;

	const fromAccount = document.getElementById('from_account_no').value;
	fetch(`/api/transfer/auto-transfer?from_account_no=${fromAccount}&to_account_no=${childAccount}`)
		.then(res => (res.status === 200 ? res.json() : null))
		.then(data => {
			if (data) {
				showAutoTransferInfo(data);
			} else {
				showAutoTransferForm();
			}
		});

	// 송금폼 상태 UI 변경
	const wrapper = document.querySelector('.child-list-wrapper');
	wrapper.classList.add('single-view');
	wrapper.querySelectorAll('.nav-btn').forEach(btn => btn.style.display = 'block');

	renderChildCard(currentIndex);

	const panel = document.getElementById('transfer-panel');
	panel.classList.add('show');
	panel.style.display = 'block';
}

function closeTransferPanel() {
	const wrapper = document.querySelector('.child-list-wrapper');
	wrapper.classList.remove('single-view');
	wrapper.querySelectorAll('.nav-btn').forEach(btn => btn.style.display = 'none');

	// 자녀 카드 전체 다시 표시
	const container = document.querySelector('.child-list');
	container.innerHTML = '';
	childCards.forEach((card, index) => {
		const cloned = card.cloneNode(true);
		cloned.querySelector('.give-btn').addEventListener('click', (e) => {
			e.stopPropagation();
			currentIndex = index;
			openTransferPanel(cloned);
		});
		container.appendChild(cloned);
	});

	// 송금 패널 닫기
	const panel = document.getElementById('transfer-panel');
	panel.classList.remove('show');
	setTimeout(() => panel.style.display = 'none', 300);

	hideAutoTransferPanel();
}

function renderChildCard(index) {
	const container = document.querySelector('.child-list');
	container.innerHTML = '';
	const selectedCard = childCards[index].cloneNode(true);
	selectedCard.classList.add('enter');
	requestAnimationFrame(() => {
		selectedCard.classList.add('enter-active');
	});

	selectedCard.querySelector('.give-btn').addEventListener('click', (e) => {
		e.stopPropagation();
		openTransferPanel(selectedCard);
	});

	container.appendChild(selectedCard);
}

document.addEventListener('DOMContentLoaded', () => {
	childCards = Array.from(document.querySelectorAll('.child-card'));

	// URL 파라미터 확인 → child_no 있으면 자동 오픈
	const urlParams = new URLSearchParams(window.location.search);
	const childNoParam = urlParams.get('child_no');
	if (childNoParam) {
		const targetIndex = childCards.findIndex(c => c.dataset.childNo === childNoParam);
		if (targetIndex !== -1) {
			currentIndex = targetIndex;
			openTransferPanel(childCards[targetIndex]);
		}
	}

	// 용돈 지급 버튼 클릭 이벤트
	document.querySelectorAll('.give-btn').forEach((btn, index) => {
		btn.addEventListener('click', e => {
			e.stopPropagation();
			currentIndex = index;
			openTransferPanel(childCards[index]);
		});
	});

	// 이전 버튼
	document.querySelector('.prev-btn').addEventListener('click', () => {
		currentIndex = (currentIndex - 1 + childCards.length) % childCards.length;
		renderChildCard(currentIndex);
		openTransferPanel(childCards[currentIndex]);
	});

	// 다음 버튼
	document.querySelector('.next-btn').addEventListener('click', () => {
		currentIndex = (currentIndex + 1) % childCards.length;
		renderChildCard(currentIndex);
		openTransferPanel(childCards[currentIndex]);
	});

	// 금액 버튼
	window.setAmount = function(value) {
		const amountInput = document.getElementById('amount');
		const currentValue = parseInt(amountInput.value) || 0;
		amountInput.value = currentValue + value;
	};

	document.getElementById('amount-init').addEventListener('click', () => {
		document.getElementById('amount').value = 0;
	});

	document.getElementById('auto-new-init').addEventListener('click', () => {
		document.getElementById('auto-new-amount').value = 0;
	});

	document.getElementById('auto-edit-init').addEventListener('click', () => {
		document.getElementById('auto-amount-input').value = 0;
	});
});

function sendAllowance() {
	const amount = document.getElementById('amount').value;
	const from_account_no = document.getElementById('from_account_no').value;
	const to_account_no = document.getElementById('to_account_no').value;
	const trans_desc = document.getElementById('amount-desc').value;

	if (!amount || amount <= 0) {
		showAlert({
			text: '금액을 입력하세요.',
			icon: 'warning'
		});
		return;
	}

	fetch("/api/transfer/allowance", {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify({ from_account_no, to_account_no, amount, trans_desc })
	})
		.then(async res => {
			const resBody = await res.json();

			if (res.ok) {
				await showAlert({
					text: resBody.message,
					icon: 'success'
				});
				location.href = '/parent_page';
			} else {
				showAlert({
					title: '용돈 주기에 실패했습니다.',
					text: resBody.message,
					icon: 'warning'
				});
				console.warn(resBody.message);
			}
		})
		.catch(err => {
			console.error("요청 실패:", err);
			showAlert({
				text: "알 수 없는 오류가 발생했습니다.",
				icon: 'error'
			});
		});

}

function cancelAutoTransfer() {
	const from_account_no = document.getElementById('from_account_no').value;
	const to_account_no = document.getElementById('to_account_no').value;
	fetch('/api/transfer/auto-transfer', {
		method: 'DELETE',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ from_account_no, to_account_no })
	})
		.then(res => res.json())
		.then(async res => {
			await showAlert({ text: res.message, icon: 'success' });
			showAutoTransferForm();
		})
		.catch(err => {
			showAlert({ text: '오류 발생', icon: 'warning' });
			console.log(err.message);
		});
}

function updateAutoTransfer() {
	const from_account_no = document.getElementById('from_account_no').value;
	const to_account_no = document.getElementById('to_account_no').value;
	const amount = document.getElementById('auto-amount-input').value;
	const cycleInput = document.querySelector('input[name="auto-edit-cycle"]:checked');
	if (!amount || amount <= 0) {
		alert('금액을 입력하세요.');
		return;
	}
	if (!cycleInput) {
		alert('주기를 선택해주세요.');
		return;
	}
	const trans_cycle = cycleInput.value;
	fetch('/api/transfer/auto-transfer', {
		method: 'PUT',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ from_account_no, to_account_no, amount, trans_cycle })
	})
		.then(res => res.json())
		.then(async res => {
			await showAlert({ text: res.message, icon: 'success' });
			showAutoTransferInfo({ amount, trans_cycle });
		})
		.catch(err => {
			showAlert({ text: '오류 발생', icon: 'warning' });
			console.log(err.message);
		});
}

function registerAutoTransfer() {
	const from_account_no = document.getElementById('from_account_no').value;
	const to_account_no = document.getElementById('to_account_no').value;
	const amount = document.getElementById('auto-new-amount').value;
	const cycleInput = document.querySelector('input[name="auto-new-cycle"]:checked');
	if (!amount || amount <= 0) {
		alert('금액을 입력하세요.');
		return;
	}
	if (!cycleInput) {
		alert('주기를 선택해주세요.');
		return;
	}
	const trans_cycle = cycleInput.value;
	fetch('/api/transfer/auto-transfer', {
		method: 'POST',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ from_account_no, to_account_no, amount, trans_cycle })
	})

		.then(async res => {
			const resBody = await res.json();

			if (res.ok) {
				await showAlert({
					text: resBody.message,
					icon: 'success'
				});
				showAutoTransferInfo({ amount, trans_cycle });
			} else {
				showAlert({
					title: '용돈 주기에 실패했습니다.',
					text: resBody.message,
					icon: 'warning'
				});
				console.warn(resBody.message);
			}
		})


		.catch(err => {
			showAlert({ text: '오류 발생', icon: 'warning' });
			console.log(err.message);
		});
}

function setAutoAmount(id, value) {
	const input = document.getElementById(id);
	const current = parseInt(input.value) || 0;
	input.value = current + value;
}

