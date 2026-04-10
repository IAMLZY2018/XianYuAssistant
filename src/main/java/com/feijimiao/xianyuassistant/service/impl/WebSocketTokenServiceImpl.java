package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.entity.XianyuCookie;
import com.feijimiao.xianyuassistant.exception.CaptchaRequiredException;
import com.feijimiao.xianyuassistant.mapper.XianyuCookieMapper;

import com.feijimiao.xianyuassistant.service.AccountService;
import com.feijimiao.xianyuassistant.service.CookieRefreshService;
import com.feijimiao.xianyuassistant.service.OperationLogService;
import com.feijimiao.xianyuassistant.service.WebSocketTokenService;
import com.feijimiao.xianyuassistant.utils.HttpClientUtils;
import com.feijimiao.xianyuassistant.utils.XianyuSignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebSocket Token服务实现
 * 参考Python XianyuAutoAgent的get_token/hasLogin方法
 * 
 * 核心逻辑：
 * 1. 调用token API时，需要从响应Set-Cookie中提取新的_m_h5_tk并更新到数据库
 * 2. token获取失败时，先处理响应中的Set-Cookie（类似Python的clear_duplicate_cookies），再重试
 * 3. Cookie过期时，先调hasLogin刷新Cookie，再用新Cookie重新获取token
 */
@Slf4j
@Service
public class WebSocketTokenServiceImpl implements WebSocketTokenService {

    @Autowired
    private XianyuCookieMapper xianyuCookieMapper;
    
    @Autowired
    private com.feijimiao.xianyuassistant.mapper.XianyuAccountMapper xianyuAccountMapper;
    
    @Autowired
    private CookieRefreshService cookieRefreshService;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private OperationLogService operationLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Token API地址
     */
    private static final String TOKEN_API_URL = "https://h5api.m.goofish.com/h5/mtop.taobao.idlemessage.pc.login.token/1.0/";
    
    /**
     * Token 有效期（20小时，参考 Python 的 TOKEN_REFRESH_INTERVAL）
     */
    private static final long TOKEN_VALID_DURATION = 20 * 60 * 60 * 1000; // 20小时
    
    /**
     * 记录正在等待验证的账号和验证URL
     * Key: accountId, Value: captchaUrl
     */
    private final Map<Long, String> pendingCaptchaAccounts = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * 记录验证URL的创建时间，用于超时清理
     * Key: accountId, Value: timestamp
     */
    private final Map<Long, Long> captchaTimestamps = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * 验证URL有效期（5分钟）
     */
    private static final long CAPTCHA_TIMEOUT = 5 * 60 * 1000;
    
    /**
     * Token获取失败重试最大次数（参考Python: retry_count >= 2）
     */
    private static final int MAX_TOKEN_RETRY_COUNT = 2;
    
    /**
     * Cookie过期时hasLogin重试最大次数
     */
    private static final int MAX_COOKIE_RETRY_COUNT = 2;
    
    /**
     * 重试间隔（毫秒）
     */
    private static final long RETRY_INTERVAL = 500;
    
    @Override
    public String getAccessToken(Long accountId, String cookiesStr, String deviceId) {
        return getAccessTokenWithRetry(accountId, cookiesStr, deviceId, 0);
    }
    
    /**
     * 获取AccessToken（带重试机制）
     * 参考Python XianyuApis.get_token方法
     * 
     * 核心流程（与Python一致）：
     * 1. 发送token请求
     * 2. 如果成功，返回accessToken
     * 3. 如果失败，先处理响应中的Set-Cookie（更新_m_h5_tk），然后重试
     * 4. 重试达到上限时，调用hasLogin刷新Cookie后重试（重置重试计数）
     * 5. hasLogin也失败，则报错
     * 
     * @param accountId 账号ID
     * @param cookiesStr Cookie字符串
     * @param deviceId 设备ID
     * @param retryCount 当前重试次数
     * @return accessToken
     */
    private String getAccessTokenWithRetry(Long accountId, String cookiesStr, String deviceId, int retryCount) {
        try {
            // 0. 检查是否正在等待验证
            if (pendingCaptchaAccounts.containsKey(accountId)) {
                Long timestamp = captchaTimestamps.get(accountId);
                if (timestamp != null && System.currentTimeMillis() - timestamp < CAPTCHA_TIMEOUT) {
                    String captchaUrl = pendingCaptchaAccounts.get(accountId);
                    log.debug("【账号{}】正在等待滑块验证，跳过重复请求", accountId);
                    throw new CaptchaRequiredException(captchaUrl);
                } else {
                    log.info("【账号{}】验证超时，清除等待状态", accountId);
                    pendingCaptchaAccounts.remove(accountId);
                    captchaTimestamps.remove(accountId);
                }
            }
            
            // 1. 先从数据库检查是否有有效的 Token
            XianyuCookie cookieEntity = xianyuCookieMapper.selectOne(
                    new LambdaQueryWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
            );
            
            if (cookieEntity != null && cookieEntity.getWebsocketToken() != null 
                    && cookieEntity.getTokenExpireTime() != null) {
                long now = System.currentTimeMillis();
                if (cookieEntity.getTokenExpireTime() > now) {
                    long remainingHours = (cookieEntity.getTokenExpireTime() - now) / (60 * 60 * 1000);
                    log.info("【账号{}】使用数据库中的accessToken（剩余有效期: {}小时）", 
                            accountId, remainingHours);
                    pendingCaptchaAccounts.remove(accountId);
                    captchaTimestamps.remove(accountId);
                    return cookieEntity.getWebsocketToken();
                } else {
                    log.info("【账号{}】数据库中的Token已过期，需要重新获取", accountId);
                }
            }
            
            log.info("【账号{}】开始获取新的accessToken... (重试次数: {})", accountId, retryCount);
            
            // 2. 生成时间戳
            String timestamp = String.valueOf(System.currentTimeMillis());
            
            // 3. 解析Cookie获取_m_h5_tk token
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookiesStr);
            String mh5tk = cookies.get("_m_h5_tk");
            String token = "";
            if (mh5tk != null && mh5tk.contains("_")) {
                token = mh5tk.split("_")[0];
            }
            
            // 4. 构建data参数
            String dataVal = String.format("{\"appKey\":\"444e9908a51d1cb236a27862abc769c9\",\"deviceId\":\"%s\"}", deviceId);
            
            // 5. 生成签名
            String sign = XianyuSignUtils.generateSign(timestamp, token, dataVal);
            
            // 6. 构建URL参数
            Map<String, String> params = new HashMap<>();
            params.put("jsv", "2.7.2");
            params.put("appKey", "34839810");
            params.put("t", timestamp);
            params.put("sign", sign);
            params.put("v", "1.0");
            params.put("type", "originaljson");
            params.put("accountSite", "xianyu");
            params.put("dataType", "json");
            params.put("timeout", "20000");
            params.put("api", "mtop.taobao.idlemessage.pc.login.token");
            params.put("sessionOption", "AutoLoginOnly");
            params.put("spm_cnt", "a21ybx.im.0.0");
            params.put("spm_pre", "a21ybx.item.want.1.14ad3da6ALVq3n");
            params.put("log_id", "14ad3da6ALVq3n");
            
            // 7. 构建请求体
            Map<String, String> data = new HashMap<>();
            data.put("data", dataVal);
            
            // 8. 构建请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("Host", "h5api.m.goofish.com");
            headers.put("sec-ch-ua-platform", "\"Windows\"");
            headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36");
            headers.put("accept", "application/json");
            headers.put("sec-ch-ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\"");
            headers.put("content-type", "application/x-www-form-urlencoded");
            headers.put("sec-ch-ua-mobile", "?0");
            headers.put("origin", "https://www.goofish.com");
            headers.put("sec-fetch-site", "same-site");
            headers.put("sec-fetch-mode", "cors");
            headers.put("sec-fetch-dest", "empty");
            headers.put("referer", "https://www.goofish.com/");
            headers.put("accept-language", "en,zh-CN;q=0.9,zh;q=0.8,zh-TW;q=0.7,ja;q=0.6");
            headers.put("priority", "u=1, i");
            headers.put("cookie", cookiesStr);
            
            // 9. 构建完整URL
            StringBuilder urlBuilder = new StringBuilder(TOKEN_API_URL);
            urlBuilder.append("?");
            params.forEach((key, value) -> {
                try {
                    urlBuilder.append(key).append("=").append(java.net.URLEncoder.encode(value, "UTF-8")).append("&");
                } catch (Exception e) {
                    log.error("URL编码失败: key={}", key, e);
                }
            });
            String fullUrl = urlBuilder.toString();
            if (fullUrl.endsWith("&")) {
                fullUrl = fullUrl.substring(0, fullUrl.length() - 1);
            }
            
            log.info("【账号{}】============", accountId);
            log.info("【账号{}】1、请求体: {}", accountId, data);
            log.info("【账号{}】2、发送POST请求: {}", accountId, fullUrl);
            
            // 10. 发送POST请求（带响应头，用于提取Set-Cookie）
            HttpClientUtils.HttpResponseResult httpResponse = HttpClientUtils.postWithHeaders(fullUrl, headers, data);
            
            if (httpResponse == null) {
                log.error("【账号{}】获取accessToken失败：HTTP请求异常", accountId);
                return handleTokenFailure(accountId, cookiesStr, deviceId, retryCount, null, "HTTP请求异常");
            }
            
            String response = httpResponse.getBody();
            
            // 【关键】处理响应中的Set-Cookie（参考Python: self.clear_duplicate_cookies()）
            // Python的requests.Session会自动处理Set-Cookie，但Java的HttpClientUtils不会
            // 所以我们需要手动从响应头中提取Set-Cookie并更新数据库中的Cookie
            List<String> setCookieHeaders = httpResponse.getHeaderValues("Set-Cookie");
            if (!setCookieHeaders.isEmpty()) {
                log.info("【账号{}】检测到响应中的Set-Cookie，数量: {}", accountId, setCookieHeaders.size());
                String updatedCookieStr = updateCookiesFromResponse(accountId, cookiesStr, setCookieHeaders);
                if (updatedCookieStr != null && !updatedCookieStr.equals(cookiesStr)) {
                    cookiesStr = updatedCookieStr;
                    log.info("【账号{}】Cookie已从响应Set-Cookie中更新", accountId);
                }
            }
            
            log.info("【账号{}】3、响应内容: {}", accountId, response);
            log.info("【账号{}】============", accountId);
            
            if (response == null || response.isEmpty()) {
                log.error("【账号{}】获取accessToken失败：响应为空", accountId);
                return handleTokenFailure(accountId, cookiesStr, deviceId, retryCount, null, "响应为空");
            }
            
            // 11. 解析响应
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            
            // 检查ret字段
            Object retObj = responseMap.get("ret");
            if (retObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> retList = (java.util.List<String>) retObj;
                log.info("【账号{}】ret字段内容: {}", accountId, retList);
                
                boolean success = retList.stream().anyMatch(ret -> ret.contains("SUCCESS::调用成功"));
                
                if (success) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
                    if (dataMap != null && dataMap.containsKey("accessToken")) {
                        String accessToken = (String) dataMap.get("accessToken");
                        
                        // 保存 token 到数据库
                        saveTokenToDatabase(accountId, accessToken);
                        
                        // 更新账号状态为正常（1）
                        updateAccountStatusToNormal(accountId);
                        
                        log.info("【账号{}】accessToken获取成功并已保存到数据库", accountId);
                        log.debug("【账号{}】accessToken: {}...", accountId, 
                                accessToken.substring(0, Math.min(20, accessToken.length())));
                        
                        operationLogService.log(accountId,
                            com.feijimiao.xianyuassistant.constants.OperationConstants.Type.REFRESH,
                            com.feijimiao.xianyuassistant.constants.OperationConstants.Module.TOKEN,
                            "WebSocket Token获取成功",
                            com.feijimiao.xianyuassistant.constants.OperationConstants.Status.SUCCESS,
                            com.feijimiao.xianyuassistant.constants.OperationConstants.TargetType.TOKEN,
                            String.valueOf(accountId),
                            null, null, null, null);
                        
                        return accessToken;
                    }
                }
                
                // 检查是否需要滑块验证
                boolean needCaptcha = retList.stream().anyMatch(ret -> ret.contains("FAIL_SYS_USER_VALIDATE"));
                log.info("【账号{}】是否需要滑块验证: {}", accountId, needCaptcha);
                
                if (needCaptcha) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
                    log.info("【账号{}】data字段内容: {}", accountId, dataMap);
                    
                    if (dataMap != null && dataMap.containsKey("url")) {
                        String captchaUrl = (String) dataMap.get("url");
                        
                        pendingCaptchaAccounts.put(accountId, captchaUrl);
                        captchaTimestamps.put(accountId, System.currentTimeMillis());
                        
                        updateAccountStatusToCaptchaRequired(accountId);
                        
                        log.warn("【账号{}】检测到滑块验证，URL: {}", accountId, captchaUrl);
                        log.warn("【账号{}】需要人工完成滑块验证，请访问: http://localhost:8080/websocket-manual-captcha.html", accountId);
                        log.warn("【账号{}】账号状态已更新为-2（需要验证）", accountId);
                        
                        throw new CaptchaRequiredException(captchaUrl);
                    } else {
                        log.error("【账号{}】需要滑块验证但未找到URL", accountId);
                    }
                }
                
                // 检查是否触发风控（RGV587_ERROR）
                boolean needRiskControl = retList.stream().anyMatch(ret -> ret.contains("RGV587_ERROR") || ret.contains("被挤爆啦"));
                if (needRiskControl) {
                    log.error("【账号{}】❌ 触发风控: {}", accountId, retList);
                    log.error("【账号{}】系统目前无法自动解决，请进入闲鱼网页版-点击消息-过滑块-复制最新的Cookie", accountId);
                    updateCookieStatus(accountId, 3);
                    throw new com.feijimiao.xianyuassistant.exception.CookieExpiredException(
                            "触发风控，请进入闲鱼网页版过滑块后更新Cookie");
                }
            }
            
            log.error("【账号{}】获取accessToken失败：{}", accountId, response);
            
            // Token获取失败，进入失败处理流程
            return handleTokenFailure(accountId, cookiesStr, deviceId, retryCount, response, "Token API调用失败");
            
        } catch (CaptchaRequiredException e) {
            throw e;
        } catch (com.feijimiao.xianyuassistant.exception.CookieExpiredException e) {
            throw e;
        } catch (Exception e) {
            log.error("【账号{}】获取accessToken异常", accountId, e);
            return null;
        }
    }
    
    /**
     * 处理Token获取失败的情况
     * 参考Python XianyuApis.get_token的失败处理逻辑：
     * 
     * Python逻辑：
     * 1. 非SUCCESS时，先检查响应Set-Cookie并更新cookies（clear_duplicate_cookies）
     * 2. retry_count < 2时，直接重试（此时session已有新cookie）
     * 3. retry_count >= 2时，调用hasLogin刷新Cookie，成功后重置retry_count重新获取token
     * 
     * @param accountId 账号ID
     * @param cookiesStr 当前Cookie字符串（可能已从Set-Cookie更新）
     * @param deviceId 设备ID
     * @param retryCount 当前重试次数
     * @param response API响应内容
     * @param reason 失败原因
     * @return accessToken或null
     */
    private String handleTokenFailure(Long accountId, String cookiesStr, String deviceId, 
                                       int retryCount, String response, String reason) {
        
        boolean isCookieExpired = response != null && (
            response.contains("FAIL_SYS_SESSION_EXPIRED") || 
            response.contains("FAIL_SYS_TOKEN_EXOIRED") ||  // API返回的拼写错误
            response.contains("FAIL_SYS_TOKEN_EXPIRED") || 
            response.contains("令牌过期"));
        
        if (isCookieExpired) {
            log.warn("【账号{}】检测到令牌过期/Cookie过期", accountId);
        }
        
        // 参考Python: retry_count < 2 时直接重试（此时Cookie已从Set-Cookie更新）
        if (retryCount < MAX_TOKEN_RETRY_COUNT) {
            log.warn("【账号{}】Token获取失败({})，准备重试... (重试次数: {}/{})", 
                    accountId, reason, retryCount + 1, MAX_TOKEN_RETRY_COUNT);
            
            try {
                Thread.sleep(RETRY_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 用可能已更新的Cookie重试
            return getAccessTokenWithRetry(accountId, cookiesStr, deviceId, retryCount + 1);
        }
        
        // 参考Python: retry_count >= 2 时，调用hasLogin刷新Cookie后重试
        log.warn("【账号{}】Token获取重试已达上限，尝试通过hasLogin刷新Cookie...", accountId);
        return refreshTokenViaHasLogin(accountId, deviceId, 0);
    }
    
    /**
     * 通过hasLogin刷新Cookie后重新获取Token
     * 参考Python: get_token中retry_count >= 2时的逻辑
     * 
     * Python逻辑：
     * if retry_count >= 2:
     *     if self.hasLogin():  # hasLogin会自动更新session cookies
     *         return self.get_token(device_id, 0)  # 重置重试次数
     *     else:
     *         sys.exit(1)  # Cookie彻底失效
     * 
     * @param accountId 账号ID
     * @param deviceId 设备ID
     * @param hasLoginRetryCount hasLogin重试次数
     * @return accessToken或null
     */
    private String refreshTokenViaHasLogin(Long accountId, String deviceId, int hasLoginRetryCount) {
        if (hasLoginRetryCount >= MAX_COOKIE_RETRY_COUNT) {
            log.error("【账号{}】hasLogin刷新重试次数已达上限，Cookie已彻底过期", accountId);
            updateCookieStatus(accountId, 2);
            
            operationLogService.log(accountId,
                com.feijimiao.xianyuassistant.constants.OperationConstants.Type.REFRESH,
                com.feijimiao.xianyuassistant.constants.OperationConstants.Module.TOKEN,
                "WebSocket Token获取失败：Cookie过期且自动刷新失败",
                com.feijimiao.xianyuassistant.constants.OperationConstants.Status.FAIL,
                com.feijimiao.xianyuassistant.constants.OperationConstants.TargetType.TOKEN,
                String.valueOf(accountId),
                null, null, "Cookie过期且自动刷新失败", null);
            
            throw new com.feijimiao.xianyuassistant.exception.CookieExpiredException(
                    "Cookie已过期且自动刷新失败，请手动更新Cookie后重试");
        }
        
        log.info("【账号{}】开始通过hasLogin刷新Cookie... (重试次数: {}/{})", 
                accountId, hasLoginRetryCount, MAX_COOKIE_RETRY_COUNT);
        
        try {
            // 调用hasLogin刷新Cookie（参考Python的hasLogin方法）
            boolean refreshSuccess = cookieRefreshService.refreshCookie(accountId);
            
            if (refreshSuccess) {
                log.info("【账号{}】hasLogin成功，Cookie已刷新，重新获取Token（重置重试计数）", accountId);
                
                try {
                    Thread.sleep(RETRY_INTERVAL);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // 从数据库获取刷新后的Cookie
                String newCookieStr = accountService.getCookieByAccountId(accountId);
                if (newCookieStr != null && !newCookieStr.isEmpty()) {
                    // 重置retryCount为0，重新开始获取token流程
                    return getAccessTokenWithRetry(accountId, newCookieStr, deviceId, 0);
                } else {
                    log.error("【账号{}】获取刷新后的Cookie失败", accountId);
                }
            } else {
                log.warn("【账号{}】hasLogin失败", accountId);
            }
        } catch (Exception e) {
            log.error("【账号{}】hasLogin刷新过程发生异常", accountId, e);
        }
        
        // hasLogin失败，重试
        return refreshTokenViaHasLogin(accountId, deviceId, hasLoginRetryCount + 1);
    }
    
    /**
     * 从响应的Set-Cookie中更新Cookie
     * 参考Python: requests.Session自动处理Set-Cookie + clear_duplicate_cookies
     * 
     * 关键：token API在返回FAIL_SYS_TOKEN_EXOIRED时，响应的Set-Cookie中会包含新的_m_h5_tk
     * Python的requests.Session会自动保存这些Cookie，所以下次请求时签名是正确的
     * 但Java的HttpClientUtils是无状态的，需要手动处理
     * 
     * @param accountId 账号ID
     * @param currentCookieStr 当前Cookie字符串
     * @param setCookieHeaders 响应中的Set-Cookie列表
     * @return 更新后的Cookie字符串
     */
    private String updateCookiesFromResponse(Long accountId, String currentCookieStr, List<String> setCookieHeaders) {
        try {
            String newCookieStr = mergeCookies(currentCookieStr, setCookieHeaders);
            
            // 清理重复Cookie
            newCookieStr = cookieRefreshService.clearDuplicateCookies(newCookieStr);
            
            // 检查是否有新的_m_h5_tk
            Map<String, String> oldCookies = XianyuSignUtils.parseCookies(currentCookieStr);
            Map<String, String> newCookies = XianyuSignUtils.parseCookies(newCookieStr);
            
            String oldMh5tk = oldCookies.get("_m_h5_tk");
            String newMh5tk = newCookies.get("_m_h5_tk");
            
            boolean mh5tkUpdated = (newMh5tk != null && !newMh5tk.equals(oldMh5tk));
            if (mh5tkUpdated) {
                log.info("【账号{}】✅ _m_h5_tk已从响应中更新: {} -> {}", accountId,
                        oldMh5tk != null ? oldMh5tk.substring(0, Math.min(20, oldMh5tk.length())) + "..." : "null",
                        newMh5tk.substring(0, Math.min(20, newMh5tk.length())) + "...");
            }
            
            // 更新数据库中的Cookie
            if (!newCookieStr.equals(currentCookieStr)) {
                xianyuCookieMapper.update(null,
                        new LambdaUpdateWrapper<XianyuCookie>()
                                .eq(XianyuCookie::getXianyuAccountId, accountId)
                                .set(XianyuCookie::getCookieText, newCookieStr)
                                .set(XianyuCookie::getCookieStatus, 1)
                );
                
                // 如果_m_h5_tk更新了，也更新mH5Tk字段
                if (mh5tkUpdated && newMh5tk != null) {
                    xianyuCookieMapper.update(null,
                            new LambdaUpdateWrapper<XianyuCookie>()
                                    .eq(XianyuCookie::getXianyuAccountId, accountId)
                                    .set(XianyuCookie::getMH5Tk, newMh5tk)
                    );
                }
                
                log.info("【账号{}】Cookie已从响应Set-Cookie更新到数据库", accountId);
            }
            
            return newCookieStr;
        } catch (Exception e) {
            log.error("【账号{}】处理响应Set-Cookie失败", accountId, e);
            return currentCookieStr;
        }
    }
    
    /**
     * 合并Cookie（新Cookie覆盖旧Cookie）
     * 参考Python: requests.Session自动合并Cookie的逻辑
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
        
        // 解析新Cookie（Set-Cookie格式: name=value; Path=/; Domain=.goofish.com; ...）
        for (String newCookie : newCookies) {
            Pattern pattern = Pattern.compile("([^=;\\s]+)=([^;\\s]+)");
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
    
    @Override
    public void saveToken(Long accountId, String token) {
        saveTokenToDatabase(accountId, token);
    }
    
    @Override
    public void clearToken(Long accountId) {
        try {
            log.info("【账号{}】清除数据库中的Token缓存", accountId);
            
            xianyuCookieMapper.update(null,
                    new LambdaUpdateWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
                            .set(XianyuCookie::getTokenExpireTime, 0L)
            );
            
            log.info("【账号{}】Token缓存已清除", accountId);
        } catch (Exception e) {
            log.error("【账号{}】清除Token缓存失败", accountId, e);
        }
    }
    
    @Override
    public void clearCaptchaWait(Long accountId) {
        log.info("【账号{}】清除验证等待状态", accountId);
        pendingCaptchaAccounts.remove(accountId);
        captchaTimestamps.remove(accountId);
        log.info("【账号{}】验证等待状态已清除", accountId);
    }
    
    /**
     * 刷新WebSocket token
     */
    @Override
    public String refreshToken(Long accountId) {
        try {
            log.info("【账号{}】开始刷新WebSocket token...", accountId);
            
            // 1. 获取Cookie
            XianyuCookie cookie = xianyuCookieMapper.selectOne(
                    new LambdaQueryWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
            );
            if (cookie == null || cookie.getCookieText() == null) {
                log.warn("【账号{}】未找到Cookie，无法刷新token", accountId);
                return null;
            }
            
            // 2. 清除旧token，强制重新获取
            clearToken(accountId);
            
            // 3. 获取deviceId
            String deviceId = cookie.getCookieText(); // 临时使用，下面会正确获取
            com.feijimiao.xianyuassistant.entity.XianyuAccount account = xianyuAccountMapper.selectById(accountId);
            if (account != null && account.getDeviceId() != null) {
                deviceId = account.getDeviceId();
            } else {
                Map<String, String> cookies = XianyuSignUtils.parseCookies(cookie.getCookieText());
                String unb = cookies.get("unb");
                if (unb != null) {
                    deviceId = accountService.getOrGenerateDeviceId(accountId, unb);
                } else {
                    deviceId = "device_" + accountId;
                }
            }
            
            // 4. 获取新token
            String newToken = getAccessToken(accountId, cookie.getCookieText(), deviceId);
            
            if (newToken != null && !newToken.isEmpty()) {
                log.info("【账号{}】✅ WebSocket token刷新成功", accountId);
                return newToken;
            } else {
                log.warn("【账号{}】⚠️ WebSocket token刷新失败", accountId);
                return null;
            }
            
        } catch (Exception e) {
            log.error("【账号{}】刷新WebSocket token异常", accountId, e);
            return null;
        }
    }
    
    /**
     * 更新账号状态为需要验证（-2）
     */
    private void updateAccountStatusToCaptchaRequired(Long accountId) {
        try {
            com.feijimiao.xianyuassistant.entity.XianyuAccount account = xianyuAccountMapper.selectById(accountId);
            if (account != null) {
                account.setStatus(-2);
                xianyuAccountMapper.updateById(account);
                log.info("【账号{}】账号状态已更新为-2（需要验证）", accountId);
            }
        } catch (Exception e) {
            log.error("【账号{}】更新账号状态失败", accountId, e);
        }
    }
    
    /**
     * 更新账号状态为正常（1）
     */
    private void updateAccountStatusToNormal(Long accountId) {
        try {
            com.feijimiao.xianyuassistant.entity.XianyuAccount account = xianyuAccountMapper.selectById(accountId);
            if (account != null && account.getStatus() == -2) {
                account.setStatus(1);
                xianyuAccountMapper.updateById(account);
                log.info("【账号{}】账号状态已恢复为1（正常）", accountId);
            }
        } catch (Exception e) {
            log.error("【账号{}】更新账号状态失败", accountId, e);
        }
    }
    
    /**
     * 更新Cookie状态
     */
    private void updateCookieStatus(Long accountId, Integer status) {
        try {
            xianyuCookieMapper.update(null,
                    new LambdaUpdateWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
                            .set(XianyuCookie::getCookieStatus, status)
            );
            String statusText = status == 2 ? "过期" : status == 3 ? "失效" : "未知";
            log.info("【账号{}】Cookie状态已更新为{}({})", accountId, status, statusText);
        } catch (Exception e) {
            log.error("【账号{}】更新Cookie状态失败", accountId, e);
        }
    }
    
    /**
     * 保存 Token 到数据库
     */
    private void saveTokenToDatabase(Long accountId, String token) {
        try {
            long expireTime = System.currentTimeMillis() + TOKEN_VALID_DURATION;
            
            int updated = xianyuCookieMapper.update(null,
                    new LambdaUpdateWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
                            .set(XianyuCookie::getWebsocketToken, token)
                            .set(XianyuCookie::getTokenExpireTime, expireTime)
            );
            
            if (updated > 0) {
                log.info("【账号{}】Token已保存到数据库，过期时间: {}", accountId, 
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .format(new java.util.Date(expireTime)));
            } else {
                log.warn("【账号{}】Token保存失败，未找到对应的Cookie记录", accountId);
            }
        } catch (Exception e) {
            log.error("【账号{}】保存Token到数据库失败", accountId, e);
        }
    }
}
