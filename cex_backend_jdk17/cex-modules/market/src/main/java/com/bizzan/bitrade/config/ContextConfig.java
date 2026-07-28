package com.bizzan.bitrade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 浏览器的 CORS 规则要求 —— 如果前端需要携带 Cookie/Token 发起跨域请求，
 * 	服务端必须明确指定允许的域名（如 https://xxx.com），
 * 	而不能用 *；Spring 3.5.x 强化了这个校验，直接抛出异常。
 */
@Configuration
public class ContextConfig {

	@Value("${app.cors.allowed-origin-patterns}")
	private String[] allowedOriginPatterns;

	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilter() {
		CorsConfiguration config = new CorsConfiguration();
		// 1. 禁用allowedOrigins，仅使用allowedOriginPatterns（支持通配符+credentials）
		config.setAllowedOriginPatterns(Arrays.asList(allowedOriginPatterns));
		// 2. 允许携带凭证（Cookie/Token）
		config.setAllowCredentials(true);
		// 3. 允许所有请求头
		config.addAllowedHeader(CorsConfiguration.ALL);
		// 4. 允许所有HTTP方法（GET/POST/PUT/DELETE等）
		config.addAllowedMethod(CorsConfiguration.ALL);
		// 5. 预检请求缓存时间（秒），减少OPTIONS请求次数
		config.setMaxAge(3600L);
		// 注册CORS配置到所有路径
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);

		FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
		bean.setOrder(0); // 确保 CORS 过滤器优先级最高
		return bean;
	}
}