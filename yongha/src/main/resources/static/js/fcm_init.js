// fcm_init.js
// Firebase + FCM 공통 초기화

// ✅ Firebase SDK를 이미 html에서 불러온 경우만 사용
const firebaseConfig = {
  apiKey: "AIzaSyDyRD6EcQ4kzT0AnwQ781KKn11tz5bPyRo",
  authDomain: "yongha-push-test.firebaseapp.com",
  projectId: "yongha-push-test",
  storageBucket: "yongha-push-test.appspot.com",
  messagingSenderId: "564950133395",
  appId: "1:564950133395:web:6146004746b8db3997fb66"
};

const VAPID_KEY =
  "BHLjffHPAsqSalBfSy8qKak3Q0dHvhI47k4dj10l7WN2GYOC3F5yrMY6yOUCYvVt8YUO4Xz9dvMYI4X6uuQMBXU";

// Firebase 앱 하나만 초기화
if (!firebase.apps.length) {
  firebase.initializeApp(firebaseConfig);
}
const messaging = firebase.messaging();

// SW 등록 함수
async function registerSw() {
  if (!("serviceWorker" in navigator)) return null;
  return await navigator.serviceWorker.register(
    "/firebase-messaging-sw.js",
    { scope: "/" }
  );
}

// ✅ 토큰 발급 함수 (체크박스 동의 여부는 페이지에서 확인)
async function getFcmToken() {
  const reg = await registerSw();

  if (!reg) throw new Error("Service Worker registration 실패");

  const token = await messaging.getToken({
    vapidKey: VAPID_KEY,
    serviceWorkerRegistration: reg
  });

  return token;
}

// export (전역으로)
window.FcmInit = {
  getFcmToken,
};
