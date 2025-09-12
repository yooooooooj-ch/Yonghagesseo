package com.ddak.yongha.config;

import java.util.List;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.ddak.yongha.service.CodeService;
import com.ddak.yongha.vo.CodeDto;

@ControllerAdvice
public class GlobalCodeModelAdvice {
	private final CodeService codes;

	public GlobalCodeModelAdvice(CodeService codes) {
		this.codes = codes;
	}

	@ModelAttribute("PROFILE_CODES")
	public List<CodeDto> profile() {
		return codes.getList("PROFILE");
	}

	@ModelAttribute("GOAL_TYPE_CODES")
	public List<CodeDto> goal() {
		return codes.getList("GOAL_TYPE");
	}

	@ModelAttribute("CONS_TYPE_CODES")
	public List<CodeDto> cons() {
		return codes.getList("CONS_TYPE");
	}

	@ModelAttribute("USER_TYPE_CODES")
	public List<CodeDto> userType() {
		return codes.getList("USER_TYPE");
	}
}