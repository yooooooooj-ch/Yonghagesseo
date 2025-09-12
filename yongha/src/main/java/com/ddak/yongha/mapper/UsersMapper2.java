package com.ddak.yongha.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UsersMapper2 {
	@Select("SELECT COUNT(*) FROM users WHERE user_type = 0")
	int countParents();

	@Select("SELECT COUNT(*) FROM users WHERE user_type = 1")
	int countChildren();

	@Select("SELECT COUNT(*) FROM accounts")
	int countAccounts();

	Map<String, Object> getChildAgeGroups(Map<String, Object> params);

	Integer countParentsWithPeriod(Map<String, Object> params);

	Integer countChildrenWithPeriod(Map<String, Object> params);

	int countTotalUsersWithPeriod(String lastMonth);

	double countAvgTransferWithPeriod(Map<String, Object> params);
	
	Map<String, Object> getAvgConsByAgeGroups(Map<String, Object> params);	
	
	List<Map<String, Object>> getPrefRate(Map<String, Object> params);
	
	Integer countUnachievedGoals(Map<String, Object> params);
	
	Integer countAchievedGoals(Map<String, Object> params);

	List<Map<String, Object>> getAchvPeriodRate(Map<String, Object> params);

}