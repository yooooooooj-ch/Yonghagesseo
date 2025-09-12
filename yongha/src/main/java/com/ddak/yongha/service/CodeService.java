package com.ddak.yongha.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.ddak.yongha.mapper.CodeMapper;
import com.ddak.yongha.vo.CodeDto;

import jakarta.annotation.PostConstruct;

@Service
public class CodeService {
	private final CodeMapper codeMapper;
	private final ConcurrentHashMap<String, List<CodeDto>> listCache = new ConcurrentHashMap<>();

	public CodeService(CodeMapper codeMapper) {
		this.codeMapper = codeMapper;
	}

	@PostConstruct
	public void preload() {
		// 필요 타입 사전 로드
		getList("PROFILE");
		getList("GOAL_TYPE");
		getList("CONS_TYPE");
		getList("USER_TYPE");
	}

	public List<CodeDto> getList(String typeName) {
		return listCache.computeIfAbsent(typeName, this::reload);
	}

	public void refresh(String typeName) {
		listCache.put(typeName, reload(typeName));
	}

	private List<CodeDto> reload(String typeName) {
		List<Map<String, Object>> rows = codeMapper.selectCodesByType(typeName);
		List<CodeDto> list = new ArrayList<>(rows.size());
		for (var row : rows) {
			int value = ((Number) row.get("value")).intValue();
			String label = (String) row.get("label");
			String desc = (String) row.get("desc");
			list.add(new CodeDto(value, label, desc));
		}
		return Collections.unmodifiableList(list);
	}
}
