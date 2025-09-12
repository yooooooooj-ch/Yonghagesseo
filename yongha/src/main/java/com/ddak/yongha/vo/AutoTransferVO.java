package com.ddak.yongha.vo;

import lombok.Data;

@Data
public class AutoTransferVO {

	/*
	 * auto_trans_no   NUMBER PRIMARY KEY,
	 * from_account_no NUMBER,
	 * to_account_no   NUMBER,
	 * amount          NUMBER(20, 0) DEFAULT 0 CHECK (amount >= 0) NOT NULL,
	 * trans_cycle     NUMBER DEFAULT 1 NOT NULL,
	 * last_trans_date DATE,
	 */

	private int auto_trans_no;
	private int from_account_no, to_account_no;
	private int amount;
	private int trans_cycle;

}
