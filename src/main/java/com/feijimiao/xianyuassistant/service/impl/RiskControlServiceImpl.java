package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.service.RiskControlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 风控检测服务实现
 * 参考Python代码的风控处理逻辑
 */
@Slf4j
@Service
public class RiskControlServiceImpl implements RiskControlService {

    /**
     * 风控错误码
     * 参考Python: RGV587_ERROR
     */
    private static final String RISK_CONTROL_ERROR = "RGV587_ERROR";
    
    /**
     * 限流错误提示
     * 参考Python: 被挤爆啦
     */
    private static final String RATE_LIMIT_ERROR = "被挤爆啦";
    
    /**
     * Token失效错误码
     */
    private static final String TOKEN_EXPIRED_ERROR = "TOKEN_EXPIRED";
    
    /**
     * 成功标识
     * 参考Python: SUCCESS::调用成功
     */
    private static final String SUCCESS_FLAG = "SUCCESS::调用成功";

    @Override
    public boolean detectRiskControl(Map<String, Object> response) {
        if (response == null) {
            return false;
        }
        
        // 检查ret字段
        Object retObj = response.get("ret");
        if (retObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> retList = (List<String>) retObj;
            
            // 检查是否包含成功信息
            boolean hasSuccess = retList.stream()
                    .anyMatch(ret -> ret != null && ret.contains(SUCCESS_FLAG));
            
            if (!hasSuccess) {
                // 检查是否是风控错误
                String errorMsg = retList.toString();
                if (errorMsg.contains(RISK_CONTROL_ERROR) || errorMsg.contains(RATE_LIMIT_ERROR)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    @Override
    public RiskControlResult handleRiskControl(Long accountId, Map<String, Object> response) {
        if (response == null) {
            return RiskControlResult.NORMAL;
        }
        
        // 检查Token是否失效
        if (isTokenExpired(response)) {
            log.error("【账号{}】❌ Token失效，需要刷新Token", accountId);
            return RiskControlResult.TOKEN_EXPIRED;
        }
        
        // 检查是否触发风控
        if (detectRiskControl(response)) {
            Object retObj = response.get("ret");
            String errorMsg = retObj != null ? retObj.toString() : "未知错误";
            
            log.error("【账号{}】❌ 触发风控: {}", accountId, errorMsg);
            log.error("【账号{}】🔴 系统目前无法自动解决，请进入闲鱼网页版-点击消息-过滑块-复制最新的Cookie", accountId);
            
            // 检查是否是限流
            if (errorMsg.contains(RATE_LIMIT_ERROR)) {
                log.warn("【账号{}】触发限流，建议稍后重试", accountId);
                return RiskControlResult.RATE_LIMITED;
            }
            
            return RiskControlResult.RISK_CONTROL_DETECTED;
        }
        
        return RiskControlResult.NORMAL;
    }
    
    @Override
    public boolean isTokenExpired(Map<String, Object> response) {
        if (response == null) {
            return false;
        }
        
        // 检查code字段
        Object codeObj = response.get("code");
        if (codeObj != null) {
            int code = 0;
            if (codeObj instanceof Number) {
                code = ((Number) codeObj).intValue();
            } else if (codeObj instanceof String) {
                try {
                    code = Integer.parseInt((String) codeObj);
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
            
            // 401表示Token失效
            if (code == 401) {
                return true;
            }
        }
        
        // 检查ret字段
        Object retObj = response.get("ret");
        if (retObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> retList = (List<String>) retObj;
            String errorMsg = retList.toString();
            
            // 检查Token失效错误
            if (errorMsg.contains(TOKEN_EXPIRED_ERROR) || 
                errorMsg.contains("TOKEN_FAIL") ||
                errorMsg.contains("登录过期")) {
                return true;
            }
        }
        
        return false;
    }
}
