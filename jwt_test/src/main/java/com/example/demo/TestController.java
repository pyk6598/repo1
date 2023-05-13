package com.example.demo;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.demo.login.JwtUtil;
import com.example.demo.login.JwtVerifyArgs;

@Controller
public class TestController {

	@RequestMapping("/")
	public Object index(String username) {
		if(! ObjectUtils.isEmpty(username)) {
			SecretKey secretKey = LoginFilter.secretKey;
			Map<String, Object> payload = new HashMap<>();
			payload.put("username", username);
			List<String> accessTokenList = JwtUtil.makeJwtTokenList(secretKey, payload, Arrays.asList(Duration.ofMinutes(5)));
			JwtVerifyArgs args = new JwtVerifyArgs();
			
			HttpServletRequest servletRequest = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
			HttpServletResponse servletResponse = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();

			args.setHttpRequest(servletRequest);
			args.setHttpResponse(servletResponse);
			JwtUtil.sendJwtToken(args, accessTokenList.get(0), null);
			return "redirect:/";
		}
		return "/index";
	}
}
