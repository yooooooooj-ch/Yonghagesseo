package com.ddak.yongha.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;
import java.util.Date;

@Mapper
public interface PredictionMapper {
    List<Map<String, Object>> getUserExpensesByParent(int parentNo);
    List<Map<String, Object>> getParentsToSend();
    void updateParentLastSendDate(int parent_no);

    Double getAutoTransferAmountByChild(int childNo);
    Integer getAutoTransferCycleByChild(int childNo);
    Date getAutoTransferLastDateByChild(int childNo);

    Double getChildCurrentBalance(int childNo);

    // 새로 추가: 현재 진행중인 목표 금액
    Double getChildTargetAmount(int childNo);
    
    List<Map<String, Object>> getParentsToSendTest();
}
