package com.ddak.yongha.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ddak.yongha.vo.ConsumeVO;

@Mapper
public interface ConsumeMapper {

	// --- 단건/목록 조회 ---
	ConsumeVO selectConsumeById(@Param("consNo") long consNo);

	List<ConsumeVO> selectConsumeList(Map<String, Object> params);

	int countConsumeList(Map<String, Object> params);

	// --- 등록/수정/삭제 ---
	int insertConsume(ConsumeVO vo);

	int updateConsumeAll(ConsumeVO vo);

	int deleteConsume(@Param("cons_no") long consNo);

	// --- 포인트/잔액 증감 ---
	Long selectPointForUpdate(@Param("userNo") int userNo); // FOR UPDATE

	int deductPoint(@Param("userNo") int userNo,
			@Param("usedPoint") long usedPoint);

	int creditPoint(Map<String, Object> params);

	int deductBalance(@Param("accountNo") long accountNo,
			@Param("chargeAmount") long chargeAmount);

	int creditBalance(Map<String, Object> params);

	long sumConsumeAmount(Map<String, Object> params);

	// --- 부모 페이지에서 자녀소비내역 조회 ---

	Integer countChildOfParent(Integer parentNo, Integer childNo);

	Long sumConsumeAmountByUserNo(Map<String, Object> params);

	// 김동주 작성
	// 최근 7일치 자녀 소비 데이터 조회
	List<ConsumeVO> selectRecentConsumesByUserNo(
			@Param("userNo") int userNo,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate);

}