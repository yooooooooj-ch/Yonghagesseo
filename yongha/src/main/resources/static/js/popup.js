document.addEventListener('DOMContentLoaded', () => {

    // 팝업 열기
    window.openPopup = function(selector) {
        const popup = document.querySelector(selector);
        if (popup) popup.classList.add('show');
    }

    // 팝업 닫기
    window.closePopup = function(selector) {
        const popup = document.querySelector(selector);
        if (popup) popup.classList.remove('show');
    }

    // 모든 닫기 버튼에 이벤트 바인딩
    document.querySelectorAll('.popup-close').forEach(btn => {
        btn.addEventListener('click', () => {
            const popup = btn.closest('.popup-layer');
            if (popup) popup.classList.remove('show');
        });
    });

});
