package com.ddak.yongha.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ddak.yongha.service.AccountService;
import com.ddak.yongha.service.FcmService;
import com.ddak.yongha.service.TransferService;
import com.ddak.yongha.service.UsersService;
import com.ddak.yongha.vo.AutoTransferVO;
import com.ddak.yongha.vo.TransferVO;
import com.ddak.yongha.vo.UsersVO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
public class TransferController {

	@Autowired
	private AccountService accountService;

	@Autowired
	private TransferService transferService;
	
	@Autowired
	private FcmService fcmService;
	
	@Autowired
	private UsersService usersService;

	@PostMapping("/allowance")
	public ResponseEntity<?> transferAllowance(@RequestBody TransferVO tvo) {

		if (tvo.getTo_account_no() == 0) {
			return badRequest("자녀용의 계좌번호가 존재하지 않습니다.");
		}

		// transfer 테이블에 이체 내역 저장
		transferService.insertTransfer(tvo);

		// acoount 테이블의 각 계좌에 금액 조정
		accountService.updateBalance(tvo.getFrom_account_no(), -tvo.getAmount());
		accountService.updateBalance(tvo.getTo_account_no(), tvo.getAmount());

		Map<String, Object> result = new HashMap<>();
		result.put("message", "용돈 주기 완료");
		
		// fcm 푸시알림 전송
		// 용돈받는 자녀의 푸시알림 ON/OFF 체크
		String childFcmToken = usersService.findByAccountNo(tvo.getTo_account_no()).getFcm_token();
		if(childFcmToken != null) {
			try {
                UsersVO parent = usersService.findByAccountNo(tvo.getFrom_account_no());
                String title = "용돈이 도착했어요! 🪙"; 
                String desc = tvo.getTrans_desc() != null ? tvo.getTrans_desc() : "";
                String body  = String.format("[%s]님으로부터 %,d원을 받았어요\n %s",
                		parent != null ? parent.getUser_name() : "부모",
                		tvo.getAmount(),
                		desc);
                
				try {
					fcmService.sendNotification(childFcmToken, title, body, null); // 전송
				} catch (Exception e) {
//					log.warn("자녀 FCM 전송 실패 parentNo={}, parent.getUser_no(), e);
				}
                
            } catch (Exception e) {
//                log.warn("용돈 이체 알림 전송 실패 transferNo={}", tvo.getTrans_no(), e);
            }
		}
		
		return ResponseEntity.ok(result);
	}

	@PostMapping("/auto-transfer")
	public ResponseEntity<?> autoTransfer(@RequestBody AutoTransferVO atvo) {

		if (atvo.getTo_account_no() == 0) {
			return badRequest("자녀용의 계좌번호가 존재하지 않습니다.");
		}

		transferService.registerAutoTransfer(atvo);

		Map<String, Object> result = new HashMap<>();
		result.put("message", "자동 이체 등록 완료");
		
		// fcm 푸시알림 전송
		// 용돈받는 자녀의 푸시알림 ON/OFF 체크
		String childFcmToken = usersService.findByAccountNo(atvo.getTo_account_no()).getFcm_token();
		if(childFcmToken != null) {
			try {
                UsersVO parent = usersService.findByAccountNo(atvo.getFrom_account_no());
                String title = "약속한 용돈이 도착했어요! 🪙"; 
                String body  = String.format("[%s]님으로부터 %,d원을 받았어요",
                		parent != null ? parent.getUser_name() : "부모",
                		atvo.getAmount());
                
				try {
					fcmService.sendNotification(childFcmToken, title, body, null); // 전송
				} catch (Exception e) {
//							log.warn("자녀 FCM 전송 실패 parentNo={}, parent.getUser_no(), e);
				}
                
            } catch (Exception e) {
//		                log.warn("자동 용돈 이체 알림 전송 실패 transferNo={}", atvo.getTrans_no(), e);
            }
		}

		return ResponseEntity.ok(result);
	}

	@GetMapping("/auto-transfer")
	public ResponseEntity<?> getAutoTransfer(@RequestParam int from_account_no, @RequestParam int to_account_no) {
		AutoTransferVO atvo = transferService.getAutoTransfer(from_account_no, to_account_no);
		if (atvo == null) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(atvo);
	}

	@DeleteMapping("/auto-transfer")
	public ResponseEntity<?> deleteAutoTransfer(@RequestBody AutoTransferVO atvo) {
		transferService.deleteAutoTransfer(atvo.getFrom_account_no(), atvo.getTo_account_no());
		Map<String, Object> result = new HashMap<>();
		result.put("message", "자동 이체 취소 완료");
		return ResponseEntity.ok(result);
	}

	@PutMapping("/auto-transfer")
	public ResponseEntity<?> updateAutoTransfer(@RequestBody AutoTransferVO atvo) {
		transferService.updateAutoTransfer(atvo);
		Map<String, Object> result = new HashMap<>();
		result.put("message", "자동 이체 변경 완료");
		return ResponseEntity.ok(result);
	}

	private ResponseEntity<Map<String, Object>> badRequest(String message) {
		Map<String, Object> error = new HashMap<>();
		error.put("message", message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

}
