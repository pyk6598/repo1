package com.example.demo.login;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.util.URLEncoder;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.WebUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;


public abstract class JwtUtil {
	/**
	 * 토큰 생성
	 * payload (토큰의 body)는 사용자아이디 등 사용자 식별값을 키=값 형태로 넘겨준다.
	 */
	protected static String makeJwtToken(SecretKey secretKey, Duration expDuration, Map<String, Object> payload) {
		Date now = new Date();
		Claims claims = Jwts.claims();
		claims.putAll(new HashMap<>(payload));
		claims.setIssuedAt(now);
		claims.setExpiration(new Date(now.getTime() + expDuration.toMillis()));
		JwtBuilder jb = Jwts.builder();
		jb.setClaims(claims);
		jb.signWith(secretKey);
		return jb.compact();
	}

	/**
	 * 클라이언트에서 넘겨준 토큰 검증
	 * 서명 검증하여 위조되지 않았는지 SignatureException
	 * 만료시간 지나지 않았는지 ExpiredJwtException
	 */
	protected static Jws<Claims> verifyJwtToken(SecretKey secretKey, String token) {
		Jws<Claims> jws = Jwts.parserBuilder() // (1)
				.setSigningKey(secretKey) // (2)
				.build() // (3)
				.parseClaimsJws(token); // (4)
		return jws;
	}

	/**
	 * 액세스 토큰, 리프레시 토큰 생성
	 * 인증서버에서 로그인 성공하면
	 * payload 에 아이디등 입력하여 token 생성
	 * 클라이언트에 쿠키 등으로 전달
	 * 리프레시 토큰 은 디비 등에 저장해 놓고
	 * 클라이언트에서 받은 리프레시 토큰 검증시 저장된 값과 비교해야한다.
	 */
	public static List<String> makeJwtTokenList(SecretKey secretKey, Map<String, Object> payload, List<Duration> durList){
		List<String> jwtTokenList = new ArrayList<>();
		for(Duration dur: durList) {
			jwtTokenList.add(makeJwtToken(secretKey, dur, payload));
		}
		return jwtTokenList;
	}

	/**
	 * 쿠키 만들기
	 */
	protected static Cookie makeCookie(JwtArgs args, String name) {
		String cookieDomain = args.getCookieDomain();
		String cookiePath = args.getCookiePath();
		Cookie cookie = new Cookie(name, null);
		cookie.setHttpOnly(true);
		if(! ObjectUtils.isEmpty(cookieDomain))cookie.setDomain(cookieDomain);
		if(! ObjectUtils.isEmpty(cookiePath))cookie.setPath(cookiePath);
		return cookie;
	}
	/**
	 * 쿠키 보내기
	 */
	protected static void addCookie(JwtArgs args, String name, String value) {
		Cookie cookie = makeCookie(args, name);
		cookie.setValue(new URLEncoder().encode(value, StandardCharsets.UTF_8));
		cookie.setMaxAge(-1);// 세션 쿠키
		args.getHttpResponse().addCookie(cookie);
	}
	/**
	 * 쿠키 지우기
	 */
	protected static void removeCookie(JwtArgs args, String name) {
		Cookie cookie = makeCookie(args, name);
		cookie.setValue("");
		cookie.setMaxAge(0);// 쿠키 삭제
		args.getHttpResponse().addCookie(cookie);
	}
	/**
	 * 쿠키값
	 */
	public static String getCookieValue(JwtArgs args, String name) {
		Cookie cookie = WebUtils.getCookie(args.getHttpRequest(), name);
		if(cookie == null)return null;
		String cookieValue = cookie.getValue();
		if(cookieValue == null)return null;
		return URLDecoder.decode(cookieValue, StandardCharsets.UTF_8);
	}
	/**
	 * 인증서버에서 아이디 비번 정상이면 아래 호출
	 */
	public static void sendJwtToken(JwtArgs args, String accessToken, String refreshToken) {
		if(! ObjectUtils.isEmpty(accessToken)) {
			addCookie(args, args.getAccessTokenCookieName(), accessToken);
		}
		if(! ObjectUtils.isEmpty(refreshToken)) {
			addCookie(args, args.getRefreshTokenCookieName(), refreshToken);
		}
	}
	
	/**
	 * 쿠키 삭제. 로그아웃 처리
	 */
	public static void removeJwtToken(JwtArgs args) {
		removeCookie(args, args.getAccessTokenCookieName());
		removeCookie(args, args.getRefreshTokenCookieName());
	}

	/**
	 * 필터에서 수행하는 로그인 검증(= 토큰 검증)
	 * 액세스 토큰 만료시 리프레시 토큰 검증하여 액세스 토큰 재발급
	 */
	protected static void verifyAndRefresh(JwtArgs args) {
		String accessToken = args.getAccessToken();
		String refreshToken = args.getRefreshToken();
		SecretKey secretKey = args.getSecretKey();
		Function<Claims, String> compRefreshTokenFunc = args.getCompRefreshTokenFunc();
		Duration accessTokenDuration = args.getAccessTokenDuration();
		if(ObjectUtils.isEmpty(accessToken))return;
		if(ObjectUtils.isEmpty(refreshToken))return;
		boolean isAccessTokenExpired = false;
		Jws<Claims> jws = null;
		try {
			jws = verifyJwtToken(secretKey, accessToken);
			// 액세스 토큰 검증 = 로그인 검증 
			args.setClaims(jws.getBody());
			args.setValid(true);
			return;
		}catch(ExpiredJwtException expiredAccess) {
			// 액세스 토큰 만료로 재발급 필요
			isAccessTokenExpired = true;
		}catch(Exception error){
			// 그 외 에러는 로그인 검증 실패이므로 에러 리턴
			args.setValid(false);
			args.setException(error);
			return;
		}
		// 아래부분은 액세스 토큰 만료되었을 경우만 실행된다.
		try {
			jws = verifyJwtToken(secretKey, refreshToken);
		}catch(Exception error) {
			args.setValid(false);
			args.setException(error);
			return;
		}
		// 리프레시 토큰으로 액세스 토큰 재발급
		// 리프레시 토큰은 반드시 서버에 저장된 값과 비교해야 한다.
		String compRefreshToken = compRefreshTokenFunc.apply(jws.getBody());
		if(! refreshToken.equals(compRefreshToken)) {
			// 서버의 리프레시 토큰과 틀리면 리턴
			args.setValid(false);
			args.setMessage("refresh token is not matched");
			return;
		}
		// 액세스토큰 재발급
		Claims payload = jws.getBody();
		String newAccessToken = makeJwtToken(secretKey, accessTokenDuration, payload);
		args.setValid(true);
		args.setClaims(payload);
		args.setNewAccessToken(newAccessToken);
	}

	/**
	 * 필터에서 수행하는 로그인 검증(= 토큰 검증)
	 * 리프레시 토큰 대신 액세스 토큰 만료전 재발급 수행
	 */
	protected static void verifyAndSliding(JwtArgs args) {
		String accessToken = args.getAccessToken();
		if(ObjectUtils.isEmpty(accessToken))return;
		SecretKey secretKey = args.getSecretKey();
		Duration accessTokenDuration = args.getAccessTokenDuration();
		try {
			Jws<Claims> jws = verifyJwtToken(secretKey, accessToken);
			// 액세스 토큰 검증 = 로그인 검증 
			Date expireDate = jws.getBody().getExpiration();
System.out.println(String.format("expireDate %s", expireDate));
			long expireTime = expireDate.getTime() - System.currentTimeMillis();
			Claims payload = jws.getBody();
			if(expireTime < args.getAccessTokenRegenDuration().toMillis()) {
				// 액세스 토큰 만료 30분 밖에 안남았다면 1시간 연장
				String newAccessToken = JwtUtil.makeJwtToken(secretKey, accessTokenDuration, payload);
				args.setNewAccessToken(newAccessToken);
			}
			args.setClaims(payload);
			args.setValid(true);
			return;
		}catch(Exception error){
			// 그 외 에러는 로그인 검증 실패이므로 에러 리턴
			args.setValid(false);
			args.setException(error);
			return;
		}
	}

	/**
	 * verify 후처리. 쿠키 처리
	 */
	protected static void doCookieProcess(JwtArgs args) {
		String accessTokenCookieName = args.getAccessTokenCookieName();
		String refreshTokenCookieName = args.getRefreshTokenCookieName();
		String accessToken = args.getAccessToken();
		String refreshToken = args.getRefreshToken();
		String newAccessToken = args.getNewAccessToken();
		if(args.isValid()) {
			// 액세스 토큰 재발급
			if(! ObjectUtils.isEmpty(newAccessToken)) {
				addCookie(args, accessTokenCookieName, newAccessToken);
			}
		} else {
			// 정상 아닌데 토큰 있으면 지우자
			if(! ObjectUtils.isEmpty(accessToken)) {
				removeCookie(args, accessTokenCookieName);
			}
			if(! ObjectUtils.isEmpty(refreshToken)) {
				removeCookie(args, refreshTokenCookieName);
			}
		}
	}

	/**
	 * 필터에서 수행할 로직
	 */
	public static void verifyAndRefreshInFilter(JwtArgs args) {
		verifyAndRefresh(args);
		doCookieProcess(args);
	}

	/**
	 * 필터에서 수행할 로직
	 */
	public static void verifyAndSlidingInFilter(JwtArgs args) {
		verifyAndSliding(args);
		doCookieProcess(args);
	}
	
	public static JwtArgs makeJwtArgs(HttpServletRequest request, HttpServletResponse response, SecretKey secretKey) {
		JwtArgs args = new JwtArgs();
		//args.setCookieDomain("pyk.net");
		args.setHttpRequest(request);
		args.setHttpResponse(response);
		String accessToken = JwtUtil.getCookieValue(args, args.getAccessTokenCookieName());
		String refreshToken = JwtUtil.getCookieValue(args, args.getRefreshTokenCookieName());
		args.setAccessToken(accessToken);
		args.setRefreshToken(refreshToken);
		args.setSecretKey(secretKey);
		return args;
	}

	public static void main(String[] args) {
		SecretKey sKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
		String token1 = makeJwtToken(sKey, Duration.ofSeconds(5), null);
		
		verifyJwtToken(sKey, token1);
		
	}

}
