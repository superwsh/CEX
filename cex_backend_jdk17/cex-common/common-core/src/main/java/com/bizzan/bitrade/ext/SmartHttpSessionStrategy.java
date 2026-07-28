package com.bizzan.bitrade.ext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;

import java.util.Collections;
import java.util.List;
import java.util.Set;
//import org.springframework.session.Session;
//import org.springframework.session.web.http.CookieHttpSessionStrategy;
//import org.springframework.session.web.http.HeaderHttpSessionStrategy;
//import org.springframework.session.web.http.HttpSessionStrategy;


public class SmartHttpSessionStrategy implements HttpSessionIdResolver {
//public class SmartHttpSessionStrategy implements HttpSessionStrategy {
//    private CookieHttpSessionStrategy browser;
//    private HeaderHttpSessionStrategy api;
//    private String tokenName = "x-auth-token";
    private final CookieHttpSessionIdResolver cookieResolver;
    private final HeaderHttpSessionIdResolver headerResolver;
    private final String tokenName = "x-auth-token";

    public SmartHttpSessionStrategy() {
        this.cookieResolver = new CookieHttpSessionIdResolver();
        this.headerResolver = new HeaderHttpSessionIdResolver(tokenName);
    }

    @Override
    public List<String> resolveSessionIds(HttpServletRequest request) {
        // 优先从 URL 参数读取（如 ?x-auth-token=xxx）
        String paramToken = request.getParameter(tokenName);
        if (StringUtils.isNotEmpty(paramToken)) {
            return Collections.singletonList(paramToken);
        }

        // 判断是 API 请求（带 x-auth-token 头）还是浏览器请求
        // 2. 其次从 Header 读取（API 请求）
        String authHeader = request.getHeader(tokenName);
        if (StringUtils.isNotBlank(authHeader)) {
            return headerResolver.resolveSessionIds(request);
        }

        // 3. 最后从 Cookie 读取（浏览器请求）
        return cookieResolver.resolveSessionIds(request);
    }

    @Override
    public void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId) {
        String authHeader = request.getHeader(tokenName);
        if (authHeader != null) {
            headerResolver.setSessionId(request, response, sessionId);
        } else {
            cookieResolver.setSessionId(request, response, sessionId);
        }
    }

    @Override
    public void expireSession(HttpServletRequest request, HttpServletResponse response) {
        String authHeader = request.getHeader(tokenName);
        if (authHeader != null) {
            headerResolver.expireSession(request, response);
        } else {
            cookieResolver.expireSession(request, response);
        }
    }

//    @Override
//    public String getRequestedSessionId(HttpServletRequest request) {
//        String paramToken = request.getParameter(tokenName);
//        if (StringUtils.isNotEmpty(paramToken)) {
//            return paramToken;
//        }
//        return getStrategy(request).getRequestedSessionId(request);
//    }
//
//    @Override
//    public void onNewSession(Session session, HttpServletRequest request, HttpServletResponse response) {
//        getStrategy(request).onNewSession(session, request, response);
//    }
//
//    @Override
//    public void onInvalidateSession(HttpServletRequest request, HttpServletResponse response) {
//        getStrategy(request).onInvalidateSession(request, response);
//    }
//
//    private HttpSessionStrategy getStrategy(HttpServletRequest request) {
//        String authType = request.getHeader("x-auth-token");
//        if (authType == null) {
//            return this.browser;
//        } else {
//            return this.api;
//        }
//    }
}