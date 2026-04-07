package com.feijimiao.xianyuassistant.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.constants.OperationConstants;
import com.feijimiao.xianyuassistant.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 操作日志记录工具类
 * 提供便捷的日志记录方法
 */
@Slf4j
@Component
public class OperationLogUtils {
    
    @Autowired
    private OperationLogService operationLogService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 记录WebSocket连接操作
     */
    public void logWebSocketConnect(Long accountId, boolean success, String errorMessage) {
        operationLogService.log(accountId, 
            OperationConstants.Type.CONNECT, 
            OperationConstants.Module.WEBSOCKET,
            success ? "WebSocket连接成功" : "WebSocket连接失败", 
            success ? OperationConstants.Status.SUCCESS : OperationConstants.Status.FAIL,
            OperationConstants.TargetType.WEBSOCKET, 
            String.valueOf(accountId),
            null, null, errorMessage, null);
    }
    
    /**
     * 记录WebSocket重连操作
     */
    public void logWebSocketReconnect(Long accountId, boolean success, boolean isManual, String errorMessage) {
        String desc = isManual ? 
            (success ? "主动重启连接成功" : "主动重启连接失败") :
            (success ? "异常断开后重连成功" : "异常断开后重连失败");
        
        operationLogService.log(accountId, 
            OperationConstants.Type.RECONNECT, 
            OperationConstants.Module.WEBSOCKET,
            desc, 
            success ? OperationConstants.Status.SUCCESS : OperationConstants.Status.FAIL,
            OperationConstants.TargetType.WEBSOCKET, 
            String.valueOf(accountId),
            null, null, errorMessage, null);
    }
    
    /**
     * 记录账号添加操作
     */
    public void logAccountAdd(Long accountId, String accountNote, boolean success, String errorMessage) {
        operationLogService.log(accountId, 
            OperationConstants.Type.ADD, 
            OperationConstants.Module.ACCOUNT,
            success ? "账号添加成功: " + accountNote : "账号添加失败", 
            success ? OperationConstants.Status.SUCCESS : OperationConstants.Status.FAIL,
            OperationConstants.TargetType.ACCOUNT, 
            String.valueOf(accountId),
            null, null, errorMessage, null);
    }
    
    /**
     * 记录扫码登录操作
     */
    public void logQRLogin(Long accountId, boolean success, String errorMessage) {
        operationLogService.log(accountId, 
            OperationConstants.Type.LOGIN, 
            OperationConstants.Module.QR_LOGIN,
            success ? "扫码登录成功" : "扫码登录失败", 
            success ? OperationConstants.Status.SUCCESS : OperationConstants.Status.FAIL,
            OperationConstants.TargetType.ACCOUNT, 
            String.valueOf(accountId),
            null, null, errorMessage, null);
    }
    
    /**
     * 记录Cookie更新操作
     */
    public void logCookieUpdate(Long accountId, boolean success, String errorMessage) {
        operationLogService.log(accountId, 
            OperationConstants.Type.UPDATE, 
            OperationConstants.Module.COOKIE,
            success ? "Cookie更新成功" : "Cookie更新失败", 
            success ? OperationConstants.Status.SUCCESS : OperationConstants.Status.FAIL,
            OperationConstants.TargetType.COOKIE, 
            String.valueOf(accountId),
            null, null, errorMessage, null);
    }
    
    /**
     * 记录Token刷新操作
     */
    public void logTokenRefresh(Long accountId, String tokenType, boolean success, String errorMessage) {
        operationLogService.log(accountId, 
            OperationConstants.Type.REFRESH, 
            OperationConstants.Module.TOKEN,
            success ? tokenType + " Token刷新成功" : tokenType + " Token刷新失败", 
            success ? OperationConstants.Status.SUCCESS : OperationConstants.Status.FAIL,
            OperationConstants.TargetType.TOKEN, 
            String.valueOf(accountId),
            null, null, errorMessage, null);
    }
    
    /**
     * 记录消息发送操作
     */
    public void logMessageSend(Long accountId, String cid, String toId, String text, 
                              boolean success, String errorMessage) {
        try {
            Map<String, Object> params = Map.of(
                "cid", cid,
                "toId", toId,
                "text", text.length() > 100 ? text.substring(0, 100) + "..." : text
            );
            
            operationLogService.log(accountId, 
                OperationConstants.Type.SEND, 
                OperationConstants.Module.MESSAGE,
                success ? "消息发送成功" : "消息发送失败", 
                success ? OperationConstants.Status.SUCCESS : OperationConstants.Status.FAIL,
                OperationConstants.TargetType.MESSAGE, 
                cid,
                objectMapper.writeValueAsString(params), 
                null, 
                errorMessage, 
                null);
        } catch (Exception e) {
            log.error("记录消息发送日志失败", e);
        }
    }
    
    /**
     * 记录快速回复操作
     */
    public void logQuickReply(Long accountId, String cid, String toId, String text, 
                             boolean success, String errorMessage) {
        try {
            Map<String, Object> params = Map.of(
                "cid", cid,
                "toId", toId,
                "text", text.length() > 100 ? text.substring(0, 100) + "..." : text
            );
            
            operationLogService.log(accountId, 
                OperationConstants.Type.SEND, 
                OperationConstants.Module.MESSAGE,
                success ? "快速回复成功" : "快速回复失败", 
                success ? OperationConstants.Status.SUCCESS : OperationConstants.Status.FAIL,
                OperationConstants.TargetType.MESSAGE, 
                cid,
                objectMapper.writeValueAsString(params), 
                null, 
                errorMessage, 
                null);
        } catch (Exception e) {
            log.error("记录快速回复日志失败", e);
        }
    }
}
