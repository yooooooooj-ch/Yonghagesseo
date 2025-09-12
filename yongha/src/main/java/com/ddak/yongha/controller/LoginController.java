package com.ddak.yongha.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.ddak.yongha.mapper.UsersMapper;
import com.ddak.yongha.security.JwtUtil;
import com.ddak.yongha.service.UsersService;
import com.ddak.yongha.vo.UsersVO;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(value = "/api/user")
@RequiredArgsConstructor
public class LoginController {

	private final UsersService usersService;

	private final UsersMapper usersMapper;

	private final PasswordEncoder passwordEncoder;
	
	private final JwtUtil jwtUtil;	  

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	// 소셜 로그인
	@PostMapping("/social-login")
	public ResponseEntity<Map<String, Object>> socialLogin(@RequestBody UsersVO req, HttpSession session,
			HttpServletResponse response) {
		try {
			// NEW: 조회/응답 모두에서 동일하게 쓸 user_id 정규화
			final String clampedUserId = SocialIdUtil.clampUserId20(req.getUser_id());

			// 카카오 고유 아이디로 DB조회
			UsersVO user = usersMapper.findByUserId(clampedUserId);     

			if (user == null) {
				// 미가입 - 간편가입 안내
				Map<String, Object> result = new HashMap<>();
				result.put("status", "NOT_REGISTERED"); // 유지
				result.put("msg", "처음 오셨네요! 간편가입을 완료해 주세요."); // NEW: SweetAlert 메시지

				// 회원가입 폼에 자동 입력될 카카오 소셜 회원 데이터 (clamp 적용)
				Map<String, Object> socialData = new HashMap<>();
				socialData.put("user_id", clampedUserId); // CHANGED: clamp 적용
				socialData.put("user_name", nullToEmpty(req.getUser_name()));
				socialData.put("birthday", req.getBirthday() != null ? req.getBirthday().toString() : "");
				socialData.put("email", nullToEmpty(req.getEmail()));
				socialData.put("tel", nullToEmpty(req.getTel()));

				result.put("socialData", socialData);
				return ResponseEntity.ok(result);
			} else {
				// JWT 토큰 발급
				String token = jwtUtil.createToken(user.getUser_no(), user.getUser_id());
				session.setAttribute("user_no", user.getUser_no());
				// === Refresh Token ===
				// 1) refresh 토큰 발급
				String refresh = jwtUtil.createRefreshToken(user.getUser_no(), user.getUser_id());

				// 2) HttpOnly+Secure 쿠키로 설정
				ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refresh).httpOnly(true)
						.secure(false) // 운영(HTTPS)에서는 true. 로컬 개발 http면 임시로 false 가능
						.sameSite("Strict") // 또는 "Lax" (크로스사이트 필요시 조정)
						.path("/") // 전체 경로에서 전송
				        .maxAge(7 * 24 * 60 * 60) // JwtUtil의 refresh 만료와 맞추기 (초 단위)
//						.maxAge(60) // JwtUtil의 refresh 만료와 맞추기 (60초) (for test)
						.build();

				// 3) 헤더로 추가
				response.addHeader("Set-Cookie", refreshCookie.toString());
				// =========

				Map<String, Object> result = new HashMap<>();
				result.put("status", "LOGIN"); // 유지
				result.put("msg", "로그인 성공! 메인으로 이동합니다."); // NEW: SweetAlert 메시지
				result.put("token", token);

				// (선택) 프론트 편의용 사용자 요약
				Map<String, Object> userBrief = new HashMap<>();
				userBrief.put("user_no", user.getUser_no());
				userBrief.put("user_id", user.getUser_id());
				userBrief.put("user_name", nullToEmpty(user.getUser_name()));
				result.put("user", userBrief);

				return ResponseEntity.ok(result);
			}
		} catch (IllegalArgumentException e) {
			// NEW: 유효성 실패 → 400
			return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "msg", e.getMessage()));
		} catch (Exception e) {
			// NEW: 서버 오류 → 500
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("status", "ERROR", "msg", "소셜 로그인 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));
		}
	}

	// 일반 로그인
	@GetMapping("/login") // 변경 : Post -> Get *spring security 의존성이 있으면 자동 로그인 창으로 넘어감으로 제외할 것.
	public ResponseEntity<String> login(@RequestParam String user_id, @RequestParam String password,
			HttpSession session, HttpServletResponse response) {

		UsersVO user = usersService.authenticate(user_id, password);
		if (user != null) {
			// JWT 토큰 생성
			String token = jwtUtil.createToken(user.getUser_no(), user.getUser_id());
			session.setAttribute("user_no", user.getUser_no());
			// === Refresh Token ===
			// 1) refresh 토큰 발급
			String refresh = jwtUtil.createRefreshToken(user.getUser_no(), user.getUser_id());

			// 2) HttpOnly+Secure 쿠키로 설정
			ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refresh).httpOnly(true).secure(false) 
					.sameSite("Strict") // 또는 "Lax" (크로스사이트 필요시 조정)
					.path("/") // 전체 경로에서 전송
					.maxAge(7 * 24 * 60 * 60) // JwtUtil의 refresh 만료와 맞추기 (초 단위)
//					.maxAge(60) // JwtUtil의 refresh 만료와 맞추기 (60초) (for test)
					.build();

			// 3) 헤더로 추가
			response.addHeader("Set-Cookie", refreshCookie.toString());
			// =========
			return ResponseEntity.ok(token);
		} else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("error"); // 401 error
		}
	}

	// 일반 로그인 검증 완료 메세지 result 에서 출력
	@GetMapping("/secure")
	public String kajaSecure() {
		return "토큰 인증 했다는~";
	}

	/*
	 * JWT에서 user_no만 추출 후 프론트엔드로 반환하는 API
	 */
	@GetMapping("/me")
	public ResponseEntity<Map<String, Object>> getMyInfo(
			@RequestHeader(value = "Authorization", required = false) String token) {
		Map<String, Object> result = new HashMap<>();

		// 토큰(String)이 없으면 비로그인 처리
		if (token == null || token.trim().isEmpty()) {
			result.put("error", "로그인 필요");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
		}

		try {
			Claims claims = jwtUtil.getClaims(token);
			int user_no = Integer.valueOf(claims.getSubject());
			result.put("user_no", user_no);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			// 토큰 파싱 실패, 만료 등 모두 비로그인 처리
			result.put("error", "유효하지 않은 토큰");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
		}
	}

	/*
	 * 통합 로그아웃
	 */
	@PostMapping("/logout")
	@ResponseBody
	public String logout(HttpSession session, HttpServletResponse response) {
		session.invalidate(); // 서버 HttpSession 무효화

		// refresh 쿠키 제거 - HttpOnly 쿠키는 백엔드에서 무효화. setCookie 메서드 X
		ResponseCookie expired = ResponseCookie.from("refresh_token", "").httpOnly(true).secure(false)
				.sameSite("Strict") // 개발중 secure(false)
				.path("/").maxAge(0) // 즉시 만료
				.build();
		response.addHeader("Set-Cookie", expired.toString());

		return "OK";
	}

	/*
	 * 소셜로그인 아이디 글자제한
	 */
	public final class SocialIdUtil {
		private SocialIdUtil() {
		}

		public static String clampUserId20(String raw) {
			if (raw == null)
				return "";
			String s = raw.trim();
			return s.length() > 20 ? s.substring(s.length() - 20) : s; // 뒤 20자
		}
	}

	// 아이디 찾기
	@GetMapping("/find-id")
	public Map<String, Object> findIdByEmail(@RequestParam("email") String email) {
		if (email == null || email.trim().isEmpty()) {
			return Map.of("success", false, "msg", "이메일을 입력해 주세요.");
		}

		final String normalized = email.trim().toLowerCase();

		// 아이디 조회
		String id = usersMapper.findByUserEmail(normalized).getUser_id();

		if (id == null || id.isEmpty()) {
			return Map.of("success", false, "msg", "해당 이메일의 아이디를 찾을 수 없습니다.");
		}

		// 아이디를 그대로 반환후 프론트에서 출력
		return Map.of("success", true, "user_id", id);
	}

	// 비밀번호 재설정
	@PostMapping("/reset-password")
	public Map<String, Object> resetPassword(@RequestBody Map<String, String> req, HttpSession session) {
		String userId = req.get("user_id");
		String email = req.get("email");
		String rawPw = req.get("new_password");
		UsersVO userFindById = usersMapper.findByUserId(userId);
		UsersVO userFindByEmail = usersMapper.findByUserEmail(email);

		// 입력한 아이디와 이메일이 회원DB에 존재하며 매칭되는지 확인
		if (userFindById == null || userFindByEmail == null
				|| !Objects.equals(userFindById.getUser_no(), userFindByEmail.getUser_no())) {
			return Map.of("success", false, "msg", "회원 정보가 일치하지 않습니다.");
		}

		// pw업데이트
		String encoded = passwordEncoder.encode(rawPw);
		int updated = usersMapper.changePassword(userFindById.getUser_no(), encoded);

		if (updated > 0) {
			return Map.of("success", true);
		}
		return Map.of("success", false, "msg", "비밀번호 변경에 실패했습니다.");
	}

	// 아이디와 이메일로 유저 조회
	@PostMapping("/verify-id-email")
	public Map<String, Object> verifyIdEmail(@RequestBody Map<String, String> req) {
		String userId = (req.get("user_id") == null) ? "" : req.get("user_id").trim();
		String email = (req.get("email") == null) ? "" : req.get("email").trim();

		if (userId.isEmpty() || email.isEmpty()) {
			return Map.of("success", false, "msg", "아이디와 이메일을 입력해 주세요.");
		}

		int exists = usersMapper.existsByIdAndEmail(userId, email.toLowerCase());
		if (exists > 0) {
			return Map.of("success", true);
		}
		return Map.of("success", false, "msg", "회원 정보가 일치하지 않습니다.");
	}

	/*
	 * 리프레쉬 토큰 검증
	 */
	@PostMapping("/refresh")
	public ResponseEntity<?> refreshAccessToken(HttpServletRequest request, HttpServletResponse response,
			HttpSession session) {
		Integer userNo = (Integer) session.getAttribute("user_no");
		if (userNo == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("세션 만료");
		}

		String refreshToken = null;
		if (request.getCookies() != null) {
			for (Cookie c : request.getCookies()) {
				if ("refresh_token".equals(c.getName())) {
					refreshToken = c.getValue();
					break;
				}
			}
		}
		if (refreshToken == null) {
			// 세션 무효화 + 쿠키 삭제
			session.invalidate();
			ResponseCookie expired = ResponseCookie.from("refresh_token", "").httpOnly(true).secure(false)
					.sameSite("Strict").path("/").maxAge(0).build();
			response.addHeader("Set-Cookie", expired.toString());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("리프레시 토큰 없음");
		}

		try {
			Claims claims = jwtUtil.getClaims(refreshToken);
			if (!String.valueOf(userNo).equals(claims.getSubject())) {
				// 세션 무효화 + 쿠키 삭제
				session.invalidate();
				ResponseCookie expired = ResponseCookie.from("refresh_token", "").httpOnly(true).secure(false)
						.sameSite("Strict").path("/").maxAge(0).build();
				response.addHeader("Set-Cookie", expired.toString());
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰/세션 불일치");
			}

			String userId = (String) session.getAttribute("user_id"); 
			String newAccess = jwtUtil.createToken(userNo, userId); // 토큰 재발급
			return ResponseEntity.ok(newAccess);

		} catch (ExpiredJwtException e) {
			// 세션 무효화 + 쿠키 삭제
			session.invalidate();
			ResponseCookie expired = ResponseCookie.from("refresh_token", "").httpOnly(true).secure(false)
					.sameSite("Strict").path("/").maxAge(0).build();
			response.addHeader("Set-Cookie", expired.toString());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("리프레시 만료");
		} catch (Exception e) {
			// 세션 무효화 + 쿠키 삭제
			session.invalidate();
			ResponseCookie expired = ResponseCookie.from("refresh_token", "").httpOnly(true).secure(false)
					.sameSite("Strict").path("/").maxAge(0).build();
			response.addHeader("Set-Cookie", expired.toString());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("리프레시 검증 실패");
		}
	}

}
