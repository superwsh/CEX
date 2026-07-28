package com.bizzan.bitrade.controller;

import com.alibaba.fastjson2.JSONObject;
import com.bizzan.bitrade.controller.BaseController;
import com.bizzan.bitrade.system.GeetestLib;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.httpclient.HttpClient;
//import org.apache.commons.httpclient.HttpException;
//import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
//import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;

/**
 * @author GS
 * @date 2018年02月23日
 */
@RestController
@Slf4j
public class GeetestController extends BaseController {

    @Autowired
    private GeetestLib gtSdk;
    @Value("${water.proof.app.id}")
    private  String appId;
    @Value("${water.proof.app.secret.key}")
    private  String appSecretKey ;
    private static final String url = "https://ssl.captcha.qq.com/ticket/verify";


    @Autowired
    private RestTemplate restTemplate;


    @RequestMapping(value = "/start/captcha")
    public String startCaptcha(HttpServletRequest request) {
        String resStr = "{}";
        String userid = "spark";
        //自定义参数,可选择添加
        HashMap<String, String> param = new HashMap<String, String>();
        String ip = getRemoteIp(request);
        param.put("user_id", userid); //网站用户id
        param.put("client_type", "web"); //web:电脑上的浏览器；h5:手机上的浏览器，包括移动应用内完全内置的web_view；native：通过原生SDK植入APP应用的方式
        param.put("ip_address", ip); //传输用户请求验证时所携带的IP
        //进行验证预处理
        int gtServerStatus = gtSdk.preProcess(param);
        //将服务器状态设置到session中
        request.getSession().setAttribute(gtSdk.gtServerStatusSessionKey, gtServerStatus);
        //将userid设置到session中
        request.getSession().setAttribute("userid", userid);
        resStr = gtSdk.getResponseStr();
        return resStr;
    }

    public  Boolean  watherProof( String ticket,  String randStr, String ip) throws Exception {
        String response = null;
        Boolean responseBool = false;
        try {
            log.info("watherProof>>>>>start>>>ip>>>" + ip);
            StringBuilder sb = new StringBuilder();
            sb.append(url).append("?aid=").append(appId)
                    .append("&AppSecretKey=").append(appSecretKey)
                    .append("&Ticket=").append(ticket).
                    append("&Randstr=").append(randStr).
                    append("&UserIP=").append(ip);
//            getMethod = new GetMethod(sb.toString());
//            int code = client.executeMethod(getMethod);
            // 替代原 GetMethod + client.executeMethod 逻辑
//            response = restTemplate.getForObject(sb.toString(), String.class);
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    sb.toString(),          // 请求URL
                    HttpMethod.GET,   // 请求方式（GET）
                    HttpEntity.EMPTY, // 请求体（GET请求无body，传EMPTY）
                    String.class      // 响应体类型（字符串）
            );

            // 3. 获取响应码和响应内容
            int code = responseEntity.getStatusCode().value(); // 响应码（如200）
            response = responseEntity.getBody(); // 响应内容（等价于原getResponseBodyAsString()）
            log.info("状态响应码为>>>>>>" + code);
        } catch (Exception e) {
            log.error("发生异常", e);
        }
        log.info(">>>>>>>>发送校验结果响应为>>>>>>"+response);
        if(!StringUtils.isEmpty(response)){
            JSONObject responseJson = JSONObject.parseObject(response);
            String code = responseJson.getString("response");
            if("1".equals(code)){
                responseBool = true ;
            }
        }
        return  responseBool;
    }
}
