package com.ddak.yongha.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ddak.yongha.security.JwtFilter;
import com.ddak.yongha.security.JwtUtil;

import lombok.RequiredArgsConstructor;

// Spring Security 설정
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	
	private final JwtUtil jwtUtil;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		JwtFilter jwtFilter = new JwtFilter(jwtUtil);
		
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())  // 폼 로그인 비활성화
                .logout(logout -> logout.disable()) // 기보 로그아웃 비활성화
                .httpBasic(httpBasic -> httpBasic.disable()) // Basic 인증 비활성화
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login","/api/user/**", "/css/**", "/js/**", "/index", "/", "/**").permitAll() // 로그인, static은 허용
                        .requestMatchers(HttpMethod.GET, "/join-family").permitAll() // 토큰 검증 페이지 진입은 누구나 가능
                        .requestMatchers(HttpMethod.POST, "/join-family/accept", "/join-family/reject").authenticated() 
                        .anyRequest().authenticated()  // 나머지는 인증 필요
                )
//            .addFilterBefore(new JwtFilter(), UsernamePasswordAuthenticationFilter.class); // new JwtFilter 로 객체 등록
                // : 나중에 동적인 관리가 동원되는 경우(service단 추가 등) 아래처럼 DI(주입)하는 방식을 사용해야 함
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class); // JwtFilter 주입 (DI)
        return http.build();
    }
}