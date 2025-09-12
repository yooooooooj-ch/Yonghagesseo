package com.ddak.yongha.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ddak.yongha.service.FcmService;
import com.ddak.yongha.vo.FcmRequestVo;

@RestController
@RequestMapping("/test")
public class TestFcmAutoPushController {

    @Autowired
    private FcmService fcmService;

    @GetMapping("/charge-success")
    public ResponseEntity<String> testAutoPush() {
        String parentToken = "여기_실제_부모_토큰_넣기";

        FcmRequestVo fcmRequest = new FcmRequestVo();
        fcmRequest.setToken(parentToken);

        FcmRequestVo.Notification notification = new FcmRequestVo.Notification();
        notification.setTitle("용돈 충전 완료!");
        notification.setBody("자녀에게 용돈이 충전되었어요 💰");
        fcmRequest.setNotification(notification);

        fcmService.sendPushNotification(fcmRequest);

        return ResponseEntity.ok("자동 푸시 테스트 완료!");
    }
}
