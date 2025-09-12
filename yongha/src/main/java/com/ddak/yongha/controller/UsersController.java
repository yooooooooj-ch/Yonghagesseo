package com.ddak.yongha.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.ddak.yongha.mapper.UsersMapper;
import com.ddak.yongha.service.ParentMailService;
import com.ddak.yongha.service.UsersService;
import com.ddak.yongha.vo.UsersVO;

@Controller
@RequestMapping("/users")
public class UsersController {

	@Autowired
	UsersMapper um;

	@Autowired
	UsersService usersService;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	private ParentMailService parentMailService;

	// 회원가입 응답결과 json으로 반환
	@PostMapping("/signup")
	@ResponseBody
	public Map<String, Object> signup(UsersVO user) {
		Map<String, Object> result = new HashMap<>();

		// password가 null값으로 들어온 경우 == 소셜로그인 => 랜덤비밀번호값생성
		if (user.getPassword() == null || user.getPassword().isBlank()) {
			String rawTemp = "SOCIAL-" + UUID.randomUUID(); // 임시 비번(로그인엔 사용 안 함)
			user.setPassword(passwordEncoder.encode(rawTemp));
		} else { // 일반 회원가입의 경우, 암호화
			user.setPassword(passwordEncoder.encode(user.getPassword()));
		}

		try {
			um.insertUser(user);
			result.put("success", true);
		} catch (Exception e) {
			result.put("success", false);
			result.put("msg", e.getMessage());
		}
		return result;
	}

	@GetMapping("/check-id-duplicate")
	@ResponseBody
	public Map<String, Boolean> checkIdDuplicate(@RequestParam("user_id") String user_id) {
		UsersVO user = um.findByUserId(user_id);
		Map<String, Boolean> result = new HashMap<>();
		result.put("duplicate", user != null);
		return result;
	}

	@GetMapping("/check-email-duplicate")
	@ResponseBody
	public Map<String, Boolean> checkEmailDuplicate(@RequestParam("email") String email) {
		UsersVO user = um.findByUserEmail(email);
		Map<String, Boolean> result = new HashMap<>();
		result.put("duplicate", user != null);
		return result;
	}

	@PostMapping("/changeProfile")
	public String changeProfile(@RequestParam("profile") int profile, @SessionAttribute("user_no") int user_no) {
		System.out.println("user_no=" + user_no + ", profile=" + profile);
		usersService.updateProfile(user_no, profile);
		return "redirect:/"; // 업데이트 후 이동할 페이지
	}

	// 회원 정보 수정 페이지 로드
	@GetMapping("/my-info")
	public String userProfile(@SessionAttribute(name = "user_no", required = false) Integer userNo, Model model) {

		boolean loggedIn = (userNo != null);
		model.addAttribute("loggedIn", loggedIn);
		UsersVO user = usersService.findByUserNo(userNo);

		if (loggedIn) {
			if (user == null) {
				model.addAttribute("loggedIn", false);
			} else {
				Integer mailCycle = parentMailService.getMailCycle(userNo);
				user.setMail_cycle(mailCycle);
				model.addAttribute("user", user);
			}
		}

		Integer userType = user.getUser_type();
		List<UsersVO> familyMembers = new ArrayList<>();

		if (userType != null && userType == 0) { // 부모
			// 부모 사용자: 내 자녀들을 familyMembers에 담기
			List<UsersVO> children = usersService.getMyChildsInfo(userNo); // 내 자녀 조회
			if (children != null)
				familyMembers.addAll(children);

		} else if (userType != null && userType == 1) { // 자녀
			// 자녀 사용자: 부모님 + 형제자매를 familyMembers에 담기 (기존 로직 유지)
			List<UsersVO> parents = usersService.getMyParentsInfo(userNo); // 부모 조회
			List<UsersVO> siblings = usersService.getMySiblingsInfo(userNo); // 형제자매 조회
			if (parents != null)
				familyMembers.addAll(parents);
			if (siblings != null)
				familyMembers.addAll(siblings);
		}

		model.addAttribute("familyMembers", familyMembers);
		return "my-info"; // 항상 같은 뷰 리턴
	}

	// 회원 정보 수정
	@PostMapping("/edit-my-info")
	@ResponseBody
	public Map<String, Object> editUserInfo(@SessionAttribute(name = "user_no", required = false) Integer userNo,
			@RequestBody UsersVO user) {
		Map<String, Object> result = new HashMap<>();

		if (userNo == null) {
			result.put("success", false);
			result.put("msg", "로그인이 필요합니다.");
			return result;
		}

		// 수정 불가 필드 보호: DB의 기존값으로 덮어서 조작 방지
		UsersVO current = usersService.findByUserNo(userNo);
		if (current == null) {
			result.put("success", false);
			result.put("msg", "회원 정보를 찾을 수 없습니다.");
			return result;
		}

		// 세션의 user_no 강제 주입 (요청 바디의 user_no는 무시)
		user.setUser_no(userNo);

		// 화면에 보이지만 수정불가인 항목은 서버에서 기존값으로 고정
		user.setUser_id(current.getUser_id());
		user.setUser_name(current.getUser_name());
		user.setBirthday(current.getBirthday()); // LocalDate/Date 그대로

		try {
			// 전체 UPDATE
			um.updateUser(user); // 매퍼 xml의 <update id="updateByUserNo">와 매칭
			parentMailService.saveMailCycle(userNo, user.getMail_cycle());
			result.put("success", true);
		} catch (Exception e) {
			result.put("success", false);
			result.put("msg", e.getMessage());
		}
		return result;
	}
}
