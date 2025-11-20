package org.unilab.improfessorbe.global.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.unilab.improfessorbe.domain.user.service.CustomOAuth2UserService;
import org.unilab.improfessorbe.global.security.jwt.JwtAuthenticationFilter;
import org.unilab.improfessorbe.global.security.jwt.JwtExceptionFilter;
import org.unilab.improfessorbe.global.security.jwt.JwtTokenProvider;
import org.unilab.improfessorbe.global.security.oauth2.handler.OAuth2LoginFailureHandler;
import org.unilab.improfessorbe.global.security.oauth2.handler.OAuth2LoginSuccessHandler;
import org.unilab.improfessorbe.global.util.RedisUtil;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtTokenProvider jwtTokenProvider;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler(JwtTokenProvider jwtTokenProvider, RedisUtil redisUtil) {
		return new OAuth2LoginSuccessHandler(jwtTokenProvider, redisUtil);
	}

	@Bean
	public OAuth2LoginFailureHandler oAuth2LoginFailureHandler() {
		return new OAuth2LoginFailureHandler();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
		CustomOAuth2UserService customOAuth2UserService, OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
		OAuth2LoginFailureHandler oAuth2LoginFailureHandler) throws Exception {
		httpSecurity
			.csrf(csrf -> csrf.disable())
			.httpBasic(httpBasic -> httpBasic.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/", "/api/users/register", "/api/users/login/**", "/api/users/refresh-token",
					"/api/users/email/send-verification",
					"/api/users/email/verify", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", "/oauth2/**",
					"/login/oauth2/**", "/favicon.ico")
				.permitAll() // 테스트용 잠시 "/api/test/public", "/index.html", "api/test/logout/success"
				.requestMatchers("/admin")
				.hasRole("ADMIN")
				.anyRequest()
				.authenticated()
			)
			.oauth2Login(oauth2 -> oauth2
				.userInfoEndpoint(userInfo -> userInfo
					.userService(customOAuth2UserService)
				)
				.successHandler(oAuth2LoginSuccessHandler)
				.failureHandler(oAuth2LoginFailureHandler)
			)
			.formLogin(form -> form.disable())
			.logout(logout -> logout.permitAll())
			.cors(configurer -> configurer.configurationSource(corsConfigurationSource()));

		httpSecurity.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
				UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(new JwtExceptionFilter(), JwtAuthenticationFilter.class);

		return httpSecurity.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.addAllowedHeader("*");
		corsConfiguration.setAllowedMethods(Collections.singletonList("*"));
		corsConfiguration.setAllowCredentials(true);
		corsConfiguration.setAllowedOrigins(
			List.of("http://localhost:5173", "https://www.improfessor.co.kr",
				"https://improfessor-fe-nine.vercel.app")
		);

		corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", corsConfiguration);
		return source;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws
		Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}
}


