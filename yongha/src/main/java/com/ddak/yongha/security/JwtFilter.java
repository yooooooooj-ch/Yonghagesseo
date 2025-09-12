package com.ddak.yongha.security;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 필터는 bean이 아니므로 주입x
public class JwtFilter extends OncePerRequestFilter {
	
	private final JwtUtil jwtUtil;	
	
	public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    private static final String BEAR2 = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String uri = request.getRequestURI();

        // 리프레시/로그인/로그아웃 등은 기존처럼 필터 통과
        if (uri.equals("/api/user/refresh")
                || uri.equals("/api/user/login")
                || uri.equals("/api/user/logout")) {
            chain.doFilter(request, response);
            return;
        }

        // 3) 나머지는 통과 (기존 유지)
        chain.doFilter(request, response);
    }
}
