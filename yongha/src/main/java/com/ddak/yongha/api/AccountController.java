package com.ddak.yongha.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.ddak.yongha.service.AccountService;
import com.ddak.yongha.service.UsersService;
import com.ddak.yongha.vo.AccountsVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

	@Autowired
	private AccountService accountService;

	@Autowired
	private UsersService usersService;

	@GetMapping("/suggestions")
	public ResponseEntity<?> getSuggestions(@SessionAttribute(value = "user_no", required = false) Integer user_no) {
		if (user_no == null) {
			return ResponseEntity
					.status(HttpStatus.BAD_REQUEST)
					.body("로그인이 필요한 서비스입니다.");
		}

		List<AccountsVO> suggestions = accountService.generateSuggestions(usersService.isChild(user_no));
		return ResponseEntity.ok(suggestions);
	}

	@PostMapping("/register")
	public ResponseEntity<?> registerAccount(@RequestBody AccountsVO avo,
			@SessionAttribute(value = "user_no", required = false) Integer user_no) {
		// user_no = null;
		if (user_no == null) {
			return ResponseEntity
					.status(HttpStatus.BAD_REQUEST)
					.body("로그인이 필요한 서비스입니다.");
		}

		if (accountService.hasAccount(avo.getAccount_id())) {
			return ResponseEntity
					.status(HttpStatus.BAD_REQUEST)
					.body("이미 존재하는 계좌번호입니다.");
		}

		if (accountService.hasAccount(user_no)) {
			accountService.updateAccount(avo, user_no);
			return ResponseEntity.ok().build();
		}

		accountService.register(avo, user_no);
		return ResponseEntity.ok().build();
	}

}
