
package com.example.demo.login;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.jsonwebtoken.Claims;
import lombok.Data;

@Data
public class JwtArgs {
	private JwtConf conf;
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