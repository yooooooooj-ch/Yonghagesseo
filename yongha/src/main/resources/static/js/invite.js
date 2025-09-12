
function invite() {
	const child_email = document.getElementById("child_email").value;

	fetch("/rest/users/invite", {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify({ child_email })
	})
                .then(async res => {
                        const resBody = await res.json();

                        if (res.ok) {
                                location.href = '/invite_family/qr?token=' + encodeURIComponent(resBody.token);
                        } else {
                                showAlert({
                                        title: '아기용 초대에 실패했습니다.',
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