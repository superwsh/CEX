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
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

	@Value("${app.cors.allowed-origin-patterns}")
	private String[] allowedOriginPatterns;

	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();

		// 注意：allowCredentials=true 时不能使用 "*" 作为 allowedOrigin
		// 使用 addAllowedOriginPattern 代替 addAllowedOrigin("*")
		config.setAllowedOriginPatterns(Arrays.asList(allowedOriginPatterns)); // Spring 5.3+ 支持，适用于带凭证的请求
		config.setAllowCredentials(true);
		config.addAllowedHeader("*");
		config.addAllowedMethod("*");

		source.registerCorsConfiguration("/**", config);

		FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
		bean.setOrder(0); // 确保 CORS 过滤器优先执行
		return bean;
	}

//	@Bean
//	public FilterRegistrationBean corsFilter() {
//	     UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//	     CorsConfiguration config = new CorsConfiguration();
//	     config.addAllowedOrigin("*");
//	     config.setAllowCredentials(true);
//	     config.addAllowedHeader("*");
//	     config.addAllowedMethod("*");
//	     source.registerCorsConfiguration("/**", config);
//	     FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
//	     bean.setOrder(0);
//	     return bean;
//	}

}