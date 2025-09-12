package com.ddak.yongha.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ddak.yongha.mapper.AccountsMapper;
import com.ddak.yongha.mapper.AutoTransferMapper;
import com.ddak.yongha.mapper.TransferMapper;
import com.ddak.yongha.vo.AutoTransferVO;
import com.ddak.yongha.vo.TransferVO;

@Service
public class TransferService {

	@Autowired
	private TransferMapper transferMapper;

	@Autowired
	private AutoTransferMapper autoTransferMapper;

	@Autowired
	private AccountsMapper accountsMapper;

	public void insertTransfer(TransferVO tvo) {
		transferMapper.insertTransfer(tvo);

	}

	public void registerAutoTransfer(AutoTransferVO atvo) {
		autoTransferMapper.insertAutoTransfer(atvo);

	}

	public AutoTransferVO getAutoTransfer(int from_account_no, int to_account_no) {
		AutoTransferVO atvo = new AutoTransferVO();
		atvo.setFrom_account_no(from_account_no);
		atvo.setTo_account_no(to_account_no);
		return autoTransferMapper.selectAutoTransfer(atvo);
	}

	public void deleteAutoTransfer(int from_account_no, int to_account_no) {
		AutoTransferVO atvo = new AutoTransferVO();
		atvo.setFrom_account_no(from_account_no);
		atvo.setTo_account_no(to_account_no);
		autoTransferMapper.deleteAutoTransfer(atvo);
	}

	public void updateAutoTransfer(AutoTransferVO atvo) {
		autoTransferMapper.updateAutoTransfer(atvo);
	}

	@Transactional
	public void executeAutoTransfers() {
		List<AutoTransferVO> dueTransfers = autoTransferMapper.selectDueTransfers();
		// List<AutoTransferVO> dueTransfers =
		// autoTransferMapper.selectDueTransfersTest();
		for (AutoTransferVO transfer : dueTransfers) {
			try {
				// transfer 테이블에 거래 기록
				TransferVO tvo = new TransferVO();
				tvo.setFrom_account_no(transfer.getFrom_account_no());
				tvo.setTo_account_no(transfer.getTo_account_no());
				tvo.setAmount(transfer.getAmount());
				tvo.setTrans_desc("자동이체");
				transferMapper.insertTransfer(tvo);

				// 잔액 차감 + 이체 처리
				accountsMapper.updateBalance(transfer.getFrom_account_no(), -transfer.getAmount());
				accountsMapper.updateBalance(transfer.getTo_account_no(), transfer.getAmount());

				// 마지막 이체일 업데이트
				autoTransferMapper.updateLastTransferDate(transfer.getFrom_account_no(), transfer.getTo_account_no());

				System.out.println("자동이체 완료 : " + transfer.getFrom_account_no() + " -> " + transfer.getTo_account_no()
						+ " : " + transfer.getAmount());
			} catch (Exception e) {
				System.out.println("자동이체 에러 : " + e);
			}
		}
	}

	// 부모가 이체한 금액에 대한 총량 찾기
    public List<Map<String, Object>> getDailyTransferAmountAllAccounts() {
        return transferMapper.getDailyTransferAmountAllAccounts();
    }

}
