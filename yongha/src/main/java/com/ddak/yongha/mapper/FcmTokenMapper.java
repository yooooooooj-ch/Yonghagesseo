package com.ddak.yongha.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FcmTokenMapper {

    // fcm_token 저장
    int updateFcmToken(@Param("user_no") int user_no,
                       @Param("fcm_token") String token);

    // fcm_token 조회
    String findTokenByUserNo(@Param("userNo") int userNo);
}
