package com.ddak.yongha.controller;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ddak.yongha.mapper.ConsumeMapper;
import com.ddak.yongha.service.ChildConsumeService;
import com.ddak.yongha.service.UsersService;
import com.ddak.yongha.vo.ChildInfoVO;
import com.ddak.yongha.vo.ConsumeVO;
import com.ddak.yongha.vo.TypeRatio;
import com.ddak.yongha.vo.Window;

@Controller
@RequestMapping("/consume")
public class ChildConsumeController {

	@Autowired
	private UsersService usersService;

	@Autowired
	private ChildConsumeService consumeService;

	@Autowired
	private ConsumeMapper consumeMapper;

	// 소비등록페이지
	@GetMapping
	public String goConsumePage(@SessionAttribute("user_no") Integer userNo,
			Model model) {
		ChildInfoVO civo = usersService.getChildInfo(userNo);

		if (civo.getAccount_no() == null) {
			model.addAttribute("msg", "연결된 계좌가 없습니다. 먼저 계좌를 등록하세요.");
			model.addAttribute("target", "/child_page");
			model.addAttribute("icon", "warning");
			return "alert-redirect";
		}

		model.addAttribute("userInfo", civo);
		model.addAttribute("consume", new ConsumeVO());
		model.addAttribute("isEdit", false);

		// kpi 부분
		TypeRatio day = consumeService.getTypeRatio(userNo, Window.DAY_1);
		TypeRatio thisMonth = consumeService.getTypeRatioForMonth(userNo, YearMonth.now());
		model.addAttribute("todayConsume", day.getTotal());
		model.addAttribute("monthConsume", thisMonth.getTotal());

		return "child/consume";
	}

	// 소비등록
	@PostMapping("/insert")
	public String insertConsume(@SessionAttribute("user_no") Integer userNo, ConsumeVO cvo, Model model) {
		try {
			consumeService.addConsumeWithPoint(cvo, userNo);

			model.addAttribute("msg", "소비 등록이 완료되었습니다");
			model.addAttribute("target", "/consume");
			model.addAttribute("icon", "success");
			return "alert-redirect";
		} catch (Exception e) {
			model.addAttribute("msg", e.getMessage() != null ? e.getMessage() : "처리 중 오류가 발생했습니다.");
			model.addAttribute("target", "/consume");
			model.addAttribute("icon", "error");
			return "alert-redirect";
		}
	}

	// 소비내역 페이지
	@GetMapping("/list")
	public String list(@SessionAttribute("user_no") Integer userNo,
			@RequestParam(required = false) String fromDate,
			@RequestParam(required = false) String toDate,
			@RequestParam(required = false) Integer consType,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			Model model) {

		if (userNo == null) {
			model.addAttribute("msg", "로그인이 필요합니다.");
			model.addAttribute("target", "/login");
			model.addAttribute("icon", "warning");
			return "alert-redirect";
		}

		int offset = page * size;
		Map<String, Object> param = new HashMap<>();
		param.put("userNo", userNo);
		param.put("fromDate", fromDate);
		param.put("toDate", toDate);
		param.put("consType", consType);
		param.put("keyword", keyword);
		param.put("offset", offset);
		param.put("limit", size);

		List<ConsumeVO> list = consumeMapper.selectConsumeList(param);
		int total = consumeMapper.countConsumeList(param);
		int totalPages = (int) Math.ceil((double) total / size);
		long sumAmount = consumeService.sumConsumeAmount(userNo, fromDate, toDate, consType, keyword);
		int displayPage = page + 1; // 화면 표시는 1-based

		model.addAttribute("list", list);
		model.addAttribute("total", total);
		model.addAttribute("page", page); // 0-based 현재 페이지
		model.addAttribute("size", size);
		model.addAttribute("totalPages", totalPages); // 총 페이지 수(1..N)
		model.addAttribute("displayPage", displayPage); // 화면 표시용(1-based)
		model.addAttribute("sumAmount", sumAmount);

		model.addAttribute("fromDate", fromDate);
		model.addAttribute("toDate", toDate);
		model.addAttribute("consType", consType);
		model.addAttribute("keyword", keyword);

		// kpi 부분
		// 오늘 지출내역
		TypeRatio day = consumeService.getTypeRatio(userNo, Window.DAY_1);
		model.addAttribute("todayList", day.getConsumes());

		// 최근 7일 타입별 비율
		TypeRatio week = consumeService.getTypeRatio(userNo, Window.DAY_7);
		model.addAttribute("ratioByType7d", week.getRatioByType());

		return "child/consume_list";
	}

	// 수정 폼 (consume.html 재활용)
	@GetMapping("/edit")
	public String edit(@SessionAttribute("user_no") Integer userNo,
			@RequestParam("cons_no") long consNo,
			Model model) {
		ConsumeVO vo = consumeMapper.selectConsumeById(consNo);
		if (vo == null) {
			model.addAttribute("msg", "내역이 없습니다.");
			model.addAttribute("target", "/consume/list");
			model.addAttribute("icon", "warning");
			return "alert-redirect";
		}
		model.addAttribute("userInfo", usersService.getChildInfo(userNo));
		model.addAttribute("consume", vo);
		model.addAttribute("isEdit", true);

		// kpi 부분
		TypeRatio day = consumeService.getTypeRatio(userNo, Window.DAY_1);
		TypeRatio thisMonth = consumeService.getTypeRatioForMonth(userNo, YearMonth.now());
		model.addAttribute("todayConsume", day.getTotal());
		model.addAttribute("monthConsume", thisMonth.getTotal());

		return "child/consume";
	}

	// 수정 처리
	@PostMapping("/update")
	public String update(@SessionAttribute("user_no") Integer userNo,
			ConsumeVO form,
			RedirectAttributes ra,
			Model model) {
		try {
			consumeService.updateConsumeWithDiff(form, userNo);

			model.addAttribute("msg", "수정되었습니다.");
			model.addAttribute("target", "/consume/list");
			model.addAttribute("icon", "success");
			return "alert-redirect";
		} catch (Exception e) {
			model.addAttribute("msg", e.getMessage());
			model.addAttribute("target", "/consume/list");
			model.addAttribute("icon", "errer");
			return "alert-redirect";
		}
	}

	// 삭제 처리
	@PostMapping("/delete")
	public String delete(@SessionAttribute("user_no") Integer userNo,
			@RequestParam("cons_no") long consNo,
			RedirectAttributes ra,
			Model model) {
		try {
			consumeService.deleteConsumeAndRefund(consNo, userNo);
			model.addAttribute("msg", "삭제되었습니다.");
			model.addAttribute("target", "/consume/list");
			model.addAttribute("icon", "success");
			return "alert-redirect";
		} catch (Exception e) {
			model.addAttribute("msg", e.getMessage());
			model.addAttribute("target", "/consume/list");
			model.addAttribute("icon", "errer");
			return "alert-redirect";
		}
	}

}
