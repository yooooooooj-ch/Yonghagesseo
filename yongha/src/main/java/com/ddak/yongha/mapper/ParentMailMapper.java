package com.ddak.yongha.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ddak.yongha.vo.UsersVO;

@Mapper
public interface ParentMailMapper {

	List<UsersVO> findChildrenByParent(@Param("parentNo") int parentNo);

	Integer findMailCycleByParentNo(@Param("parentNo") int parentNo);

	Integer existsParentUser(@Param("parentNo") int parentNo);

	void updateMailCycle(@Param("parentNo") int parentNo, @Param("mailCycle") int mailCycle);

	void insertParentUser(@Param("parentNo") int parentNo, @Param("mailCycle") int mailCycle);

	@Delete("DELETE FROM Parentuser WHERE parent_no = #{parentNo}")
	void deleteMailCycle(int parentNo);

}
