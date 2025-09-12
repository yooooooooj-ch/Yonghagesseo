package com.ddak.yongha.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ddak.yongha.security.JwtInviteUtil;

@Configuration
public class InviteJwtConfig {
  @Bean
  JwtInviteUtil jwtInviteUtil(
     @Value("${invite.jwt.secret}") String secret,
     @Value("${invite.jwt.issuer}") String issuer,
     @Value("${invite.jwt.audience}") String audience,
     @Value("${invite.jwt.ttl-hours}") long ttlHours) {
    return new JwtInviteUtil(secret, issuer, audience, ttlHours);
  }
}
