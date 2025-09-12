package com.ddak.yongha.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ddak.yongha.service.PredictionService;

@Component
public class PredictionScheduler {

	@Autowired
	private PredictionService predictionService;

	// 테스트용: 1분마다 실행
	// @Scheduled(fixedRate = 60 * 1000)
	@Scheduled(cron = "0 0 0 * * ?") // 매일 자정마다 실행
	public void runPredictionTask() {
		System.out.println("[Scheduler] 소비 예측 시작");
		predictionService.runPrediction();
		System.out.println("[Scheduler] 소비 예측 완료");
	}
}
