async function accept() {
	const btn = document.getElementById('btn-accept');
        const urlParams = new URLSearchParams(window.location.search);
        const token = urlParams.get('token');

        if (!token) {
                await showAlert({ title: '실패', text: '잘못된 초대 링크입니다.', icon: 'warning' });
                return;
        }

	try {
		btn.classList.add('loading'); btn.disabled = true;

                const res = await fetch('/rest/users/family', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ token })
                });
		const resBody = await res.json();

		if (res.ok) {
			await showAlert({ title: '완료', text: resBody.message, icon: 'success' });
			location.href = '/';
		} else {
			await showAlert({ title: '실패', text: resBody.message, icon: 'warning' });
			location.href = '/';
		}
	} catch (e) {
		console.error(e);
		await showAlert({ text: '알 수 없는 오류가 발생했습니다.', icon: 'error' });
	} finally {
		btn.classList.remove('loading'); btn.disabled = false;
	}
}

async function reject() {
	await showAlert({ text: '가족 초대를 거절했습니다.', icon: 'success' });
	location.href = '/';
}