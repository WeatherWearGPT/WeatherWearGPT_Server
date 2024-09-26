package com.weatherweargpt.jwt;

import com.weatherweargpt.config.AuthUser;
import com.weatherweargpt.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@Slf4j
public class AuthUserResolver implements HandlerMethodArgumentResolver {

	private final JWTUtil jwtUtil;

	public AuthUserResolver(JWTUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		boolean hasAnnotation = parameter.hasParameterAnnotation(AuthUser.class);
		boolean isUserEntityType = UserEntity.class.isAssignableFrom(parameter.getParameterType());

		return hasAnnotation && isUserEntityType;
	}

	@Override
	public UserEntity resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
									  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

		System.out.println("Authorization Header: " + authorizationHeader);

		if (authorizationHeader == null) {
			return null; // 여기서 null이 반환되므로 withdraw 메서드에서 처리 필요
		}

		String jwtToken = authorizationHeader.substring(7);

		System.out.println("JWT Token: " + jwtToken);

		try {
			UserEntity user = jwtUtil.getUser(jwtToken);

			System.out.println(user.getUserName()+"AuthUser");
			return user;
		} catch (IllegalArgumentException e) {
			log.error("JWT Token is invalid: {}", e.getMessage());
			return null; // JWT가 유효하지 않을 때도 null 반환
		}
	}

}