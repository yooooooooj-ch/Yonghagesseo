package com.ddak.yongha.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ddak.yongha.service.TransferService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AutoTransferScheduler {

	private final TransferService transferService;

	// 매일 자정 실행 (cron: 초, 분, 시, 일, 월, 요일)
	@Scheduled(cron = "0 0 0 * * ?") // 매일 자정마다 실행
	// @Scheduled(cron = "0/30 * * * * ?") // 매 30초마다 실행 (테스트용)
	public void executeAutoTransfers() {
		transferService.executeAutoTransfers();
	}
}