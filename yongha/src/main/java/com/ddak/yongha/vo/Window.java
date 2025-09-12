package com.ddak.yongha.vo;

//기간 프리셋 (오늘 포함 N일 롤링)
public enum Window {
	DAY_1(1), DAY_7(7), DAY_30(30);

	private final int days;

	Window(int days) {
		this.days = days;
	}

	public int days() {
		return days;
	}
}