package com.ddak.yongha.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FcmRequestVo {
    // private String userId;
    private int user_no;             // DB 저장용 식별자(Users.user_no)
    private String token;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Notification {
        private String title;
        private String body;
        private String image;
    }

    private Notification notification;            // 푸시 전송 시 사용

    // ✅ 추가: 데이터 페이로드 (선택)
    // null-safe 하게 쓰고 싶으면 기본값을 빈 HashMap으로 두면 편해요.
    private Map<String, String> data = new HashMap<>();
}
