package com.feijimiao.xianyuassistant.service;

/**
 * WebSocket Token服务接口
 * 用于获取WebSocket连接所需的accessToken
 */
public interface WebSocketTokenService {
    
    /**
     * 获取accessToken
     * 参考Python的refresh_token方法
     * 内部会自动从数据库读取最新的Cookie和deviceId
     *
     * @param accountId 账号ID
     * @return accessToken，失败返回null
     */
    String getAccessToken(Long accountId);
    
    /**
     * 保存Token到数据库
     * 
     * @param accountId 账号ID
     * @param token accessToken
     */
    void saveToken(Long accountId, String token);
    
    /**
     * 清除Token缓存（强制刷新）
     * 
     * @param accountId 账号ID
     */
    void clearToken(Long accountId);
    
    /**
     * 清除验证等待状态
     */
    void clearCaptchaWait(Long accountId);
    
    /**
     * 刷新WebSocket token
     * 
     * @param accountId 账号ID
     * @return 新的token，失败返回null
     */
    String refreshToken(Long accountId);
}
