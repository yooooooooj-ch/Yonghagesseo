package com.ddak.yongha.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ddak.yongha.mapper.ConsumeMapper;
import com.ddak.yongha.vo.ConsumeVO;
import com.ddak.yongha.vo.TypeRatio;
import com.ddak.yongha.vo.Window;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChildConsumeService {

	private final ConsumeMapper consumeMapper;

	@Transactional
	public void addConsumeWithPoint(ConsumeVO vo, int userNo) {
		// 1) 보유 포인트 잠금 조회
		Long currentPoint = consumeMapper.selectPointForUpdate(userNo);
		if (currentPoint == null)
			currentPoint = 0L;

		long amount = nvl(vo.getAmount());
		long requestUsedPoint = nvl(vo.getUsed_point());

		// === 사전 검증 ===
		if (requestUsedPoint < 0) {
			throw new IllegalStateException("사용 포인트는 0 이상이어야 합니다.");
		}
		// 사용 포인트가 결제 금액보다 큰 요청은 허용하지 않음 (선택 사항이지만 권장)
		if (requestUsedPoint > amount) {
			throw new IllegalStateException("사용 포인트가 결제 금액을 초과했습니다.");
		}
		// 요청 포인트가 보유 포인트보다 크면 실패
		if (requestUsedPoint > currentPoint) {
			throw new IllegalStateException("보유 포인트가 부족합니다.");
		}

		// 2) 실제 사용할 포인트 = 요청값 (위 검증 통과 시 보유/금액 범위 내)
		long actualUsedPoint = requestUsedPoint;
		long chargeAmount = amount - actualUsedPoint;

		// 3) 포인트 차감
		if (actualUsedPoint > 0) {
			int upd = consumeMapper.deductPoint(userNo, actualUsedPoint);
			if (upd != 1) {
				throw new IllegalStateException("포인트 차감에 실패했습니다.");
			}
		}

		// 4) 잔액 차감(부족 시 실패)
		if (chargeAmount > 0) {
			int upd = consumeMapper.deductBalance(vo.getAccount_no(), chargeAmount);
			if (upd != 1) {
				throw new IllegalStateException("잔액이 부족하거나 차감에 실패했습니다.");
			}
		}

		// 5) 소비 INSERT (실제 사용 포인트 저장)
		vo.setUsed_point(actualUsedPoint);
		int ins = consumeMapper.insertConsume(vo);
		if (ins != 1) {
			throw new IllegalStateException("소비 등록에 실패했습니다.");
		}
	}

	@Transactional
	public void updateConsumeWithDiff(ConsumeVO req, int userNo) {
		// 0) 원본 로드(+ 소유 검증)
		ConsumeVO origin = consumeMapper.selectConsumeById(req.getCons_no());
		if (origin == null)
			throw new IllegalStateException("존재하지 않는 내역입니다.");
		if (!Objects.equals(origin.getAccount_no(), req.getAccount_no())) {
			// 보안상 요청의 account_no를 무시하고 원본 account_no 사용 권장
			req.setAccount_no(origin.getAccount_no());
		}

		long oldAmount = nvl(origin.getAmount());
		long oldUsedPt = nvl(origin.getUsed_point());
		long reqAmount = nvl(req.getAmount());
		long reqUsedPt = nvl(req.getUsed_point());

		// 1) 포인트 가용치 확보: (현재 보유 포인트 + 기존 사용 포인트) 만큼은 재사용 가능
		Long curPoint = consumeMapper.selectPointForUpdate(userNo);
		if (curPoint == null)
			curPoint = 0L;
		long maxUsablePoint = curPoint + oldUsedPt;

		long newUsedPt = Math.min(reqUsedPt, Math.min(reqAmount, maxUsablePoint));
		long deltaPoint = newUsedPt - oldUsedPt;

		long oldCash = oldAmount - oldUsedPt;
		long newCash = reqAmount - newUsedPt;
		long deltaCash = newCash - oldCash;

		// 2) 포인트 증감(먼저 처리)
		if (deltaPoint > 0) {
			int r = consumeMapper.deductPoint(userNo, deltaPoint);
			if (r != 1)
				throw new IllegalStateException("포인트 추가 차감 실패");
		} else if (deltaPoint < 0) {
			int r = consumeMapper.creditPoint(Map.of("userNo", userNo, "addPoint", -deltaPoint));
			if (r != 1)
				throw new IllegalStateException("포인트 환급 실패");
		}

		// 3) 잔액 증감
		if (deltaCash > 0) {
			int r = consumeMapper.deductBalance(origin.getAccount_no(), deltaCash);
			if (r != 1)
				throw new IllegalStateException("잔액 추가 차감 실패(부족)");
		} else if (deltaCash < 0) {
			int r = consumeMapper.creditBalance(Map.of("accountNo", origin.getAccount_no(), "addAmount", -deltaCash));
			if (r != 1)
				throw new IllegalStateException("잔액 환급 실패");
		}

		// 4) UPDATE
		req.setUsed_point(newUsedPt);
		int u = consumeMapper.updateConsumeAll(req);
		if (u != 1)
			throw new IllegalStateException("수정 실패");
	}

	@Transactional
	public void deleteConsumeAndRefund(long consNo, int userNo) {
		ConsumeVO origin = consumeMapper.selectConsumeById(consNo);
		if (origin == null)
			return;

		// (선택) 소유 검증: origin.account_no → userNo 소유 확인

		long usedPt = nvl(origin.getUsed_point());
		long cash = nvl(origin.getAmount()) - usedPt;

		// 환급 순서 무관하나, 오류 시 전체 롤백
		if (usedPt > 0) {
			int r = consumeMapper.creditPoint(Map.of("userNo", userNo, "addPoint", usedPt));
			if (r != 1)
				throw new IllegalStateException("포인트 환급 실패");
		}
		if (cash > 0) {
			int r = consumeMapper.creditBalance(Map.of("accountNo", origin.getAccount_no(), "addAmount", cash));
			if (r != 1)
				throw new IllegalStateException("잔액 환급 실패");
		}

		int d = consumeMapper.deleteConsume(consNo);
		if (d != 1)
			throw new IllegalStateException("삭제 실패");
	}

	private long nvl(Long v) {
		return v == null ? 0L : v;
	}

	public long sumConsumeAmount(Integer userNo, String fromDate, String toDate, Integer consType, String keyword) {
		Map<String, Object> p = new HashMap<>();
		p.put("userNo", userNo);
		p.put("fromDate", fromDate);
		p.put("toDate", toDate);
		p.put("consType", consType);
		p.put("keyword", keyword);
		Long s = consumeMapper.sumConsumeAmountByUserNo(p);
		return s == null ? 0L : s;
	}

	// 공통 집계 메서드: [from ~ to] (양 끝 포함)
	public TypeRatio getTypeRatio(int userNo, LocalDate from, LocalDate to) {
		if (from == null || to == null)
			throw new IllegalArgumentException("from/to must not be null");
		if (to.isBefore(from))
			throw new IllegalArgumentException("to must be >= from");

		// DB 조회 (기간 내 소비내역)
		List<ConsumeVO> consumes = getConsumesBetween(userNo, from, to);

		// 타입별 합계
		Map<Integer, Long> amountByType = consumes.stream()
				.collect(Collectors.groupingBy(
						ConsumeVO::getCons_type,
						TreeMap::new,
						Collectors.summingLong(ConsumeVO::getAmount)));

		long total = amountByType.values().stream().mapToLong(Long::longValue).sum();

		// 비율 계산
		Map<Integer, Double> ratioByType = new LinkedHashMap<>();
		for (Map.Entry<Integer, Long> e : amountByType.entrySet()) {
			double pct = (total == 0) ? 0.0 : (e.getValue() * 100.0) / total;
			ratioByType.put(e.getKey(), round1(pct));
		}

		return new TypeRatio(consumes, amountByType, ratioByType, total, from, to);
	}

	// 프리셋(하루/7일/30일)용 편의 메서드: 모두 "오늘 포함 N일" 윈도우
	public TypeRatio getTypeRatio(int userNo, Window window) {
		LocalDate today = LocalDate.now();
		LocalDate start = today.minusDays(window.days() - 1); // 예: 7일이면 today-6
		return getTypeRatio(userNo, start, today);
	}

	// 필요시 특정 월(달력월) 집계가 필요하면 이렇게 추가:
	// 해당 달(예: 이번 달)의 1일 ~ 말일 (오늘 포함 아님, “달력월” 기준)
	public TypeRatio getTypeRatioForMonth(int userNo, YearMonth ym) {
		LocalDate from = ym.atDay(1);
		LocalDate to = ym.atEndOfMonth();
		return getTypeRatio(userNo, from, to);
	}

	// 기존 round1 재사용
	private double round1(double v) {
		return Math.round(v * 10.0) / 10.0;
	}

	public List<ConsumeVO> getConsumesBetween(int userNo, LocalDate from, LocalDate to) {
		// 양 끝 포함되도록 LocalDateTime 경계 지정
		LocalDateTime start = from.atStartOfDay();
		LocalDateTime endInclusive = to.plusDays(1).atStartOfDay().minusNanos(1);
		return consumeMapper.selectRecentConsumesByUserNo(userNo, start, endInclusive);
	}

}
