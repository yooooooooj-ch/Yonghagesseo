package com.ddak.yongha.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountsVO {

	/*
	 *  account_no   NUMBER PRIMARY KEY,  -- 계좌의 고정 ID
	 *  user_no      NUMBER UNIQUE,       -- 사용자당 1계좌
	 *  account_id   VARCHAR2(20) UNIQUE, -- 계좌번호 (변경 가능)
	 *  balance      NUMBER DEFAULT 0 CHECK (balance >= 0),
	 *  bank_name    VARCHAR2(50) NOT NULL,
	 */

	private int account_no;
	private String account_id, bank_name;
	private int balance;
}
