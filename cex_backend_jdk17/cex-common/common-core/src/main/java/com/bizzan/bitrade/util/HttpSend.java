package com.bizzan.bitrade.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.util.Map;

/**
 * @author tansitao
 * @time 2018-04-05
 * http短信接口访问工具
 */
public class HttpSend {

	private static final Logger log = LoggerFactory.getLogger(HttpSend.class);

	// 静态初始化RestTemplate（仅初始化一次，线程安全）
	private static final RestTemplate REST_TEMPLATE;

	// 静态代码块初始化RestTemplate（配置超时、编码等参数）
	static {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		// 连接超时：5秒（适配JDK17+的Duration，替代硬编码毫秒）
		factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
		// 读取超时：10秒
		factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
		// 创建RestTemplate实例（全局唯一，线程安全）
		REST_TEMPLATE = new RestTemplate(factory);
	}

	/**
	 * 短信接口POST请求（保留原静态方法名和入参，完全兼容原有调用逻辑）
	 * @param url       提交的URL
	 * @param paramsMap 提交<参数，值>Map
	 * @return 提交响应字符串
	 */
	public static String yunpianPost(String url, Map<String, String> paramsMap) {
		// 1. 基础参数校验
		if (url == null || url.trim().isEmpty()) {
			log.error("POST请求失败：URL为空");
			return "";
		}

		try {
			// 2. 构建请求头（固定UTF-8编码，解决中文乱码）
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			headers.set("Charset", "UTF-8");
			headers.set("User-Agent", "Mozilla/5.0 (compatible; Spring Boot3.x RestTemplate)");

			// 3. 转换参数为MultiValueMap（适配form表单提交）
			MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
			if (paramsMap != null && !paramsMap.isEmpty()) {
				for (Map.Entry<String, String> param : paramsMap.entrySet()) {
					// 过滤空参数，避免接口异常
					if (param.getValue() != null && !param.getValue().trim().isEmpty()) {
						formParams.add(param.getKey(), param.getValue());
					}
				}
			}

			// 4. 构建HTTP请求实体
			HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formParams, headers);

			// 5. 执行POST请求（核心：静态RestTemplate调用）
			ResponseEntity<String> responseEntity = REST_TEMPLATE.postForEntity(url, requestEntity, String.class);

			// 6. 校验响应状态并返回结果
			if (responseEntity.getStatusCode().is2xxSuccessful()) {
				String responseBody = responseEntity.getBody() == null ? "" : responseEntity.getBody().trim();
				log.info("POST请求成功 | URL：{} | 响应：{}", url, responseBody);
				return responseBody;
			} else {
				log.error("POST请求失败 | URL：{} | 状态码：{} | 响应：{}",
						url, responseEntity.getStatusCodeValue(), responseEntity.getBody());
				return "";
			}

		} catch (RestClientException e) {
			// 捕获所有RestTemplate异常（超时、连接失败、解析异常等）
			log.error("POST请求异常 | URL：{}", url, e);
			return "";
		}
	}
	
	/**
     * 基于HttpClient 4.3的通用POST方法
     *
     * @param url       提交的URL
     * @param paramsMap 提交<参数，值>Map
     * @return 提交响应
     */
//	public static String yunpianPost(String url, Map<String, String> paramsMap) {
//		HttpClient client = new HttpClient();
//		try {
//			PostMethod method = new PostMethod(url);
//			if (paramsMap != null) {
//				NameValuePair[] namePairs = new NameValuePair[paramsMap.size()];
//				int i = 0;
//				for (Map.Entry<String, String> param : paramsMap.entrySet()) {
//					NameValuePair pair = new NameValuePair(param.getKey(),
//							param.getValue());
//					namePairs[i++] = pair;
//				}
//				method.setRequestBody(namePairs);
//				HttpMethodParams param = method.getParams();
//				param.setContentCharset("utf-8");
//			}
//			client.executeMethod(method);
//			return method.getResponseBodyAsString();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return "";
//	}

}
