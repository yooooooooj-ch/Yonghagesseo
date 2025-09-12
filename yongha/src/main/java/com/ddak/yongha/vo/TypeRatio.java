package com.ddak.yongha.vo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 반환 DTO (원래 TypeRatio7d를 일반화)
@Getter
@AllArgsConstructor
public class TypeRatio {
	private final List<ConsumeVO> consumes;
	private final Map<Integer, Long> amountByType; // 타입별 합계
	private final Map<Integer, Double> ratioByType; // 타입별 비율(%), 소수1자리 반올림
	private final long total; // 총액
	private final LocalDate from; // 집계 시작일(포함)
	private final LocalDate to; // 집계 종료일(포함)
}
