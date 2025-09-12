package com.ddak.yongha.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.ddak.yongha.service.ChildConsumeService;
import com.ddak.yongha.service.ParentConsumeService;
import com.ddak.yongha.service.UsersService;
import com.ddak.yongha.vo.ConsumeVO;
import com.ddak.yongha.vo.TypeRatio;
import com.ddak.yongha.vo.UsersVO;
import com.ddak.yongha.vo.Window;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ParentConsumeController {
	@Autowired
	private UsersService usersService;

	@Autowired
	private ParentConsumeService parentConsumeService;
	
	@Autowired
	private ChildConsumeService consumeService;

	@GetMapping("/parent/consume/list")
	public String listPage(
			@SessionAttribute(value = "user_no", required = false) Integer parentNo,
			@RequestParam(value = "childNo", required = false) Integer childNo,
			@RequestParam(value = "fromDate", required = false) String fromDate,
			@RequestParam(value = "toDate", required = false) String toDate,
			@RequestParam(value = "consType", required = false) Integer consType,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size,
			Model model) {

		if (parentNo == null)
			throw new RuntimeException("로그인이 필요합니다.");

		// 자녀 목록
		List<UsersVO> children = usersService.getMyChildsInfo(parentNo);
		boolean hasChildren = (children != null && !children.isEmpty());
		model.addAttribute("hasChildren", hasChildren);

		// 선택된 자녀가 없으면 첫번째로 auto-select (자녀가 있을 때만)
		if (childNo == null && !children.isEmpty()) {
			childNo = children.get(0).getUser_no();
		}

		List<ConsumeVO> list = Collections.emptyList();
		int total = 0;
		long sumAmount = 0L;
		int totalPages = 0;

		if (childNo != null) {
			// 권한 체크
			parentConsumeService.assertChildOfParent(parentNo, childNo);

			// 자녀페이지 기존 목록 쿼리 그대로 호출(userNo=childNo) — 페이징·필터 파라미터 동일
			list = parentConsumeService.getConsumeListByChild(childNo, fromDate, toDate, consType, keyword, page, size);
			total = parentConsumeService.countConsumeListByChild(childNo, fromDate, toDate, consType, keyword);
			sumAmount = parentConsumeService.sumConsumeAmountByChild(childNo, fromDate, toDate, consType, keyword);

			totalPages = (int) Math.ceil((double) total / Math.max(1, size));
			
			// 오늘 지출내역
			TypeRatio day = consumeService.getTypeRatio(childNo, Window.DAY_1);
			model.addAttribute("todayList", day.getConsumes());

			// 최근 7일 타입별 비율
			TypeRatio week = consumeService.getTypeRatio(childNo, Window.DAY_7);
			model.addAttribute("ratioByType7d", week.getRatioByType());

		}

		model.addAttribute("children", children);
		model.addAttribute("childNo", childNo);

		model.addAttribute("fromDate", fromDate);
		model.addAttribute("toDate", toDate);
		model.addAttribute("consType", consType);
		model.addAttribute("keyword", keyword);

		model.addAttribute("list", list);
		model.addAttribute("total", total);
		model.addAttribute("sumAmount", sumAmount);
		model.addAttribute("page", page);
		model.addAttribute("size", size);
		model.addAttribute("totalPages", totalPages);
		model.addAttribute("displayPage", totalPages == 0 ? 0 : page + 1);
		
		
		return "parent/child_consume_list";
	}

}
