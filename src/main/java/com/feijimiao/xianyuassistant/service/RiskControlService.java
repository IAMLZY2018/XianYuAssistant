package com.feijimiao.xianyuassistant.service;

import java.util.Map;

/**
 * 风控检测服务
 * 参考Python代码的风控处理逻辑
 */
public interface RiskControlService {
    
    /**
     * 检测API响应是否触发风控
     * 
     * @param response API响应数据
     * @return true=触发风控，false=正常
     */
    boolean detectRiskControl(Map<String, Object> response);
    
    /**
     * 处理风控情况
     * 
     * @param accountId 账号ID
     * @param response API响应数据
     * @return 处理结果
     */
    RiskControlResult handleRiskControl(Long accountId, Map<String, Object> response);
    
    /**
     * 检测Token是否失效
     * 
     * @param response API响应数据
     * @return true=Token失效，false=正常
     */
    boolean isTokenExpired(Map<String, Object> response);
    
    /**
     * 风控处理结果
     */
    enum RiskControlResult {
        /**
         * 正常，无风控
         */
        NORMAL,
        
        /**
         * 触发风控，需要更新Cookie
         */
        RISK_CONTROL_DETECTED,
        
        /**
         * Token失效，需要刷新Token
         */
        TOKEN_EXPIRED,
        
        /**
         * 限流，需要等待
         */
        RATE_LIMITED
    }
}
