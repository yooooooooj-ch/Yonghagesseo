package com.ddak.yongha.config;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Configuration
public class JwtConfig {
  @Value("${jwt.secret.base64}") String secretBase64;

  @Bean
  public SecretKey jwtSigningKey() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
  }
}