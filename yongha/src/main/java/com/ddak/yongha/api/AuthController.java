package com.ddak.yongha.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ddak.yongha.service.EmailService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final EmailService emailService;

	private static final String SESSION_AUTH_CODE = "authCode";
	private static final String SESSION_AUTH_EXP = "authCodeExpiresAt";
	private static final String SESSION_LAST_SEND = "authCodeLastSentAt";

	private static final long CODE_TTL_MILLIS = 5 * 60 * 1000; // 5분
	private static final long RESEND_COOLDOWN = 60 * 1000; // 60초(옵션)

	// 인증코드 발급 + 메일 전송
	@PostMapping("/veri/send-email-code")
	public Map<String, Object> sendEmailCode(@RequestBody Map<String, String> param, HttpSession session) {
		String to = param.get("email");
		Map<String, Object> result = new HashMap<>();

		if (to == null || to.isBlank()) {
			result.put("success", false);
			result.put("msg", "이메일 주소가 필요합니다.");
			return result;
		}

		// (옵션) 재발송 쿨다운
		Long lastSent = (Long) session.getAttribute(SESSION_LAST_SEND);
		long now = System.currentTimeMillis();
		if (lastSent != null && now - lastSent < RESEND_COOLDOWN) {
			long waitSec = (RESEND_COOLDOWN - (now - lastSent)) / 1000;
			result.put("success", false);
			result.put("msg", "인증코드를 너무 자주 요청하고 있습니다. " + waitSec + "초 후 다시 시도하세요.");
			return result;
		}

		// 6자리 코드 생성
		String code = generate6DigitCode();

		// 세션에 코드 + 만료시각 저장
		session.setAttribute(SESSION_AUTH_CODE, code);
		session.setAttribute(SESSION_AUTH_EXP, now + CODE_TTL_MILLIS);
		session.setAttribute(SESSION_LAST_SEND, now);

		// 메일 비동기 발송 (성공/실패는 서버 로그로 관리, 프론트엔드는 요청 접수 기준으로 알림)
		emailService.sendVerificationCodeMail(to, code);

		result.put("success", true);
		result.put("msg", "인증코드를 전송했습니다. 메일함을 확인해주세요.");
		return result;
	}

	// 본인인증 코드 검증 (기존 메서드 개선: 만료시간 검사 + 1회성 사용)
	@PostMapping("/veri/check-email-code")
	public Map<String, Object> checkEmailCode(@RequestBody Map<String, String> param, HttpSession session) {
		String inputCode = param.get("authCode");
		String sessionCode = (String) session.getAttribute(SESSION_AUTH_CODE);
		Long expiresAt = (Long) session.getAttribute(SESSION_AUTH_EXP);

		Map<String, Object> result = new HashMap<>();

		if (sessionCode == null || expiresAt == null) {
			result.put("success", false);
			result.put("msg", "발급된 인증코드가 없습니다. 먼저 인증코드를 요청해주세요.");
			return result;
		}

		long now = System.currentTimeMillis();
		if (now > expiresAt) {
			clearAuthCode(session);
			result.put("success", false);
			result.put("msg", "인증코드가 만료되었습니다. 다시 요청해주세요.");
			return result;
		}

		if (sessionCode.equals(inputCode)) {
			// 성공 시 코드 폐기(1회성)
			clearAuthCode(session);
			result.put("success", true);
		} else {
			result.put("success", false);
			result.put("msg", "인증코드가 일치하지 않습니다.");
		}
		return result;
	}

	// ----- 내부 유틸 -----
	private String generate6DigitCode() {
		// 000000 ~ 999999 (0 패딩)
		int n = new java.security.SecureRandom().nextInt(1_000_000);
		return String.format("%06d", n);
	}

	private void clearAuthCode(HttpSession session) {
		session.removeAttribute(SESSION_AUTH_CODE);
		session.removeAttribute(SESSION_AUTH_EXP);
	}
}
