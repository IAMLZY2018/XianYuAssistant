package com.feijimiao.xianyuassistant.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 闲鱼API调用工具类（带自动刷新机制）
 * 参考Python Demo的被动刷新策略
 */
@Slf4j
@Component
public class XianyuApiCallUtils {
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.CookieRefreshService cookieRefreshService;
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.AccountService accountService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_COUNT = 2;
    
    /**
     * 重试间隔（毫秒）
     */
    private static final long RETRY_INTERVAL = 500;
    
    /**
     * 调用闲鱼API（带自动刷新机制）
     * 
     * @param accountId 账号ID
     * @param apiName API名称
     * @param dataMap 数据Map
     * @param cookiesStr Cookie字符串
     * @return API响应结果
     */
    public ApiCallResult callApiWithRetry(Long accountId, String apiName, 
                                          Map<String, Object> dataMap, String cookiesStr) {
        return callApiWithRetry(accountId, apiName, dataMap, cookiesStr, 0);
    }
    
    /**
     * 调用闲鱼API（带自动刷新机制，内部方法）
     */
    private ApiCallResult callApiWithRetry(Long accountId, String apiName, 
                                           Map<String, Object> dataMap, String cookiesStr, 
                                           int retryCount) {
        try {
            // 1. 调用API
            String response = XianyuApiUtils.callApi(apiName, dataMap, cookiesStr);
            
            if (response == null || response.isEmpty()) {
                log.error("【账号{}】API调用失败：响应为空", accountId);
                return new ApiCallResult(false, null, "响应为空", false);
            }
            
            // 2. 解析响应
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            
            @SuppressWarnings("unchecked")
            List<String> ret = (List<String>) responseMap.get("ret");
            
            if (ret == null || ret.isEmpty()) {
                log.error("【账号{}】API响应格式错误：缺少ret字段", accountId);
                return new ApiCallResult(false, response, "响应格式错误", false);
            }
            
            String retCode = ret.get(0);
            
            // 3. 检查是否成功
            if (retCode.contains("SUCCESS")) {
                log.info("【账号{}】API调用成功: {}", accountId, apiName);
                return new ApiCallResult(true, response, null, false);
            }
            
            // 4. 检查是否需要刷新Cookie（令牌过期）
            if (isTokenExpired(retCode)) {
                log.warn("【账号{}】检测到令牌过期，尝试自动刷新... (重试次数: {}/{})", 
                        accountId, retryCount, MAX_RETRY_COUNT);
                
                // 检查是否超过最大重试次数
                if (retryCount >= MAX_RETRY_COUNT) {
                    log.error("【账号{}】令牌刷新重试次数已达上限，停止重试", accountId);
                    return new ApiCallResult(false, response, "令牌过期且自动刷新失败", true);
                }
                
                // 尝试刷新Cookie
                boolean refreshSuccess = cookieRefreshService.refreshCookie(accountId);
                
                if (refreshSuccess) {
                    log.info("【账号{}】Cookie刷新成功，准备重试API调用...", accountId);
                    
                    // 等待一小段时间
                    Thread.sleep(RETRY_INTERVAL);
                    
                    // 获取新的Cookie
                    String newCookieStr = accountService.getCookieByAccountId(accountId);
                    if (newCookieStr != null && !newCookieStr.isEmpty()) {
                        // 递归调用，重试API
                        return callApiWithRetry(accountId, apiName, dataMap, newCookieStr, retryCount + 1);
                    } else {
                        log.error("【账号{}】获取新Cookie失败", accountId);
                    }
                } else {
                    log.warn("【账号{}】Cookie自动刷新失败", accountId);
                }
                
                return new ApiCallResult(false, response, "令牌过期，自动刷新失败", true);
            }
            
            // 5. 检查是否触发风控
            if (isRiskControl(retCode)) {
                log.error("【账号{}】触发风控: {}", accountId, retCode);
                return new ApiCallResult(false, response, "触发风控，需要人工处理", false);
            }
            
            // 6. 其他错误
            log.error("【账号{}】API调用失败: {}", accountId, retCode);
            return new ApiCallResult(false, response, retCode, false);
            
        } catch (Exception e) {
            log.error("【账号{}】API调用异常: apiName={}", accountId, apiName, e);
            return new ApiCallResult(false, null, "调用异常: " + e.getMessage(), false);
        }
    }
    
    /**
     * 判断是否为令牌过期错误
     */
    private boolean isTokenExpired(String retCode) {
        return retCode.contains("FAIL_SYS_TOKEN_EXOIRED") ||  // 注意：API返回的拼写错误
               retCode.contains("FAIL_SYS_TOKEN_EXPIRED") ||
               retCode.contains("FAIL_SYS_SESSION_EXPIRED") ||
               retCode.contains("令牌过期");
    }
    
    /**
     * 判断是否为风控错误
     */
    private boolean isRiskControl(String retCode) {
        return retCode.contains("RGV587_ERROR") ||
               retCode.contains("被挤爆啦") ||
               retCode.contains("FAIL_SYS_USER_VALIDATE");
    }
    
    /**
     * API调用结果封装类
     */
    public static class ApiCallResult {
        private final boolean success;
        private final String response;
        private final String errorMessage;
        private final boolean tokenExpired;
        
        public ApiCallResult(boolean success, String response, String errorMessage, boolean tokenExpired) {
            this.success = success;
            this.response = response;
            this.errorMessage = errorMessage;
            this.tokenExpired = tokenExpired;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getResponse() {
            return response;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public boolean isTokenExpired() {
            return tokenExpired;
        }
        
        /**
         * 从响应中提取data字段
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> extractData() {
            if (response == null || response.isEmpty()) {
                return null;
            }
            
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> responseMap = mapper.readValue(response, Map.class);
                return (Map<String, Object>) responseMap.get("data");
            } catch (Exception e) {
                log.error("提取data字段失败", e);
                return null;
            }
        }
    }
}
