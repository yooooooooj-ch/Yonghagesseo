importScripts('https://www.gstatic.com/firebasejs/10.5.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.5.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "AIzaSyDyRD6EcQ4kzT0AnwQ781KKn11tz5bPyRo",
  authDomain: "yongha-push-test.firebaseapp.com",
  projectId: "yongha-push-test",
  storageBucket: "yongha-push-test.appspot.com",
  messagingSenderId: "564950133395",
  appId: "1:564950133395:web:6146004746b8db3997fb66"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  console.log('[SW] payload =', payload);
  console.log('[SW] payload.notification =', payload.notification);
  console.log('[SW] payload.data =', payload.data);

  if (payload.notification && (payload.notification.title || payload.notification.body)) {
    console.warn('[SW] auto notification detected -> skip'); 
    return;
  }

  const d = payload.data || {};
  console.log('[SW] using title/body =>', d.title, d.body);
  const title = d.title || '알림';
  const body  = d.body  || '';
  const tag   = d.tag   || `${d.type || 'default'}:${d.goal_no || ''}`;
  
	self.registration.showNotification(title, {
	  body,
	  tag,
	  renotify: false,
	  icon: d.icon,
	  badge: d.badge,
	  data: d.click_action ? { click_action: d.click_action } : {}
	});
});

self.addEventListener('notificationclick', (event) => {
  console.log('[SW] notification click', event.notification);

  event.notification.close(); // 알림 닫기

  // 우리가 data에 담은 click_action 꺼내기
  const targetUrl =
    (event.notification.data && event.notification.data.click_action) || '/';

  // 이미 열린 탭 있으면 focus, 없으면 새 창 열기
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if (client.url.includes(targetUrl) && 'focus' in client) {
          return client.focus();
        }
      }
      if (clients.openWindow) {
        return clients.openWindow(targetUrl);
      }
    })
  );
});


