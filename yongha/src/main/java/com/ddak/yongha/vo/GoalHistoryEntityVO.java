package com.ddak.yongha.vo;

import java.time.LocalDate;

import lombok.Data;

@Data
public class GoalHistoryEntityVO {
	private int goal_no;         // 목표 번호 (PK)
    private int child_no;        // 목표 소유자 (Users.user_no)
    private int goal_type;       // 목표 유형 (0: 기타 / 1: 전자기기 / 2: 의류 / 3: 게임 / 4: 여행 ...)
    private String goal_name;    // 목표 이름
    private long target_amount;  // 목표 금액
    private LocalDate start_date;     // 시작일 (기본값: SYSDATE)
    private LocalDate end_date;       // 종료일 
    private int achieved;        // 달성 여부 (0: 미달성, 1: 달성)
    private int ready_to_complete;        // 달성 완료 가능 여부 (0: 완료 불가능, 1: 완료 가능)
    
    private long balance; 		// 계좌 잔액
}

/*
 * 랭킹, 통계 X
 * GoalHistory CRUD 용 VO
*/
