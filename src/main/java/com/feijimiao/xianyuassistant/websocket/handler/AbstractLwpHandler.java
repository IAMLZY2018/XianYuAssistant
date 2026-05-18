package com.feijimiao.xianyuassistant.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.utils.AccountDisplayNameUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * WebSocket消息处理器抽象基类
 * 使用模板模式，定义消息处理的标准流程
 */
@Slf4j
public abstract class AbstractLwpHandler {
    
    protected final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    protected AccountDisplayNameUtils displayNameUtils;
    
    /**
     * 获取该处理器对应的lwp路径
     */
    public abstract String getLwpPath();
    
    /**
     * 获取账号显示名称
     */
    protected String getDisplayName(String accountId) {
        return displayNameUtils.getDisplayName(accountId);
    }
    
    /**
     * 格式化日志前缀
     */
    protected String logPrefix(String accountId) {
        return "【" + getDisplayName(accountId) + "】";
    }
    
    /**
     * 模板方法：处理消息的标准流程
     * 
     * @param accountId 账号ID
     * @param messageData 消息数据
     */
    public final void handle(String accountId, Map<String, Object> messageData) {
        try {
            // 1. 前置处理（日志、验证等）
            if (!preHandle(accountId, messageData)) {
                log.debug("【账号{}】前置处理失败，跳过消息处理: lwp={}", accountId, getLwpPath());
                return;
            }
            
            // 2. 解析消息参数
            Object params = parseParams(accountId, messageData);
            if (params == null) {
                log.warn("【账号{}】解析参数失败: lwp={}", accountId, getLwpPath());
                return;
            }
            
            // 3. 执行业务逻辑
            Object result = doHandle(accountId, params, messageData);
            
            // 4. 后置处理（保存、通知等）
            postHandle(accountId, result, messageData);
            
        } catch (Exception e) {
            // 5. 异常处理
            handleException(accountId, messageData, e);
        }
    }
    
    /**
     * 前置处理（钩子方法）
     * 可以进行日志记录、参数验证等
     * 
     * @return true继续处理，false跳过
     */
    protected boolean preHandle(String accountId, Map<String, Object> messageData) {
        log.debug("{}开始处理消息: lwp={}", logPrefix(accountId), getLwpPath());
        return true;
    }
    
    /**
     * 解析消息参数（抽象方法，子类必须实现）
     * 每种lwp的参数结构不同，由子类实现具体解析逻辑
     * 
     * @return 解析后的参数对象
     */
    protected abstract Object parseParams(String accountId, Map<String, Object> messageData);
    
    /**
     * 执行业务逻辑（抽象方法，子类必须实现）
     * 
     * @param params 解析后的参数
     * @param messageData 原始消息数据
     * @return 处理结果
     */
    protected abstract Object doHandle(String accountId, Object params, Map<String, Object> messageData);
    
    /**
     * 后置处理（钩子方法）
     * 可以进行结果保存、事件通知等
     */
    protected void postHandle(String accountId, Object result, Map<String, Object> messageData) {
        log.debug("{}消息处理完成: lwp={}", logPrefix(accountId), getLwpPath());
    }
    
    /**
     * 异常处理（钩子方法）
     */
    protected void handleException(String accountId, Map<String, Object> messageData, Exception e) {
        log.error("{}处理消息异常: lwp={}", logPrefix(accountId), getLwpPath(), e);
    }
    
    /**
     * 工具方法：从Map中安全获取字符串
     */
    protected String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 工具方法：从Map中安全获取整数
     */
    protected Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 工具方法：从Map中安全获取长整数
     */
    protected Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 工具方法：从Map中安全获取Map
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }
    
    /**
     * 工具方法：从Map中安全获取List
     */
    @SuppressWarnings("unchecked")
    protected java.util.List<Object> getList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof java.util.List) {
            return (java.util.List<Object>) value;
        }
        return null;
    }
}
