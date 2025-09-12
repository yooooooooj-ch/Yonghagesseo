package com.ddak.yongha.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CodeMapper {
	List<Map<String, Object>> selectCodesByType(@Param("typeName") String typeName);

}
