package com.ddak.yongha.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ddak.yongha.mapper.ParentMailMapper;
import com.ddak.yongha.vo.UsersVO;

@Service
public class ParentMailService {

	@Autowired
	private ParentMailMapper parentMailMapper;

	// 연결된 자녀 조회
	public List<UsersVO> getChildrenByParent(int parentNo) {
		return parentMailMapper.findChildrenByParent(parentNo);
	}

	// 현재 메일 주기 조회 (DB 값 → 프론트 코드 매핑)
	public Integer getMailCycle(int parentNo) {
		Integer cycle = parentMailMapper.findMailCycleByParentNo(parentNo);
		return cycle;
	}

	// 메일 수신 주기 저장
	public void saveMailCycle(int parentNo, Integer mailCycle) {
		boolean exists = parentMailMapper.existsParentUser(parentNo) != null
				&& parentMailMapper.existsParentUser(parentNo) > 0;

		if (mailCycle == null) {
			if (exists) {
				parentMailMapper.deleteMailCycle(parentNo);
			}
			return;
		}

		if (exists) {
			parentMailMapper.updateMailCycle(parentNo, mailCycle);
		} else {
			parentMailMapper.insertParentUser(parentNo, mailCycle);
		}
	}

}
