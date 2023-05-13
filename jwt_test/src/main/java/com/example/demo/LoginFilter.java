package com.example.demo;

import java.io.IOException;

import javax.crypto.SecretKey;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.login.JwtUtil;
import com.example.demo.login.JwtVerifyArgs;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class LoginFilter extends OncePerRequestFilter{

	public static SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		JwtVerifyArgs args = JwtUtil.makeJvArgs(request, response, secretKey);
		JwtUtil.verifyAndSlidingInFilter(args);

//		args.setCompRefreshTokenFunc(t -> args.getRefreshToken());
//		JwtUtil.verifyAndRefreshInFilter(args);
		System.out.println(args);

		if(args.getClaims() != null) {
			request.setAttribute("loginUser", args.getClaims());
		}
		
		
		filterChain.doFilter(request, response);
		
	}
	
}