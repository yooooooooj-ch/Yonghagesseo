package com.ddak.yongha.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ddak.yongha.mapper.ConsumeMapper;
import com.ddak.yongha.vo.ConsumeVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParentConsumeService {
	@Autowired
	private ConsumeMapper consumeMapper;

	public void assertChildOfParent(Integer parentNo, Integer childNo) {
		Integer cnt = consumeMapper.countChildOfParent(parentNo, childNo);
		if (cnt == null || cnt == 0)
			throw new RuntimeException("조회 권한이 없습니다.");
	}

	public List<ConsumeVO> getConsumeListByChild(Integer childNo, String fromDate, String toDate,
			Integer consType, String keyword, int page, int size) {
		Map<String, Object> p = new HashMap<>();
		p.put("userNo", childNo);
		p.put("fromDate", fromDate);
		p.put("toDate", toDate);
		p.put("consType", consType);
		p.put("keyword", keyword);
		int offset = Math.max(0, page) * Math.max(1, size);
		p.put("offset", offset);
		p.put("limit", Math.max(1, size));
		return consumeMapper.selectConsumeList(p);
	}

	public int countConsumeListByChild(Integer childNo, String fromDate, String toDate,
			Integer consType, String keyword) {
		Map<String, Object> p = new HashMap<>();
		p.put("userNo", childNo);
		p.put("fromDate", fromDate);
		p.put("toDate", toDate);
		p.put("consType", consType);
		p.put("keyword", keyword);
		return consumeMapper.countConsumeList(p);
	}

	public long sumConsumeAmountByChild(Integer childNo, String fromDate, String toDate,
			Integer consType, String keyword) {
		// 합계 전용 쿼리가 없다면 mapper에 sum 추가(아래 XML 참고).
		Map<String, Object> p = new HashMap<>();
		p.put("userNo", childNo);
		p.put("fromDate", fromDate);
		p.put("toDate", toDate);
		p.put("consType", consType);
		p.put("keyword", keyword);
		Long s = consumeMapper.sumConsumeAmountByUserNo(p);
		return s == null ? 0L : s;
	}

}
