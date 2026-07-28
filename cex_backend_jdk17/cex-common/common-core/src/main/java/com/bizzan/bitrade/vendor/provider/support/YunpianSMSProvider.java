package com.bizzan.bitrade.vendor.provider.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bizzan.bitrade.util.HttpSend;
import com.bizzan.bitrade.util.MessageResult;
import com.bizzan.bitrade.vendor.provider.SMSProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author tansitao
 * @time 2018-04-05
 * 云片短信验证码发送类
 */
@Slf4j
public class YunpianSMSProvider implements SMSProvider {

    private String gateway;
    private String apikey;

    public YunpianSMSProvider(String gateway, String apikey) {
        this.gateway = gateway;
        this.apikey = apikey;
    }

    private static Pattern RESPONSE_PATTERN = Pattern.compile("<response><error>(-?\\d+)</error><message>(.*[\\\\u4e00-\\\\u9fa5]*)</message></response>");


    public static String getName() {
        return "yunpian";
    }

    @Override
    public MessageResult sendSingleMessage(String mobile, String content) throws Exception {
        Map<String, String> params = new HashMap<String, String>();
        params.put("apikey", apikey);
        params.put("text", content);
        params.put("mobile", mobile);
        log.info("yunpianParameters====", params.toString());
        String resultXml= HttpSend.yunpianPost(gateway, params);
        log.info("result = {}", resultXml);
        return parseXml(resultXml);
    }

    @Override
    public MessageResult sendMessageByTempId(String mobile, String content, String templateId) throws Exception {
        return null;
    }

    @Override
    public MessageResult sendInternationalMessage(String mobile, String content) throws Exception {
        content = String.format("【djw】Your verification code is %s", content);
        Map<String, String> params = new HashMap<String, String>();
        params.put("apikey", apikey);
        params.put("text", content);
        params.put("mobile", mobile);
        String resultXml= HttpSend.yunpianPost(gateway, params);
        log.info("result = {}", resultXml);
        return parseXml(resultXml);
    }

    /**
     * 获取验证码信息格式
     *
     * @param code
     * @return
     */
    @Override
    public String formatVerifyCode(String code) {
        return String.format("【djw】您的验证码为：%s，请按页面提示填写，切勿泄露于他人。", code);
    }

    @Override
    public MessageResult sendVerifyMessage(String mobile, String verifyCode) throws Exception {
        String content = formatVerifyCode(verifyCode);
        return sendSingleMessage(mobile, content);
    }

    private MessageResult parseXml(String xml) {
        // 1. 直接解析字符串为 JSONObject
        JSONObject myJsonObject = JSON.parseObject(xml);

        // 2. 获取 code，fastjson2 提供更安全的 getIntValue，防止 null 转 int 报错
        int code = myJsonObject.getIntValue("code");

        MessageResult result = new MessageResult(500, "系统错误");

        if (code == 0) {
            result.setCode(code);
            // 3. 获取 msg 字符串
            result.setMessage(myJsonObject.getString("msg"));
        }

        //json lib 过时
//        JSONObject myJsonObject = JSONObject.fromObject(xml);
//        int code = myJsonObject.getInt("code");
//        MessageResult result = new MessageResult(500, "系统错误");
//        if(code == 0)
//        {
//            result.setCode(code);
//            result.setMessage(myJsonObject.getString("msg"));
//        }
        return result;
    }

	@Override
	public MessageResult sendCustomMessage(String mobile, String content) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
