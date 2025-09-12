package com.ddak.yongha.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.ddak.yongha.service.BannerService;
import com.ddak.yongha.service.UsersService;
import com.ddak.yongha.vo.BannerVO;
import com.ddak.yongha.vo.ChildInfoVO;
import com.ddak.yongha.vo.UsersVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {

	// private final LoginController loginController;

	@Autowired
	private UsersService usersService;

	@Autowired
	private BannerService bannerService;

	// MainController(LoginController loginController) {
	// this.loginController = loginController;
	// }

	@GetMapping("/")
	public String main(@SessionAttribute(name = "user_no", required = false) Integer userNo,
			@SessionAttribute(name = "user_type", required = false) Integer userType, HttpSession session,
			Model model) {

		if (userNo == null) {
			session.removeAttribute("user_type"); // 과거 값 제거

			List<Map<String, Object>> banners = showBanner();

			model.addAttribute("bannerList", banners);

			return "mainpage";
		}

		if (userType == null) {
			userType = usersService.getUserType(userNo);
			session.setAttribute("user_type", userType);
		}

		return switch (userType != null ? userType : -1) {
		case 0 -> "redirect:/main_parent_page";
		case 1 -> "redirect:/main_child_page";
		case 2 -> "redirect:/admin";
		default -> "mainpage";
		};
	}

	@GetMapping("/main_child_page")
	public String main_child_page(@SessionAttribute(name = "user_no", required = false) Integer user_no, Model model,
			HttpSession session) {

		if (user_no == null) {
			session.invalidate();
			return "redirect:/?relogin";
		}

		ChildInfoVO childInfo = usersService.getChildInfo(user_no);

		if (childInfo == null) {
			// 세션은 있으나 DB상 정보가 없는 비정상 상태 → 세션 정리하고 루트로
			session.invalidate();
			return "redirect:/?relogin";
		}

		childInfo.calculateDday();
		childInfo.calculateProgress();

		model.addAttribute("userInfo", childInfo);

		List<Map<String, Object>> banners = showBanner();

		model.addAttribute("bannerList", banners);
		return "child/main_child_page";
	}

	@GetMapping("/main_parent_page")
	public String main_parent_page(@SessionAttribute(name = "user_no", required = false) Integer user_no, Model model,
			HttpSession session) {

		if (user_no == null) {
			// 세션은 있으나 DB상 정보가 없는 비정상 상태 → 세션 정리하고 루트로
			session.invalidate();
			return "redirect:/?relogin";
		}

		UsersVO uvo = usersService.getUserInfoByNo(user_no);
		model.addAttribute("userInfo", uvo);
		List<Map<String, Object>> banners = showBanner();

		model.addAttribute("bannerList", banners);

		return "parent/main_parent_page";
	}

	@GetMapping("/service_explanation")
	public String serviceExplanation() {
		return "/non_main/service_explanation";
	}

	@GetMapping("/company_profile")
	public String company_profile() {
		return "non_main/company_profile";
	}

	@GetMapping("/signup")
	public String goSignupForm() {
		return "signup";
	}

	@GetMapping("/login")
	public String goLoginPage(
			@RequestParam(value = "redirectUrl", required = false) String redirectUrl,
			HttpServletRequest request,
			Model model) {

		String safe = toSafeRedirect(redirectUrl);
		model.addAttribute("redirectUrl", safe); // null이면 hidden에 빈값
		return "login";
	}

	// 네이버 로그인 callback URL
	@GetMapping("/naver-login-callback")
	public String naverLoginCallbackPage() {
		return "naver-login-callback"; // templates/naver-login-callback.html
	}

	@GetMapping("/learning_outcomes")
	public String learning_outcomes() {
		return "/non_main/learning_outcomes";
	}

	@GetMapping("/faq")
	public String faq() {
		return "/non_main/faq";
	}

	private String toSafeRedirect(String url) {
		if (url == null || url.isBlank())
			return null;
		// 내부 경로만 허용: 절대 URL, 프로토콜-relative(//), CR/LF 등 차단
		if (!url.startsWith("/") || url.startsWith("//"))
			return null;
		// 필요시 화이트리스트 추가: if (!url.matches("^/(consume|parent_page|...)")) return null;
		return url;
	}

	public List<Map<String, Object>> showBanner() {
		List<BannerVO> allBanners = bannerService.getAllBanners();
		LocalDateTime now = LocalDateTime.now();

		// 유효한 배너만 필터링
		List<BannerVO> validBanners = allBanners.stream().filter(b -> {
			LocalDateTime start = b.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			LocalDateTime end = b.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			return !now.isBefore(start) && !now.isAfter(end);
		}).collect(Collectors.toList());

		// 디폴트 배너 1~9
		List<Map<String, Object>> banners = new ArrayList<>();
		for (int i = 1; i <= 9; i++) {
			Map<String, Object> banner = new HashMap<>();
			switch (i) {
			case 1:
				banner.put("bannerIndex", i);
				banner.put("imgPath", "/img/event/event1.png");
				banner.put("title", "📢 여름 한정 이벤트!");
				banner.put("description", "시원한 여름, 특별한 혜택을 만나보세요!");
				banner.put("button", "자세히보기");
				break;
			case 2:
				banner.put("bannerIndex", i);
				banner.put("imgPath", "/img/event/event2.png");
				banner.put("title", "👩‍👧‍👦 가족 금융 퀴즈");
				banner.put("description", "함께 참여하고 상품 받아가세요!");
				banner.put("button", "참여하기");
				break;
			case 3:
				banner.put("bannerIndex", i);
				banner.put("imgPath", "/img/event/event3.png");
				banner.put("title", "📈 자녀 금융 성향 테스트");
				banner.put("description", "우리 아이는 어떤 금융 유형일까요?");
				banner.put("button", "테스트 하러 가기");
				break;
			case 4:
				banner.put("bannerIndex", i);
				banner.put("imgPath", "/img/event/event4.png");
				banner.put("title", "💡 용돈관리 팁 공개!");
				banner.put("description", "부모님을 위한 스마트한 팁을 확인하세요.");
				banner.put("button", "팁 보기");
				break;
			case 5:
				banner.put("bannerIndex", i);
				banner.put("imgPath", "/img/event/event5.png");
				banner.put("title", "🎁 신규 가입 이벤트");
				banner.put("description", "회원가입만 해도 선물이 팡팡!");
				banner.put("button", "이벤트 확인");
				break;
			case 6:
				banner.put("bannerIndex", i);
				banner.put("imgPath", "/img/event/event6.png");
				banner.put("title", "📚 부모 금융 교육 강의");
				banner.put("description", "전문가와 함께 배우는 금융 교육");
				banner.put("button", "강의 보기");
				break;
			case 7:
				banner.put("bannerIndex", i);
				banner.put("imgPath", "/img/event/event7.png");
				banner.put("title", "📝 자녀 소비 습관 기록장");
				banner.put("description", "기록하고 보상받는 습관 만들기");
				banner.put("button", "기록장 가기");
				break;
			case 8:
				banner.put("bannerIndex", i);
				banner.put("imgPath", "/img/event/event8.png");
				banner.put("title", "🏆 베스트 가족 선정 이벤트");
				banner.put("description", "활동 많은 가족에게 특별한 선물!");
				banner.put("button", "이벤트 참여");
				break;
			case 9:
				banner.put("bannerIndex", i);
				banner.put("imgPath", "/img/event/event9.png");
				banner.put("title", "🌟 Yongha 인기 기능 모음");
				banner.put("description", "지금 가장 핫한 기능들을 만나보세요!");
				banner.put("button", "기능 보기");
				break;
			}
			banners.add(banner);
		}

		// DB 배너 덮어쓰기
		validBanners.forEach(dbBanner -> {
			int idx = dbBanner.getBannerIndex() - 1;
			if (idx >= 0 && idx < banners.size()) {
				banners.get(idx).put("imgPath", dbBanner.getImgPath());
			}
		});

		return banners;
	}

}