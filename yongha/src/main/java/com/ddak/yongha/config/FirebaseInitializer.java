package com.ddak.yongha.config;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;

@Configuration
public class FirebaseInitializer {

    private static final String PROJECT_ID = "yongha-push-test";
    private static final String SA_PATH = "firebase/yongha-push-test-firebase-adminsdk-fbsvc-696e62054e.json";

    @PostConstruct
    public void init() {
        try {
            // 1) 정확히 어떤 파일을 읽는지 경로 로그
            ClassPathResource res = new ClassPathResource(SA_PATH);
            if (!res.exists()) {
                throw new IllegalStateException("❌ 서비스계정 JSON 못 찾음: " + SA_PATH + " (src/main/resources/firebase/ 아래)");
            }
            System.out.println("[Admin] SA resource = " + res.getURL());

            // 2) 파일 내용에 project_id가 실제로 있는지 1회 검증(디버깅용)
            byte[] bytes;
            try (InputStream in = res.getInputStream()) {
                bytes = in.readAllBytes();
            }
            String jsonPreview = new String(bytes, StandardCharsets.UTF_8);
            System.out.println("[Admin] SA has project_id? " + jsonPreview.contains("\"project_id\""));

            // 3) Credentials 로드
            GoogleCredentials creds = GoogleCredentials.fromStream(new java.io.ByteArrayInputStream(bytes));

            // 4) projectId 명시 (추론 실패/구버전 JSON 대비)
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(creds)
                    .setProjectId(PROJECT_ID)
                    .build();

            // 5) 기존 잘못 초기화된 인스턴스가 있으면 정리 후 재초기화
            FirebaseApp app;
            if (FirebaseApp.getApps().isEmpty()) {
                app = FirebaseApp.initializeApp(options);
            } else {
                app = FirebaseApp.getInstance();
                String current = app.getOptions().getProjectId();
                if (current == null || !PROJECT_ID.equals(current)) {
                    System.out.println("[Admin] Reinitializing FirebaseApp (old projectId=" + current + ")");
                    app.delete();
                    app = FirebaseApp.initializeApp(options);
                }
            }

            System.out.println("[Admin] projectId=" + FirebaseApp.getInstance().getOptions().getProjectId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
