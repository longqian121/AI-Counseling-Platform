package org.example.aispringboot.config;

import org.example.aispringboot.util.JwtAuthticationFilter;
import org.example.aispringboot.common.ResultCode;
import org.example.aispringboot.enumClass.UserType;
import org.example.aispringboot.util.ResponseUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
	private static final String ADMIN = UserType.ADMIN.getCode().toString();
	private static final String USER = UserType.USER.getCode().toString();
	private static final String DOCTOR = UserType.COUNSELOR.getCode().toString();

	@PostConstruct
	public void init() {
		SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
	}

	public static boolean isPublicRequest(HttpServletRequest request) {
		String path = request.getRequestURI();
		String method = request.getMethod();
		if ("OPTIONS".equals(method)) {
			return true;
		}
		if ("GET".equals(method)) {
			return "/".equals(path) || "/api/test".equals(path)
					|| "/api/knowledge/category/tree".equals(path)
					|| "/api/knowledge/article/page".equals(path)
					|| path.matches("/api/knowledge/article/[^/]+")
					|| path.startsWith("/uploads/");
		}
		return "POST".equals(method)
				&& ("/api/user/login".equals(path) || "/api/user/add".equals(path));
	}

	@Bean
	public JwtAuthticationFilter jwtAuthenticationFilter(){
		return new JwtAuthticationFilter();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				// 关闭csrf
				.csrf(AbstractHttpConfigurer::disable)
				// 闁板秶鐤嗘导姘崇樈缁狅紕鎮婃稉鐑樻￥閻樿埖鈧?
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				//配置请求的授权规??
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/", "/api/test", "/api/knowledge/category/tree",
								"/api/knowledge/article/page", "/api/knowledge/article/*", "/uploads/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/user/login", "/api/user/add").permitAll()
						.requestMatchers("/api/admin/**", "/api/data-analytics/**",
								"/api/psychological-chat/admin/**", "/api/emotion-diary/admin/**",
								"/api/knowledge/**", "/api/file/upload").hasRole(ADMIN)
						.requestMatchers("/api/doctor/**").hasRole(DOCTOR)
						.requestMatchers(HttpMethod.GET, "/api/psychological-chat/sessions/*/messages")
								.hasAnyRole(USER, ADMIN)
						.requestMatchers("/api/psychological-chat/**", "/api/emotion-diary/**",
								"/api/appointment/**", "/api/consultation/**").hasRole(USER)
						.requestMatchers(HttpMethod.GET, "/api/user/current").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/user/logout").authenticated()
						.anyRequest().denyAll()
				)
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) ->
								ResponseUtil.writeError(response, ResultCode.ACCESS_UNAUTHORIZED))
						.accessDeniedHandler((request, response, exception) ->
								ResponseUtil.writeError(response, ResultCode.AUTHORIZED_ERROR)))
				//添加JWT认证过滤??
				.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
				return http.build();
	}
}
