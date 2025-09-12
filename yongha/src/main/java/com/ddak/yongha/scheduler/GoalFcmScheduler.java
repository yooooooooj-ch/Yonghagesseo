package com.ddak.yongha.scheduler;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ddak.yongha.mapper.GoalHistoryMapper;
import com.ddak.yongha.service.FcmService;
import com.ddak.yongha.service.UsersService;
import com.ddak.yongha.vo.GoalHistoryEntityVO;
import com.ddak.yongha.vo.UsersVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoalFcmScheduler {

	private final GoalHistoryMapper goalHistoryMapper;
	private final UsersService usersService;
	private final FcmService fcmService;

	/** 이미 '달성 상태(achieved=1)'로 본 적 있는 목표 집합 (서버 생명주기 동안 유지) */
	private final Set<Integer> seenAchievedGoals = ConcurrentHashMap.newKeySet();

	/** 서버 기동 시점에 이미 달성되어 있던 목표들은 '과거 건'으로 간주하여 베이스라인(메모리 셋)에 적재 */
	@EventListener(ApplicationReadyEvent.class)
	public void warmupBaseline() {
		try {
			List<Integer> achieved = goalHistoryMapper.selectAllAchievedGoalNos(); // achieved=1 전체 goal_no
			seenAchievedGoals.addAll(achieved);
			// log.info("GoalFcmScheduler baseline loaded: {} achieved goals",
			// achieved.size());
		} catch (Exception e) {
			// log.warn("Baseline warmup failed: {}", e.toString());
		}
	}

	/*
	 * 매 주기마다: 새로 달성된 목표(0->1 변화를 방금 거친 목표)를 찾아 '현재 시점' 푸시 ON(토큰 보유)인 부모에게만 FCM 전송.
	 * 이후 부모가 OFF->ON 해도 과거 건은 보내지 않음(요구사항 충족).
	 */
	@Scheduled(cron = "0 0 * * * ?")
	public void sendPushForAchievedGoals() {
		// 현재 achieved=1인 목표 전체 조회
		List<GoalHistoryEntityVO> allAchieved = goalHistoryMapper.selectAllAchievedGoals();
		if (allAchieved == null || allAchieved.isEmpty())
			return;

		for (GoalHistoryEntityVO g : allAchieved) {
			int goalNo = g.getGoal_no();

			// 이전 스냅샷에 없던 goal_no만 '신규 달성'으로 처리 
			// seenAchievedGoals == 서버 기동 시점에 이미 달성된 목표 집합
			if (seenAchievedGoals.add(goalNo)) {
				// -> add()가 true면 이번이 처음 본 것(=0->1 변화가 스냅샷 이후 발생)
				try {
					Integer childNo = g.getChild_no();
					List<UsersVO> parents = usersService.getMyParentsInfo(childNo);
					if (parents != null && !parents.isEmpty()) {
						String title = "자녀가 목표를 달성했어요 🎉";
						String goalTitle = (g.getGoal_name() != null) ? g.getGoal_name() : "목표";
						Long targetAmt = g.getTarget_amount();
						String body = (targetAmt != null) ? String.format("%s (%,d원) 목표를 달성했습니다.", goalTitle, targetAmt)
								: String.format("%s 목표를 달성했습니다.", goalTitle);

						// 부모별로 호출
						for (UsersVO p : parents) {
							// log.info("FCM (goal_achieved) -> parentNo={}", p.getUser_no());
							try {
								fcmService.sendNotification(p.getFcm_token(), title, body, null);
							} catch (Exception e) {
								// log.warn("부모 FCM 전송 실패 parentNo={}, childNo={}", p.getUser_no(), childNo, e);
							}
						}

					}
				} catch (Exception ex) {
					// 전체 처리 실패해도 스냅샷은 진척시킴(요구사항: 과거건 재발송 X)
					// log.warn("Process newly achieved goal failed (goalNo={}): {}", goalNo,
					// ex.toString());
				}
			}
		}
	}
}
