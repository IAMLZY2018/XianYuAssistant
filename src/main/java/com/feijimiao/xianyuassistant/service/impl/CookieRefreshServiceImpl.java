package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.constants.OperationConstants;
import com.feijimiao.xianyuassistant.entity.XianyuCookie;
import com.feijimiao.xianyuassistant.mapper.XianyuCookieMapper;
import com.feijimiao.xianyuassistant.service.CookieRefreshService;
import com.feijimiao.xianyuassistant.service.OperationLogService;
import com.feijimiao.xianyuassistant.utils.XianyuSignUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cookie刷新服务实现
 * 参考Python代码的Cookie刷新逻辑
 */
@Slf4j
@Service
public class CookieRefreshServiceImpl implements CookieRefreshService {
    
    @Autowired
    private XianyuCookieMapper cookieMapper;
    
    @Autowired
    private OperationLogService operationLogService;
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String HAS_LOGIN_URL = "https://passport.goofish.com/newlogin/hasLogin.do";
    
    public CookieRefreshServiceImpl() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
    }
    
    @Override
    public boolean checkLoginStatus(Long accountId) {
        try {
            log.info("【账号{}】开始检查登录状态...", accountId);
            
            // 1. 获取Cookie
            XianyuCookie cookie = cookieMapper.selectOne(
                    new LambdaQueryWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
                            .orderByDesc(XianyuCookie::getCreatedTime)
                            .last("LIMIT 1")
            );
            
            if (cookie == null || cookie.getCookieText() == null) {
                log.warn("【账号{}】未找到Cookie", accountId);
                return false;
            }
            
            // 2. 解析Cookie
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookie.getCookieText());
            
            // 3. 构建请求参数
            FormBody.Builder formBuilder = new FormBody.Builder();
            formBuilder.add("appName", "xianyu");
            formBuilder.add("fromSite", "77");
            formBuilder.add("hid", cookies.getOrDefault("unb", ""));
            formBuilder.add("ltl", "true");
            formBuilder.add("appEntrance", "web");
            formBuilder.add("_csrf_token", cookies.getOrDefault("XSRF-TOKEN", ""));
            formBuilder.add("umidToken", "");
            formBuilder.add("hsiz", cookies.getOrDefault("cookie2", ""));
            formBuilder.add("bizParams", "taobaoBizLoginFrom=web");
            formBuilder.add("mainPage", "false");
            formBuilder.add("isMobile", "false");
            formBuilder.add("lang", "zh_CN");
            formBuilder.add("returnUrl", "");
            formBuilder.add("isIframe", "true");
            formBuilder.add("documentReferer", "https://www.goofish.com/");
            formBuilder.add("defaultView", "hasLogin");
            formBuilder.add("umidTag", "SERVER");
            formBuilder.add("deviceId", cookies.getOrDefault("cna", ""));
            
            // 4. 构建请求头
            Headers.Builder headersBuilder = new Headers.Builder();
            headersBuilder.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headersBuilder.add("Accept", "application/json, text/plain, */*");
            headersBuilder.add("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headersBuilder.add("Referer", "https://passport.goofish.com/");
            headersBuilder.add("Origin", "https://passport.goofish.com");
            headersBuilder.add("Cookie", cookie.getCookieText());
            
            // 5. 发送请求
            Request request = new Request.Builder()
                    .url(HAS_LOGIN_URL)
                    .headers(headersBuilder.build())
                    .post(formBuilder.build())
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("【账号{}】检查登录状态失败: HTTP {}", accountId, response.code());
                    return false;
                }
                
                String responseBody = response.body().string();
                log.debug("【账号{}】hasLogin响应: {}", accountId, responseBody);
                
                // 6. 解析响应
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> content = (Map<String, Object>) responseMap.get("content");
                
                if (content != null && Boolean.TRUE.equals(content.get("success"))) {
                    log.info("【账号{}】✅ 登录状态有效", accountId);
                    
                    // 7. 提取响应中的新Cookie
                    Headers responseHeaders = response.headers();
                    List<String> setCookieHeaders = responseHeaders.values("Set-Cookie");
                    
                    if (!setCookieHeaders.isEmpty()) {
                        String newCookieStr = mergeCookies(cookie.getCookieText(), setCookieHeaders);
                        
                        // 清理重复Cookie
                        newCookieStr = clearDuplicateCookies(newCookieStr);
                        
                        // 更新Cookie到数据库
                        if (!newCookieStr.equals(cookie.getCookieText())) {
                            cookie.setCookieText(newCookieStr);
                            cookie.setCookieStatus(1); // 设置为有效
                            
                            // 提取并保存_m_h5_tk
                            Map<String, String> cookieMap = XianyuSignUtils.parseCookies(newCookieStr);
                            String mH5Tk = cookieMap.get("_m_h5_tk");
                            if (mH5Tk != null && !mH5Tk.isEmpty()) {
                                cookie.setMH5Tk(mH5Tk);
                                log.info("【账号{}】✅ _m_h5_tk已更新: {}", accountId, 
                                        mH5Tk.substring(0, Math.min(20, mH5Tk.length())) + "...");
                            }
                            
                            // 更新时间戳
                            cookie.setUpdatedTime(java.time.LocalDateTime.now().toString());
                            
                            cookieMapper.updateById(cookie);
                            log.info("【账号{}】✅ Cookie已更新并保存到数据库", accountId);
                            
                            // 记录操作日志
                            operationLogService.log(accountId,
                                OperationConstants.Type.UPDATE,
                                OperationConstants.Module.COOKIE,
                                "Cookie自动刷新成功",
                                OperationConstants.Status.SUCCESS,
                                OperationConstants.TargetType.COOKIE,
                                String.valueOf(accountId),
                                null, null, null, null);
                        }
                    } else {
                        // 即使没有Set-Cookie，也要确保Cookie状态为有效
                        if (cookie.getCookieStatus() != 1) {
                            cookie.setCookieStatus(1);
                            cookie.setUpdatedTime(java.time.LocalDateTime.now().toString());
                            cookieMapper.updateById(cookie);
                            log.info("【账号{}】✅ Cookie状态已更新为有效", accountId);
                        }
                    }
                    
                    return true;
                } else {
                    log.warn("【账号{}】⚠️ 登录状态无效", accountId);
                    
                    // 记录操作日志
                    operationLogService.log(accountId,
                        OperationConstants.Type.VERIFY,
                        OperationConstants.Module.COOKIE,
                        "登录状态检查失败",
                        OperationConstants.Status.FAIL,
                        OperationConstants.TargetType.COOKIE,
                        String.valueOf(accountId),
                        null, null, "登录状态无效", null);
                    
                    return false;
                }
            }
            
        } catch (Exception e) {
            log.error("【账号{}】检查登录状态失败", accountId, e);
            
            // 记录操作日志
            operationLogService.log(accountId,
                OperationConstants.Type.VERIFY,
                OperationConstants.Module.COOKIE,
                "检查登录状态异常: " + e.getMessage(),
                OperationConstants.Status.FAIL,
                OperationConstants.TargetType.COOKIE,
                String.valueOf(accountId),
                null, null, e.getMessage(), null);
            
            return false;
        }
    }
    
    @Override
    public boolean refreshCookie(Long accountId) {
        try {
            log.info("【账号{}】开始刷新Cookie...", accountId);
            
            // 通过hasLogin接口刷新Cookie
            boolean success = checkLoginStatus(accountId);
            
            if (success) {
                log.info("【账号{}】✅ Cookie刷新成功", accountId);
                
                // 记录操作日志
                operationLogService.log(accountId,
                    OperationConstants.Type.REFRESH,
                    OperationConstants.Module.COOKIE,
                    "Cookie刷新成功",
                    OperationConstants.Status.SUCCESS,
                    OperationConstants.TargetType.COOKIE,
                    String.valueOf(accountId),
                    null, null, null, null);
            } else {
                log.error("【账号{}】❌ Cookie刷新失败，需要手动更新", accountId);
                
                // 记录操作日志
                operationLogService.log(accountId,
                    OperationConstants.Type.REFRESH,
                    OperationConstants.Module.COOKIE,
                    "Cookie刷新失败，需要手动更新",
                    OperationConstants.Status.FAIL,
                    OperationConstants.TargetType.COOKIE,
                    String.valueOf(accountId),
                    null, null, null, null);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("【账号{}】刷新Cookie失败", accountId, e);
            
            // 记录操作日志
            operationLogService.log(accountId,
                OperationConstants.Type.REFRESH,
                OperationConstants.Module.COOKIE,
                "刷新Cookie异常: " + e.getMessage(),
                OperationConstants.Status.FAIL,
                OperationConstants.TargetType.COOKIE,
                String.valueOf(accountId),
                null, null, e.getMessage(), null);
            
            return false;
        }
    }
    
    @Override
    public String clearDuplicateCookies(String cookieStr) {
        if (cookieStr == null || cookieStr.isEmpty()) {
            return cookieStr;
        }
        
        // 解析Cookie
        Map<String, String> cookies = new LinkedHashMap<>();
        String[] parts = cookieStr.split(";\\s*");
        
        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx > 0) {
                String key = part.substring(0, idx);
                String value = part.substring(idx + 1);
                cookies.put(key, value); // 后面的会覆盖前面的
            }
        }
        
        // 重新构建Cookie字符串
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        return sb.toString();
    }
    
    /**
     * 合并Cookie（新Cookie覆盖旧Cookie）
     */
    private String mergeCookies(String oldCookieStr, List<String> newCookies) {
        Map<String, String> cookies = new LinkedHashMap<>();
        
        // 解析旧Cookie
        if (oldCookieStr != null && !oldCookieStr.isEmpty()) {
            String[] parts = oldCookieStr.split(";\\s*");
            for (String part : parts) {
                int idx = part.indexOf('=');
                if (idx > 0) {
                    String key = part.substring(0, idx);
                    String value = part.substring(idx + 1);
                    cookies.put(key, value);
                }
            }
        }
        
        // 解析新Cookie
        for (String newCookie : newCookies) {
            // 提取Cookie的name和value
            Pattern pattern = Pattern.compile("([^=]+)=([^;]+)");
            Matcher matcher = pattern.matcher(newCookie);
            if (matcher.find()) {
                String key = matcher.group(1).trim();
                String value = matcher.group(2).trim();
                cookies.put(key, value);
            }
        }
        
        // 重新构建Cookie字符串
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        return sb.toString();
    }
}
