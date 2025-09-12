package com.ddak.yongha.security;

import java.security.Key;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

// JWT - 토큰 생성, 파싱, 검증하는 클래스
@Component
public class JwtUtil {
	
	private final SecretKey key;

    public JwtUtil(SecretKey key) { // JwtConfig 에서 주입
        this.key = key;
    }
    
//    private static final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256); // 비밀키(랜덤)
    private static final long EXPIRATION_TIME = 1000 * 60 * 60; // 토큰(access_token) 만료시간: 60분 'Access'
//    private static final long EXPIRATION_TIME = 1000 * 30; // 토큰(access_token) 만료시간: 30초 (for test) 'Access'
    private static final long REFRESH_EXPIRATION_TIME = 1000L * 60 * 60 * 24 * 7; // 갱신토큰(refresh_token) 만료시간: 7일 
//    private static final long REFRESH_EXPIRATION_TIME = 1000L * 60 * 1; // 갱신토큰(refresh_token) (for test) 만료시간: 1분 

    /*
     * 토큰 생성
    */
    public String createToken(int user_no, String user_id) {
        return Jwts.builder()
        		.setSubject(String.valueOf(user_no)) // 토큰 식별자'sub' : user_no (PK)
        		.claim("user_id", user_id) 
//                .claim(AUTHORIZATION_KEY, role) // 사용자 권한 
                .setIssuedAt(new Date()) // 토큰 발행 시각
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))  // 만료 시각
                .signWith(key) // 서명 (HMAC-SHA256 + key) - HMAC-SHA256 방식으로 서명 - 암호화 알고리즘
                .compact(); // 문자열 토큰 생성
    }
    
	/*
	 * 리프레쉬 토큰 생성
	*/
    public String createRefreshToken(int user_no, String user_id) {
        return Jwts.builder()
                .setSubject(String.valueOf(user_no)) // 토큰 식별자'sub' : user_no (PK)     
                .claim("user_id", user_id)                    //  access_token과 동일 claim 유지할 경우 (유저아이디 클레임 추가)
//                .claim("token_type", "refresh")            //토큰끼리 구분이 필요할 경우
                .setIssuedAt(new Date()) // 토큰 발행 시각
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION_TIME))  // 만료 시각
                .signWith(key)
                .compact();
    }
    
	/*
	 * 토큰의 Claim(사용자 정보) 추출
	*/
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token.replace("Bearer ", ""))
            .getBody();
    }

	public Key getKey() {
		return key;
	}

}

