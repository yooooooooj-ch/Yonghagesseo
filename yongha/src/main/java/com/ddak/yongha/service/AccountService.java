package com.ddak.yongha.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ddak.yongha.mapper.AccountsMapper;
import com.ddak.yongha.vo.AccountsVO;

@Service
public class AccountService {

	@Autowired
	private AccountsMapper accountsMapper;

	public List<AccountsVO> generateSuggestions(boolean isChild) {
		Random random = new Random();

		// 계좌번호 자리수 패턴 목록
		List<int[]> patterns = List.of(
				new int[] { 3, 4, 6 },
				new int[] { 2, 6, 3 },
				new int[] { 4, 4, 4 },
				new int[] { 5, 3, 4 },
				new int[] { 3, 3, 7 });

		// 은행이름 목록
		List<String> bankList = new ArrayList<>(
				List.of("용하은행", "식스뱅크", "내일은행", "다시봄은행", "큐리뱅크", "무드뱅크", "딱돼은행"));

		Collections.shuffle(bankList, random);

		int aCount = 5; // 1~5개 추천
		if (isChild)
			aCount = 2; // 자녀일 경우는 1~2개만 추천

		int suggestionCount = 1 + random.nextInt(aCount);
		List<AccountsVO> suggestions = new ArrayList<>();

		// 추천 개수가 은행 개수를 초과하지 않도록 제한
		suggestionCount = Math.min(suggestionCount, bankList.size());

		for (int i = 0; i < suggestionCount; i++) {
			int[] pattern = patterns.get(random.nextInt(patterns.size()));
			StringBuilder accountNum = new StringBuilder();

			for (int j = 0; j < pattern.length; j++) {
				int digits = pattern[j];
				int maxValue = (int) Math.pow(10, digits);
				int value = random.nextInt(maxValue);
				accountNum.append(String.format("%0" + digits + "d", value));
				if (j < pattern.length - 1) {
					accountNum.append("-");
				}
			}

			int balance;

			if (isChild) {
				// 1000원 ~ 10만원 사이 금액 추천
				balance = 1_000 + random.nextInt(99_001);

			} else {
				// 최소 100만원 ~ 최대 1000만원 사이 금액 추천
				balance = 1_000_000 + random.nextInt(9_000_001);
			}

			String bank_name = bankList.get(i);

			AccountsVO avo = new AccountsVO();
			avo.setAccount_id(accountNum.toString());
			avo.setBank_name(bank_name);
			avo.setBalance(balance);

			suggestions.add(avo);
		}

		return suggestions;
	}

	// 계좌 등록
	public void register(AccountsVO avo, int user_no) {
		accountsMapper.insertAccount(avo, user_no);
	}

	public boolean hasAccount(String account_id) {
		return accountsMapper.hasAccountByAccountId(account_id);
	}

	public boolean hasAccount(int user_no) {
		return accountsMapper.hasAccountByUserNo(user_no);
	}

	public void updateBalance(int account_no, int amount) {
		accountsMapper.updateBalance(account_no, amount);
	}

	// 계좌 변경
	public void updateAccount(AccountsVO avo, Integer user_no) {
		accountsMapper.updateAccount(avo, user_no);

	}
	
	// 계좌 잔액 조회
	public Long findBalanceByUserNo(Integer user_no) {
		return accountsMapper.findBalanceByUserNo(user_no);
	}

}
