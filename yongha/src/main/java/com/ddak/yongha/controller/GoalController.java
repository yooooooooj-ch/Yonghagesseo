package com.ddak.yongha.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GoalController {

	// (옵션) 기존 공용 페이지도 parent 폴더 쓰는 경우
	@GetMapping("/goalsetting")
	public String goalSettingPage() {
		return "goalsetting"; // templates/parent/goalsetting.html
	}

	@GetMapping("/goals/parent")
	public String goalsParent() {
		return "goals/goalsetting-parent"; //
	}

	@GetMapping("/goals/child")
	public String goalsChild() {
		return "goals/goalsetting-child"; //
	}

}
