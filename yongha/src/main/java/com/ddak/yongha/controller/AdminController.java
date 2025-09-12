// src/main/java/…/admin/AdminController.java
package com.ddak.yongha.controller;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ddak.yongha.mapper.UsersMapper;
import com.ddak.yongha.mapper.UsersMapper2;
import com.ddak.yongha.service.BannerService;
import com.ddak.yongha.service.UsersService2;
import com.ddak.yongha.vo.BannerVO;
import com.ddak.yongha.vo.UsersVO;

@Controller
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private UsersMapper usersMapper;

	@Autowired
	private UsersMapper2 usersMapper2;

	@Autowired
	UsersService2 usersService2;

	@Autowired
	BannerService bannerService;

	@GetMapping
	public String index() {
		// /admin 으로 오면 기본 페이지로 보내기
		return "redirect:/admin/users";
	}

	@GetMapping("/users")
	public String adminUsers(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String grade, // "0|1|2" (부모/자녀/관리자)
			@RequestParam(required = false) String state, // "on|off"
			@RequestParam(required = false) String sortField, // user_no|user_id|...
			@RequestParam(required = false) String sortDir, // asc|desc
			Model model) {

		int offset = page * size;

		Map<String, Object> params = new HashMap<>();
		params.put("keyword", keyword);
		params.put("grade", grade);
		params.put("state", state);
		params.put("sortField", sortField);
		params.put("sortDir", sortDir);
		params.put("offset", offset);
		params.put("limit", size);

		int total = usersMapper.countUsers(params);
		List<UsersVO> users = usersMapper.getUserPage(params);

		int totalPages = (int) Math.ceil((double) total / size);
		int displayPage = page + 1;

		model.addAttribute("users", users);
		model.addAttribute("total", total);
		model.addAttribute("page", page);
		model.addAttribute("size", size);
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("displayPage", displayPage);

		// 필터/정렬 값 유지
		model.addAttribute("keyword", keyword == null ? "" : keyword);
		model.addAttribute("grade", grade == null ? "" : grade);
		model.addAttribute("state", state == null ? "" : state);
		model.addAttribute("sortField", sortField == null ? "" : sortField);
		model.addAttribute("sortDir", sortDir == null ? "" : sortDir);
		model.addAttribute("resultCount", total);

		// KPI
		int accountCount = usersMapper2.countAccounts();
		int parentCount = usersMapper2.countParents();
		int childCount = usersMapper2.countChildren();
		model.addAttribute("userCount", usersMapper2.countParents() + usersMapper2.countChildren());
		model.addAttribute("accountCount", accountCount);
		model.addAttribute("parentCount", parentCount);
		model.addAttribute("childCount", childCount);

		double percent = (total == 0) ? 0 : Math.round(accountCount * 100.0 / (parentCount + childCount) * 100) / 100.0;
		model.addAttribute("accountPercent", percent);

		return "admin/admin-users";
	}

	// 배너 업로드 폼 페이지
	@GetMapping("/banners")
	public String banners(Model model) {
		List<BannerVO> banners = bannerService.getAllBanners2();
		Date now = new Date();

		Map<Integer, String> bannerStatusMap = new HashMap<>();

		for (BannerVO b : banners) {
			if (now.before(b.getStartDate())) {
				bannerStatusMap.put(b.getBannerNo(), "게시 전");
			} else if (now.after(b.getEndDate())) {
				bannerStatusMap.put(b.getBannerNo(), "만료");
			} else {
				bannerStatusMap.put(b.getBannerNo(), "게시 중");
			}
		}

		model.addAttribute("banners", banners);
		model.addAttribute("bannerStatusMap", bannerStatusMap);
		return "admin/admin-banners"; // templates/admin/admin-banners.html
	}

	@GetMapping("/stats")
	public String stats(Model model, @RequestParam(required = false) String fromDate,
			@RequestParam(required = false) String toDate) {

		// 파라미터 맵
		Map<String, Object> params = new HashMap<>();
		params.put("fromDate", fromDate);
		params.put("toDate", toDate);

		// 부모/자녀 (기간 적용)
		Map<String, Integer> counts = usersService2.getChildParentCountsWithPeriod(params);
		int parents = counts.getOrDefault("parents", 0);
		int children = counts.getOrDefault("children", 0);
		model.addAttribute("parents", parents);
		model.addAttribute("children", children);

		// 총 가입 계정 수(전체 누적) + 지난달 말까지 누적 → 증가율
		LocalDate today = LocalDate.now();
		int totalUser = usersMapper2.countTotalUsersWithPeriod(null);
		int totalUserLastMonth = usersMapper2.countTotalUsersWithPeriod(today.getYear() + "-" + today.getMonthValue());
		double joinPercent = Math.round((double) (totalUser - totalUserLastMonth) / totalUserLastMonth * 1000) / 10.0;
		model.addAttribute("totalUser", totalUser);
		model.addAttribute("joinPercent", joinPercent);

		// 연령대 분포
		Map<String, Integer> ageGroups = usersService2.getChildAgeGroups(params);
		model.addAttribute("ageGroups", ageGroups);

		// 연령별 소비 평균
		Map<String, Integer> avgCons = usersService2.getAvgConsByAgeGroups(params);
		model.addAttribute("avgCons", avgCons);

		// 일 평균 이체 수 (기간 or 최근30일)
		double avgTransfer = usersMapper2.countAvgTransferWithPeriod(params);
		double rounded = Math.round(avgTransfer * 100.0) / 100.0;
		model.addAttribute("avgTransfer", rounded);

		// 등록된 목표 카테고리 분포
		Map<String, Integer> prefRate = usersService2.getPrefRate(params);
		model.addAttribute("prefRate", prefRate);

		// 목표 달성/미달성 개수 (기간 적용)
		Map<String, Integer> goalsCnt = usersService2.getGoalsCountsWithPeriod(params);
		int achieved = goalsCnt.getOrDefault("achieved", 0);
		int unachieved = goalsCnt.getOrDefault("unachieved", 0);
		model.addAttribute("achieved", achieved);
		model.addAttribute("unachieved", unachieved);

		// 목표 달성기간별 분포
		Map<String, Integer> achvPeriodRate = usersService2.getAchvPeriodRate(params);
		model.addAttribute("achvPeriodRate", achvPeriodRate);

		// 각 소셜 가입 유저 카운트
		// 전체(가입타입 무관)
		int totParents = usersMapper2.countParentsWithPeriod(params);
		int totChildren = usersMapper2.countChildrenWithPeriod(params);
		model.addAttribute("totParents", totParents);
		model.addAttribute("totChildren", totChildren);

		// 소셜/일반 묶어서 반복 처리
		List<String> signupTypes = List.of("kakao", "naver");

		for (String type : signupTypes) {
			Map<String, Object> p = new HashMap<>(params);
			p.put("signupType", type);

			int cntParents = usersMapper2.countParentsWithPeriod(p);
			int cntChildren = usersMapper2.countChildrenWithPeriod(p);

			//
			model.addAttribute("totParents_" + type, cntParents);
			model.addAttribute("totChildren_" + type, cntChildren);
		}

		// clear
		params.remove("signupType");

		// 화면에서 날짜 input 유지
		model.addAttribute("fromDate", fromDate);
		model.addAttribute("toDate", toDate);

		return "admin/admin-stats"; // templates/admin/admin-stats.html
	}
}
