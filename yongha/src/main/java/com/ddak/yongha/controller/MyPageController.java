package com.ddak.yongha.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.util.UriComponentsBuilder;

import com.ddak.yongha.security.JwtInviteUtil;
import com.ddak.yongha.service.ChildConsumeService;
import com.ddak.yongha.service.EmailService;
import com.ddak.yongha.service.UsersService;
import com.ddak.yongha.vo.ChildInfoVO;
import com.ddak.yongha.vo.ConsumeVO;
import com.ddak.yongha.vo.UsersVO;
import com.ddak.yongha.vo.Window;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpSession;

@Controller
public class MyPageController {

	@Autowired
	private UsersService usersService;

	@Autowired
	private JwtInviteUtil jwtInviteUtil;

	@Autowired
	private EmailService emailService;

	@Autowired
	private ChildConsumeService childConsumeService;

	@Value("${app.base-url}")
	private String baseUrl;

	// 부모 마이페이지
	@GetMapping("/parent_page")
	public String parent_page(Model model, HttpSession session,
			@SessionAttribute(name = "user_no", required = false) Integer user_no) {

		// 비로그인 -> 로그인 페이지
		if (user_no == null) {
			return "redirect:/login";
		}

		model.addAttribute("userInfo", usersService.getUserInfoByNo(user_no));

		List<UsersVO> childsInfo = usersService.getMyChildsInfo(user_no);
		for (UsersVO child : childsInfo) {
			child.calculateNextTransDateAndDday();
		}

		model.addAttribute("childsInfo", childsInfo);

		return "parent/parent_page";
		// return "account_register";
	}

	@GetMapping("/invite_family")
	public String inviteFamily() {

		return "parent/invite_family";
	}

	@GetMapping("/join-family")
	public String joinFamily(@RequestParam("token") String token, Model model,
			@SessionAttribute(name = "user_no", required = false) Integer user_no) {
		if (user_no == null)
			return "redirect:/login?redirectUrl=/join-family?token=" + token;

		try {
			Jws<Claims> jws = jwtInviteUtil.parse(token);
			int parent_no = ((Number) jws.getBody().get("p")).intValue();
			model.addAttribute("parent_name", usersService.getUserInfoByNo(parent_no).getUser_name());
			model.addAttribute("parent_profile", usersService.getUserInfoByNo(parent_no).getProfile());
			model.addAttribute("token", token);
		} catch (JwtException e) {
			return "redirect:/";
		}

		return "join-family";
	}

	@GetMapping("/invite_family/qr")
	public String inviteFamilyQr(@RequestParam("token") String token, Model model) {
		String link = UriComponentsBuilder.fromUri(URI.create(baseUrl))
				.path("/join-family")
				.queryParam("token", token)
				.build()
				.toUriString();
		try {
			String qr = emailService.createQrDataUrl(link);
			model.addAttribute("qr", qr);
		} catch (Exception e) {
			model.addAttribute("qr", "");
		}
		model.addAttribute("link", link);
		return "parent/invite_qr";
	}

	@GetMapping("/transferAllowance")
	public String transferAllowance(@RequestParam(value = "child_no", required = false) Integer child_no,
			@SessionAttribute(value = "user_no", required = false) Integer user_no, Model model) {

		if (user_no == null) {
			return "redirect:/login";
		}

		model.addAttribute("userInfo", usersService.getUserInfoByNo(user_no));

		List<UsersVO> childsInfo = usersService.getMyChildsInfo(user_no);
		for (UsersVO child : childsInfo) {
			child.calculateNextTransDateAndDday();
		}

		model.addAttribute("childsInfo", childsInfo);

		return "parent/transfer_allowance";
	}

	// 자식 마이페이지
	@GetMapping("/child_page")
	public String child_page(Model model, HttpSession session,
			@SessionAttribute(name = "user_no", required = false) Integer user_no) {

		if (user_no == null) {
			return "redirect:/login";
		}

		ChildInfoVO childInfo = usersService.getChildInfo(user_no);
		childInfo.calculateDday();
		childInfo.calculateProgress();
		model.addAttribute("userInfo", childInfo);

		try {
			LocalDate today = LocalDate.now();
			LocalDate weekAgo = today.minusDays(6); // 오늘 포함 7일

			List<ConsumeVO> lastWeekConsumes = childConsumeService.getTypeRatio(user_no, Window.DAY_7).getConsumes();

			// 날짜별 Map으로 변환 (비어있으면 0으로 초기화)
			Map<LocalDate, Long> dailyMap = new LinkedHashMap<>();
			for (int i = 0; i <= 6; i++) {
				dailyMap.put(weekAgo.plusDays(i), 0L);
			}
			for (ConsumeVO vo : lastWeekConsumes) {
				LocalDate date = vo.getCons_date().toLocalDate();
				dailyMap.put(date, dailyMap.getOrDefault(date, 0L) + vo.getAmount());
			}

			StringBuilder sb = new StringBuilder();
			for (Map.Entry<LocalDate, Long> entry : dailyMap.entrySet()) {
				sb.append(entry.getKey()).append(",").append(entry.getValue()).append(";");
			}
			if (sb.length() > 0)
				sb.setLength(sb.length() - 1);

			Path scriptPath = Path.of("src/scripts/generate_graph3.py").toAbsolutePath();
			List<String> command = List.of("python", scriptPath.toString(), sb.toString());

			ProcessBuilder pb = new ProcessBuilder(command);
			pb.redirectErrorStream(true);
			Process process = pb.start();

			StringBuilder output = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line);
				}
			}

			int exitCode = process.waitFor();
			if (exitCode != 0 || output.toString().trim().isEmpty()) {
				model.addAttribute("error", "Python script 실행 실패 또는 출력 없음");
				return "error";
			}

			model.addAttribute("graphData", "data:image/png;base64," + output.toString().trim());

		} catch (Exception e) {
			model.addAttribute("error", e.getMessage());
			return "error";
		}

		return "child/child_page";
	}

}
