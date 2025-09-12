package com.ddak.yongha.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ddak.yongha.mapper.UsersMapper2;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsersService2 {

	@Autowired
	private final UsersMapper2 usersMapper2;

	// UsersService.java
	public Map<String, Integer> getChildParentCounts() {
		Map<String, Integer> counts = new HashMap<>();
		counts.put("parents", usersMapper2.countParents());
		counts.put("children", usersMapper2.countChildren());
		return counts;
	}

	public Map<String, Integer> getChildParentCountsWithPeriod(Map<String, Object> params) {
		Map<String, Integer> counts = new HashMap<>();
		counts.put("parents", usersMapper2.countParentsWithPeriod(params));
		counts.put("children", usersMapper2.countChildrenWithPeriod(params));
		return counts;
	}

	public Map<String, Integer> getChildAgeGroups(Map<String, Object> params) {
		Map<String, Object> raw = usersMapper2.getChildAgeGroups(params);
		Map<String, Integer> result = new HashMap<>();

		result.put("AGE_6_8", ((Number) raw.get("AGE_6_8")).intValue());
		result.put("AGE_9_12", ((Number) raw.get("AGE_9_12")).intValue());
		result.put("AGE_13_15", ((Number) raw.get("AGE_13_15")).intValue());
		result.put("AGE_16_18", ((Number) raw.get("AGE_16_18")).intValue());

		return result;
	}
	
	public Map<String, Integer> getAvgConsByAgeGroups(Map<String, Object> params) {
		Map<String, Object> raw = usersMapper2.getAvgConsByAgeGroups(params);
		Map<String, Integer> result = new HashMap<>();
		
		result.put("AGE_6_8",  ((Number) raw.getOrDefault("AGE_6_8", 0)).intValue());
		result.put("AGE_9_12", ((Number) raw.getOrDefault("AGE_9_12", 0)).intValue());
		result.put("AGE_13_15",((Number) raw.getOrDefault("AGE_13_15", 0)).intValue());
		result.put("AGE_16_18",((Number) raw.getOrDefault("AGE_16_18", 0)).intValue());
		
		return result;
	}
	
	public Map<String, Integer> getPrefRate(Map<String, Object> params) {
	    List<Map<String, Object>> raw = usersMapper2.getPrefRate(params);

	    Map<String, Integer> result = new LinkedHashMap<>(); 
	    for (Map<String, Object> r : raw) {
	        String label = (String) r.get("goal_name");        
	        int cnt = ((Number) r.get("cnt")).intValue();       
	        result.put(label, cnt);
	    }
	    return result; 
	}
	
	public Map<String, Integer> getGoalsCountsWithPeriod(Map<String, Object> params) {
		Map<String, Integer> counts = new HashMap<>();
		counts.put("achieved", usersMapper2.countAchievedGoals(params));
		counts.put("unachieved", usersMapper2.countUnachievedGoals(params));
		return counts;
	}

	public Map<String, Integer> getAchvPeriodRate(Map<String, Object> params) {
		List<Map<String, Object>> raw = usersMapper2.getAchvPeriodRate(params);

	    Map<String, Integer> result = new LinkedHashMap<>(); 
	    for (Map<String, Object> r : raw) {
	        String label = (String) r.get("period");        
	        int cnt = ((Number) r.get("cnt")).intValue();       
	        result.put(label, cnt);
	    }
	    return result; 
	}
	
	
}
