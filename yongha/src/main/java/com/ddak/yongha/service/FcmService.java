package com.ddak.yongha.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ddak.yongha.mapper.FcmTokenMapper;
import com.ddak.yongha.vo.FcmRequestVo;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {
	
	private static final String DEFAULT_TITLE = "알림";
    private static final String DEFAULT_BODY  = "메시지가 도착했습니다.";
    private static final String DEFAULT_ICON  = "/img/Yongha.png";
    private static final String DEFAULT_BADGE  = "/img/Yongha.png";
    private static final String DEFAULT_URL = "/";

    private final FcmTokenMapper fcmTokenMapper;

    // 1) 바깥에서 쓰던 진입점 유지. 실제 전송은 data-only.
    public void sendNotification(String token, String title, String body, String imageUrl) {
        FcmRequestVo req = new FcmRequestVo();
        req.setToken(token);
        req.setNotification(new FcmRequestVo.Notification(title, body, imageUrl));
        // SW가 쓸 데이터 페이로드(=VO의data)를 반드시 채움
        req.getData().put("title", StringUtils.hasText(title) ? title : DEFAULT_TITLE);
        req.getData().put("body",  StringUtils.hasText(body)  ? body  : DEFAULT_BODY);
        req.getData().put("icon",  DEFAULT_ICON);      // 웹에서 image 대신 icon 사용
        req.getData().put("badge",  DEFAULT_BADGE); 
        req.getData().put("click_action",  DEFAULT_URL); 
//        log.info("FCM prepare (sendNotification) to={}..., title={}, body={}", mask(token), title, body);
        sendPushNotification(req);
    }

    // 2) 단일 토큰 전송: data-only
    public String sendPushNotification(FcmRequestVo req) {
        if (req == null || !StringUtils.hasText(req.getToken())) {
            throw new IllegalArgumentException("타겟 토큰이 비어 있습니다.");
        }
        final Map<String,String> data = (req.getData() != null) ? req.getData() : Map.of();

        // ✅ 전송 직전, 실제 data 로그
//        log.info("FCM (sendPushNotification) to={}..., req={}", mask(req.getToken()), req);

        Message message = Message.builder()
                .setToken(req.getToken())
                .putAllData(data) // data-only
                .build();

        try {
            String messageId = FirebaseMessaging.getInstance().send(message); // **전송**
//            log.info("✅ FCM sent (data-only): id={}", messageId);
            return messageId;
        } catch (Exception e) {
//            log.error("❌ FCM send failed: token={}..., title={}, body={}",mask(req.getToken()), req.getNotification().getTitle(), req.getNotification().getBody(), e);
            throw new RuntimeException("FCM 전송 실패", e);
        }
    }

    // 공용 유틸 - log용
    private String mask(String token) {
        if (!StringUtils.hasText(token)) return "";
        int vis = Math.min(6, token.length());
        return token.substring(0, vis) + "...(" + token.length() + ")";
    }

    public void saveToken(int userNo, String token) {
        int updated = fcmTokenMapper.updateFcmToken(userNo, token);
        if (updated == 0) {
            throw new IllegalStateException("update failed for userNo=" + userNo);
        }
    }
}
