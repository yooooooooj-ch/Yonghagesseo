package com.ddak.yongha.vo;

import java.util.Date;

import lombok.Data;

@Data
public class StatisticsVO {
	private int stat_no;		// number,PK
	private int user_no;		// number
	private int stat_type;		// number, not null, check
	private String stat_img;	// Varchar2(200)
	private Date create_date;	// Date
}
