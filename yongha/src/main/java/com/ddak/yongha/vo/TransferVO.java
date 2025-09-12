package com.ddak.yongha.vo;

import java.time.LocalDate;

import lombok.Data;

@Data
public class TransferVO {

	/*
	 * trans_no        NUMBER PRIMARY KEY,
	 * from_account_id VARCHAR2(20) NOT NULL,
	 * to_account_id   VARCHAR2(20) NOT NULL,
	 * amount          NUMBER NOT NULL CHECK (amount >= 0),
	 * trans_date      DATE DEFAULT SYSDATE NOT NULL,
	 * trans_desc      VARCHAR2(200),
	 */

	private int trans_no;
	private int from_account_no, to_account_no;
	private int amount;
	private LocalDate trans_date;
	private String trans_desc;

}
