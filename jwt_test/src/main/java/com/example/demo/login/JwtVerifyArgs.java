
package com.example.demo.login;

import java.time.Duration;
import java.util.function.Function;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.jsonwebtoken.Claims;
import lombok.Data;

@Data
public class JwtVerifyArgs {
	// 고정 부분
	private Duration accessTokenDuration = Duration.ofSeconds(5);// 액세스 토큰 발급 또는 재발급 만료 시간
	private Duration refreshTokenDuration = Duration.ofSeconds(10);// 리프레시 토큰 만료 시간
	private Duration accessTokenRegenDuration = Duration.ofMinutes(3);// 액세스 토큰 재발급 위한 만료시간 체크
	private String accessTokenCookieName = "X-AC-TK";// 액세스 토큰 쿠키 키
	private String refreshTokenCookieName = "X-RF-TK";// 리프레시 토큰 쿠키 키
	private String cookieDomain;
	private String cookiePath = "/";
	private Function<Claims, String> compRefreshTokenFunc;
	// 파라미터 부분
	private HttpServletRequest httpRequest;
	private HttpServletResponse httpResponse;
	private SecretKey secretKey;
	private String accessToken;
	private String refreshToken;
	// 결과 부분 
	private String newAccessToken;
	private boolean isValid = false;
	private Claims claims;
	private Exception exception;
	private String message;
}