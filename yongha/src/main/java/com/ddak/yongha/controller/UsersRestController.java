package com.ddak.yongha.controller;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.util.UriComponentsBuilder;

import com.ddak.yongha.security.JwtInviteUtil;
import com.ddak.yongha.service.EmailService;
import com.ddak.yongha.service.UsersService;
import com.ddak.yongha.vo.UsersVO;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;

@RestController
@RequestMapping("/rest/users")
public class UsersRestController {

	@Autowired
	private EmailService emailService;

	@Autowired
	private UsersService usersService;

	@Autowired
	private JwtInviteUtil jwtInviteUtil;

	@Value("${app.base-url}")
	private String baseUrl;

	@PostMapping("/invite")
	public ResponseEntity<?> inviteFamily(@RequestBody Map<String, String> body,
			@SessionAttribute(value = "user_no", required = false) Integer user_no) {

		if (user_no == null)
			return badRequest("로그인이 필요한 서비스입니다.");

		String child_email = body.get("child_email");
		if (child_email == null || child_email.trim().isEmpty())
			return badRequest("초대할 아기용의 이메일주소를 입력해주세요.");

		UsersVO parent = usersService.getUserInfoByNo(user_no);
		UsersVO child = usersService.findByUserEmail(child_email);
		if (child == null || child.getUser_no() == 0)
			return badRequest("회원정보가 존재하지 않습니다.");
		if (usersService.isFamilyAlreadyRegistered(user_no, child.getUser_no()))
			return badRequest("이미 등록된 가족 관계입니다.");
		if (child.getUser_type() != 1)
			return badRequest("자녀로 등록된 사용자가 아닙니다.");

		String token = jwtInviteUtil.create(user_no, child.getUser_no());
		String acceptLink = UriComponentsBuilder.fromUri(URI.create(baseUrl))
				.path("/join-family")
				.queryParam("token", token)
				.build()
				.toUriString();

		// 메일 비동기 전송 (컨트롤러는 기다리지 않음)
		emailService.sendFamilyInviteMail(child_email, parent.getUser_name(), acceptLink);

		Map<String, Object> result = new HashMap<>();
		result.put("message", "아기용 초대 완료");
		result.put("token", token);
		// 필요시 202 Accepted로 의미를 명확히 할 수도 있음: return
		// ResponseEntity.accepted().body(result);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/family")
	public ResponseEntity<?> insertFamily(@RequestBody Map<String, String> body,
			@SessionAttribute(value = "user_no", required = false) Integer user_no) {

		if (user_no == null)
			return badRequest("로그인이 필요한 서비스입니다.");

		String token = body.get("token");
		if (token == null)
			return badRequest("유효하지 않은 요청입니다.");

		try {
			Jws<Claims> jws = jwtInviteUtil.parse(token);
			int parent_no = ((Number) jws.getBody().get("p")).intValue();
			int child_no = ((Number) jws.getBody().get("c")).intValue();

			if (user_no.intValue() != child_no)
				return badRequest("유효하지 않은 요청입니다.");
			if (usersService.isFamilyAlreadyRegistered(parent_no, child_no))
				return badRequest("이미 등록된 가족 관계입니다.");

			usersService.insertFamily(parent_no, child_no);

			Map<String, Object> result = new HashMap<>();
			result.put("message", "가족 참여 완료했습니다.");
			return ResponseEntity.ok(result);
		} catch (JwtException | IllegalArgumentException e) {
			return badRequest("유효하지 않은 초대입니다.");
		}
	}

	@DeleteMapping("/family")
	public ResponseEntity<?> deleteFamily(@RequestBody Map<String, String> body,
			@SessionAttribute(value = "user_no", required = false) Integer user_no) {

		int child_no = Integer.parseInt(body.get("childNo"));
		usersService.deleteFamily(user_no, child_no);

		Map<String, Object> result = new HashMap<>();
		result.put("message", "가족 구성원에서 삭제했습니다.");
		return ResponseEntity.ok(result);
	}

	private ResponseEntity<Map<String, Object>> badRequest(String message) {
		Map<String, Object> error = new HashMap<>();
		error.put("message", message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@DeleteMapping("/admin")
	public ResponseEntity<?> deleteUsers(@RequestBody Map<String, List<Integer>> body) {
		List<Integer> userNo = body.get("userNo");
		if (userNo == null || userNo.isEmpty())
			return badRequest("선택된 유저가 없습니다.");
		usersService.deleteUsers(userNo);
		Map<String, Object> result = new HashMap<>();
		result.put("message", "삭제되었습니다");
		return ResponseEntity.ok(result);
	}
}
