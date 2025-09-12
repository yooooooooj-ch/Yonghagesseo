// 통합 로그아웃 
function logout() {
	// 서버 세션 무효화
	fetch('/api/user/logout', { method: 'POST' })
		.then(() => {
			// 토큰, 클라이언트 스토리지 초기화
			localStorage.removeItem('access_token');
			localStorage.removeItem('socialData');
			sessionStorage.clear();
			// 메인페이지로 이동
			window.location.href = '/';
		});
}