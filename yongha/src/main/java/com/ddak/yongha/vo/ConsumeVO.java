package com.ddak.yongha.vo;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class ConsumeVO {
	private Long cons_no;
	private Long account_no;
	private Long amount;
	private String cons_desc;
	private Integer cons_type;

	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	private LocalDateTime cons_date;

	private Long used_point;

	// 조인용
	private Integer user_no;
	private String account_id;
	private String bank_name;
}
