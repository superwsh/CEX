package com.bizzan.bitrade.config;

import com.aliyuncs.endpoint.ResolveEndpointRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class ActuatorSecurityConfig {

	private final Environment env;

	public ActuatorSecurityConfig(Environment env) {
		this.env = env;
	}

	@Bean
	public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
		// 新属性：management.endpoints.web.base-path（默认 /actuator）
		String basePath = env.getProperty("management.endpoints.web.base-path", "/actuator");

		// 确保以 / 开头
		if (!basePath.startsWith("/")) {
			basePath = "/" + basePath;
		}

		// 构造合法路径：如 /actuator/**
		String actuatorPattern = basePath + "/**";

		http
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(authz -> authz
						.requestMatchers(actuatorPattern).authenticated()
						.anyRequest().permitAll()
				)
				.httpBasic(httpBasic -> httpBasic.realmName("Actuator Realm"));

		return http.build();
	}

//	@Override
//	protected void configure(HttpSecurity http) throws Exception {
//		String contextPath = env.getProperty("management.context-path");
//		if (StringUtils.isEmpty(contextPath)) {
//			contextPath = "";
//		}
//		http.csrf().disable();
//		http.authorizeRequests().antMatchers("/**" + contextPath + "/**").authenticated().anyRequest().permitAll().and()
//				.httpBasic();
//	}
}