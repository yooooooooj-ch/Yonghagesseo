package com.ddak.yongha.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FcmTestUserMapper {

    /**
     * 자녀 계좌 ID로 부모의 FCM 토큰을 조회
     * @param childAccountId 자녀의 account_id
     * @return 부모의 fcm_token
     */
    String getParentFcmTokenByChildAccountId(String childAccountId);

    /**
     * 자녀 계좌 ID로 자녀의 FCM 토큰을 조회
     * @param childAccountId 자녀의 account_id
     * @return 자녀의 fcm_token
     */
    String getChildFcmTokenByChildAccountId(String childAccountId); 
}
