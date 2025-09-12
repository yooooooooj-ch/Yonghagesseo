package com.ddak.yongha.vo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.Data;

@Data
public class ChildInfoVO {

	private Integer user_no;
	private String user_name;
	private Integer profile;
	private Integer user_type;

	private Integer account_no;
	private String account_id;
	private Long balance;
	private String bank_name;

	private Long point;

	private Integer goal_no;
	private Integer goal_type;
	private String goal_name;
	private Long target_amount;
	private LocalDate start_date;
	private LocalDate end_date;
	private Integer achieved;

	private long dday;
	private Integer progress;

	public void calculateDday() {
		if (end_date != null) {
			LocalDate today = LocalDate.now();
			this.dday = ChronoUnit.DAYS.between(today, end_date);
		} else {
			this.dday = -1;
		}
	}

	public void calculateProgress() {
		if (balance == null || target_amount == null || target_amount <= 0) {
			this.progress = 0;
			return;
		}
		double ratioPct = (balance * 100.0) / target_amount;
		int pct = (int) Math.round(ratioPct);
		if (pct < 0)
			pct = 0;
		if (pct > 100)
			pct = 100;
		this.progress = pct;
	}
}
