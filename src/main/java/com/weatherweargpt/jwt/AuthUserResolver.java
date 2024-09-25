package com.weatherweargpt.jwt;

import com.weatherweargpt.config.AuthUser;
import com.weatherweargpt.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
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

	@Override // JwtFilter에서 모두 검증하므로, 검증 로직은 추가하지 않음
    public UserEntity resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
								 NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
    	HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
    	String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    	
    	if(authorizationHeader == null)
    		return null;
    	
    	String jwtToken = authorizationHeader.substring(7);
    	UserEntity user = jwtUtil.getUser(jwtToken);

        return user;
    }

}