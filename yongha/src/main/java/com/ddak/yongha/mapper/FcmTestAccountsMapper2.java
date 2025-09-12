package com.ddak.yongha.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ddak.yongha.vo.FcmTestTransferVO;

@Mapper
public interface FcmTestAccountsMapper2 {

    Long selectBalanceByAccountId(@Param("accountId") String accountId);     // ✅
    Long selectAccountNoByAccountId(@Param("accountId") String accountId);   // ✅

    int updateChildBalance(FcmTestTransferVO vo);
    int updateParentBalance(FcmTestTransferVO vo);
    int insertTransfer(FcmTestTransferVO vo);
}
