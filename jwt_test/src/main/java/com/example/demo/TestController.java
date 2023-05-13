package com.example.demo;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.demo.login.JwtArgs;
import com.example.demo.login.JwtConf;
import com.example.demo.login.JwtUtil;

@Controller
public class TestController {

	@RequestMapping("/")
	public Object index(String username, HttpServletRequest req) {
		if(! ObjectUtils.isEmpty(username)) {
			SecretKey secretKey = LoginFilter.secretKey;
			Map<String, Object> payload = new HashMap<>();
			payload.put("username", username);
			List<String> accessTokenList = JwtUtil.makeJwtTokenList(secretKey, payload, Arrays.asList(Duration.ofMinutes(1), Duration.ofMinutes(2)));
			HttpServletRequest servletRequest = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
			HttpServletResponse servletResponse = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
			JwtArgs args = JwtUtil.makeJwtArgs(new JwtConf(), servletRequest, servletResponse, secretKey);
			JwtUtil.sendJwtToken(args, accessTokenList.get(0), accessTokenList.get(1));
			return "redirect:/";
		}
		Map loginUser = (Map)req.getAttribute("loginUser");
		if(loginUser != null && ! ObjectUtils.isEmpty(loginUser.get("exp"))) {
			long exp = Double.valueOf(loginUser.get("exp").toString()).longValue() *1000;
			req.setAttribute("expDate", new Date(exp));			
		}
		return "/index";
	}
	
	@RequestMapping("/logout")
	public Object logout() {
		HttpServletRequest servletRequest = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		HttpServletResponse servletResponse = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
		JwtArgs args = JwtUtil.makeJwtArgs(new JwtConf(), servletRequest, servletResponse, null);
		JwtUtil.removeJwtToken(args);
		return "redirect:/";
	}
	
	@ResponseBody
	@RequestMapping("/rest/getMap1")
	public Object getMap1(
			@RequestHeader Map headers,
			@RequestParam Map params
			) {
		Map map1 = new HashMap();
		map1.put("가낟", "ㄹㅇㄴㄹㄴㅇㄹ");
		map1.put("ㅇㄴㄹㄴㄹㅇㄴ", "ㄹㄴㅇㄱㄹㄴㅇㅇㄴㄹㄴㅇㄹ");
		return map1;
	}

	@ResponseBody
	@RequestMapping("/rest/callGetMap1")
	public Object callGetMap1() {
		//HttpComponentsClientHttpReuestFactory factory = new HttpComponentsClientHttpRequestFactory();
		
		Duration.ofSeconds(1).toMillisPart();
		
		//Duration.ofSeconds(1).toMillis()
		
		//factory.setConnectTimeout();
		//factory.setReadTimeout(Duration.ofSeconds(10).toMillis());

		return null;



	}

}
