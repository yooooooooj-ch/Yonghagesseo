package com.ddak.yongha.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.ddak.yongha.vo.TransferVO;

@Mapper
public interface TransferMapper {

	public void insertTransfer(TransferVO tvo);
	
	List<Map<String, Object>> getDailyTransferAmountAllAccounts();
}
