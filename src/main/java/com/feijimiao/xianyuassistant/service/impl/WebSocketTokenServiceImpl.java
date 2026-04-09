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

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket Token服务实现
 * 参考Python的refresh_token方法
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
     * Cookie过期重试最大次数（参考Python的retry_count >= 2）
     */
    private static final int MAX_COOKIE_RETRY_COUNT = 2;
    
    /**
     * Cookie过期重试间隔（毫秒）
     */
    private static final long COOKIE_RETRY_INTERVAL = 500;
    
    @Override
    public String getAccessToken(Long accountId, String cookiesStr, String deviceId) {
        return getAccessTokenWithRetry(accountId, cookiesStr, deviceId, 0);
    }
    
    /**
     * 获取AccessToken（带重试机制）
     * 参考Python的get_access_token方法
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
                    // 仍在等待验证，直接抛出异常，不重复请求
                    String captchaUrl = pendingCaptchaAccounts.get(accountId);
                    log.debug("【账号{}】正在等待滑块验证，跳过重复请求", accountId);
                    throw new CaptchaRequiredException(captchaUrl);
                } else {
                    // 验证超时，清除记录，允许重新请求
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
                    // 清除等待验证状态（如果有）
                    pendingCaptchaAccounts.remove(accountId);
                    captchaTimestamps.remove(accountId);
                    return cookieEntity.getWebsocketToken();
                } else {
                    log.info("【账号{}】数据库中的Token已过期，需要重新获取", accountId);
                }
            }
            
            log.info("【账号{}】开始获取新的accessToken...", accountId);
            
            // 1. 生成时间戳（参考Python: int(time.time()) * 1000）
            String timestamp = String.valueOf(System.currentTimeMillis());
            
            // 2. 解析Cookie获取_m_h5_tk token
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookiesStr);
            String mh5tk = cookies.get("_m_h5_tk");
            String token = "";
            if (mh5tk != null && mh5tk.contains("_")) {
                token = mh5tk.split("_")[0];
            }
            
            // 3. 构建data参数（参考Python格式）
            String dataVal = String.format("{\"appKey\":\"444e9908a51d1cb236a27862abc769c9\",\"deviceId\":\"%s\"}", deviceId);
            
            // 4. 生成签名
            String sign = XianyuSignUtils.generateSign(timestamp, token, dataVal);
            
            // 5. 构建URL参数（参考Python的get_token方法）
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
            
            // 6. 构建请求体
            Map<String, String> data = new HashMap<>();
            data.put("data", dataVal);
            
            // 7. 构建请求头（参考Python的get_token方法）
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
            
            // 8. 构建完整URL（带查询参数）
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
            
            // 打印请求信息
            log.info("【账号{}】============", accountId);
            log.info("【账号{}】1、请求体: {}", accountId, data);
            log.info("【账号{}】2、发送POST请求: {}", accountId, fullUrl);
            
            // 9. 发送POST请求
            String response = HttpClientUtils.post(fullUrl, headers, data);
            
            log.info("【账号{}】3、响应内容: {}", accountId, response);
            log.info("【账号{}】============", accountId);
            
            if (response == null || response.isEmpty()) {
                log.error("【账号{}】获取accessToken失败：响应为空", accountId);
                return null;
            }
            
            // 10. 解析响应
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
                        
                        // 记录操作日志
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
                    // 提取滑块验证URL
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
                    log.info("【账号{}】data字段内容: {}", accountId, dataMap);
                    
                    if (dataMap != null && dataMap.containsKey("url")) {
                        String captchaUrl = (String) dataMap.get("url");
                        
                        // 记录等待验证状态
                        pendingCaptchaAccounts.put(accountId, captchaUrl);
                        captchaTimestamps.put(accountId, System.currentTimeMillis());
                        
                        // 更新账号状态为-2（需要验证）
                        updateAccountStatusToCaptchaRequired(accountId);
                        
                        log.warn("【账号{}】检测到滑块验证，URL: {}", accountId, captchaUrl);
                        log.warn("【账号{}】需要人工完成滑块验证，请访问: http://localhost:8080/websocket-manual-captcha.html", accountId);
                        log.warn("【账号{}】账号状态已更新为-2（需要验证）", accountId);
                        
                        // 抛出异常让用户手动处理
                        throw new CaptchaRequiredException(captchaUrl);
                    } else {
                        log.error("【账号{}】需要滑块验证但未找到URL", accountId);
                    }
                }
            }
            
            log.error("【账号{}】获取accessToken失败：{}", accountId, response);
            
            // 检查是否是Cookie过期导致的失败（支持多种错误码格式）
            if (response.contains("FAIL_SYS_SESSION_EXPIRED") || 
                response.contains("FAIL_SYS_TOKEN_EXOIRED") ||  // 注意：API返回的拼写错误
                response.contains("FAIL_SYS_TOKEN_EXPIRED") || 
                response.contains("令牌过期")) {
                
                log.warn("【账号{}】检测到Cookie过期，尝试自动刷新... (重试次数: {}/{})", 
                        accountId, retryCount, MAX_COOKIE_RETRY_COUNT);
                
                // 检查是否超过最大重试次数
                if (retryCount >= MAX_COOKIE_RETRY_COUNT) {
                    log.error("【账号{}】Cookie刷新重试次数已达上限，停止重试", accountId);
                    updateCookieStatus(accountId, 2);
                    
                    // 记录操作日志
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
                
                // 尝试通过CookieRefreshService刷新Cookie（参考Python的hasLogin）
                try {
                    boolean refreshSuccess = cookieRefreshService.refreshCookie(accountId);
                    
                    if (refreshSuccess) {
                        log.info("【账号{}】Cookie刷新成功，准备重新获取Token...", accountId);
                        
                        // 等待一小段时间，避免频繁请求
                        Thread.sleep(COOKIE_RETRY_INTERVAL);
                        
                        // 获取新的Cookie
                        String newCookieStr = accountService.getCookieByAccountId(accountId);
                        if (newCookieStr != null && !newCookieStr.isEmpty()) {
                            // 递归调用，重试获取Token
                            return getAccessTokenWithRetry(accountId, newCookieStr, deviceId, retryCount + 1);
                        } else {
                            log.error("【账号{}】获取新Cookie失败", accountId);
                        }
                    } else {
                        log.warn("【账号{}】Cookie自动刷新失败", accountId);
                    }
                } catch (Exception e) {
                    log.error("【账号{}】Cookie刷新过程发生异常", accountId, e);
                }
                
                // 刷新失败，更新状态并抛出异常
                updateCookieStatus(accountId, 2);
                throw new com.feijimiao.xianyuassistant.exception.CookieExpiredException(
                        "Cookie已过期，自动刷新失败，请更新Cookie后重试");
            }
            
            // 检查是否是Cookie失效
            if (response.contains("FAIL_SYS_ILLEGAL_ACCESS") || response.contains("非法访问")) {
                // 更新Cookie状态为失效
                updateCookieStatus(accountId, 3);
                throw new com.feijimiao.xianyuassistant.exception.CookieExpiredException("Cookie已失效，请重新获取Cookie");
            }
            
            return null;
            
        } catch (CaptchaRequiredException e) {
            // 重新抛出滑块验证异常，让上层处理
            throw e;
        } catch (Exception e) {
            log.error("【账号{}】获取accessToken异常", accountId, e);
            return null;
        }
    }
    
    @Override
    public void saveToken(Long accountId, String token) {
        saveTokenToDatabase(accountId, token);
    }
    
    @Override
    public void clearToken(Long accountId) {
        try {
            log.info("【账号{}】清除数据库中的Token缓存", accountId);
            
            // 将Token过期时间设置为0，强制下次重新获取
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
            
            // 3. 获取新token
            String newToken = getAccessToken(accountId, cookie.getCookieText(), "device_" + accountId);
            
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
                account.setStatus(-2); // -2表示需要验证
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
                account.setStatus(1); // 1表示正常
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
     * 
     * @param accountId 账号ID
     * @param token accessToken
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
