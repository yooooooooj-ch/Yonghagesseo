package com.ddak.yongha.vo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.Data;

@Data
public class UsersVO {

	/*	  user_no     NUMBER PRIMARY KEY,
	 *	  user_id     VARCHAR2(20) NOT NULL UNIQUE,
	 *	  password    VARCHAR2(100) NOT NULL,
	 *	  user_name   VARCHAR2(50) NOT NULL,
	 *	  profile     NUMBER DEFAULT 0 CHECK (profile IN (0, 1, 2, 3)), -- 0: 핑크용, 1: 파란용, 2: 초록용, 3: 노란용
	 *	  birthday    DATE NOT NULL,
	 *	  email       VARCHAR2(100),
	 *	  address     VARCHAR2(200),
	 *	  tel         VARCHAR2(20),
	 *	  user_type   NUMBER CHECK (user_type IN (0, 1)) NOT NULL, -- 0: 부모, 1: 자녀
	 *	  fcm_token   VARCHAR2(255),	-- fcm 토큰 정보
	 *	  mail_cycle  NUMBER  DEFAULT 1, -- 0: 매일 / 1: 일주일 / 2: 한달 
	 */

	private int user_no, user_type, profile;
	private String user_id, password, user_name, email, address, tel, fcm_token;
	private LocalDate birthday;

	private int account_no;
	private String account_id;
	private Integer balance;
	private String bank_name;
	private int trans_cycle;

	private LocalDate last_trans_date; // 가장 최근 이체일
	private LocalDate nextTransDate; // 다음 이체일
	private long dday; // 남은 일수

	private Integer mail_cycle;

	public void calculateNextTransDateAndDday() {
		if (last_trans_date != null) {
			this.nextTransDate = last_trans_date.plusDays(trans_cycle);
			LocalDate today = LocalDate.now();
			this.dday = ChronoUnit.DAYS.between(today, nextTransDate);
		} else {
			this.dday = -1;
		}
	}
}
