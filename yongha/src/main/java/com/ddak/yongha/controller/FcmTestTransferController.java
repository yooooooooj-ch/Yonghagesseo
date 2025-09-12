package com.ddak.yongha.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ddak.yongha.service.FcmService;
import com.ddak.yongha.service.FcmTestTransferService;
import com.ddak.yongha.vo.FcmRequestVo;
import com.ddak.yongha.vo.FcmTestTransferVO;

@Controller
@RequestMapping("/fcmpush")
public class FcmTestTransferController {

	@Autowired
	private FcmTestTransferService transferService;

	@Autowired
	private FcmService fcmService;

	@GetMapping("/form")
	public String showTransferForm() {
		return "fcmTestTransferForm";
	}

	@PostMapping("/charge")
	public String charge(@ModelAttribute FcmTestTransferVO vo) {
		boolean result = transferService.chargeAllowance(vo);
		if (!result) {
			return "redirect:/fcmpush/form?fail";
		}

		// 이체 성공 시 바로 푸시 발송
		FcmRequestVo req = new FcmRequestVo();
		req.setUser_no(1); // 테스트용 고정 (원하면 제거/조회 로직으로 변경)
		req.setToken(
				"c-zFE1A-C-DsM3VQc9rUs-:APA91bEIvTZewO4egnshznZW7VziP7lFVu8_mnJDEEgtBqt-5YoWazJWz7EraRJ2aVTOTh-wGl_sYkh75-HAOUXE9T_uCKlgfzTVeJ5gxiyNyyFG53WwRIc"); // 폼에
																																									// 토큰이
																																									// 없다면
																																									// DB에서
																																									// 조회하도록
																																									// 수정

		FcmRequestVo.Notification noti = new FcmRequestVo.Notification("용돈 입금 완료",
				"계좌로 " + vo.getAmount() + "원이 입금되었습니다.", null);
		req.setNotification(noti);

		try {
			fcmService.sendPushNotification(req);
			return "redirect:/fcmpush/form?success";
		} catch (Exception e) {
			// TODO: 로그 추가
			return "redirect:/fcmpush/form?pushFail";
		}
	}

	// JSON 테스트용 엔드포인트(유지하고 싶으면)
	@PostMapping("/send")
	@ResponseBody
	public org.springframework.http.ResponseEntity<String> sendMessage(@RequestBody FcmRequestVo request) {
		if (request.getToken() == null || request.getToken().isBlank()) {
			return org.springframework.http.ResponseEntity.badRequest().body("타겟 토큰이 없습니다.");
		}
		fcmService.sendPushNotification(request);
		return org.springframework.http.ResponseEntity.ok("FCM 전송 완료");
	}
}
