package com.ddak.yongha.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ddak.yongha.vo.GoalHistoryEntityVO;
import com.ddak.yongha.vo.GoalHistoryVO;

@Mapper
public interface GoalHistoryMapper {
	
	List<GoalHistoryVO> selectTopRankedUsers();
	GoalHistoryVO getGoalHistoryByUserNo(int user_no);
	
	
	// === CRUD ===
	// Create
	int insertGoalHistory(GoalHistoryEntityVO newGoal);
	// Read
	GoalHistoryEntityVO findByGoalNoAndChildNo(@Param("goalNo") Integer goalNo, @Param("childNo") Integer childNo);
	List<GoalHistoryEntityVO> findByChildNo(Integer childNo); // for all
	GoalHistoryEntityVO findActiveByChildNo(Integer childNo); // for 'achieved=0'
	int countActiveByChildNo(@Param("childNo") Integer childNo);
	List<GoalHistoryEntityVO> selectAllAchievedGoals(); // achieved=1 전체 목표
	List<Integer> selectAllAchievedGoalNos(); // achieved=1 전체 goal_no
	// Update
	int updateGoal(@Param("goalNo") int goalNo, @Param("patch") GoalHistoryEntityVO patch);
	int completeGoalByChild(@Param("goalNo") int goalNo, @Param("childNo") Integer childNo);
	// Delete
	int deleteGoal(int goalNo);
}
