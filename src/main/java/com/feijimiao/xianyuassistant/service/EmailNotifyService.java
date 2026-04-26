package com.feijimiao.xianyuassistant.service;

/**
 * 邮件通知服务接口
 */
public interface EmailNotifyService {

    /**
     * 发送WebSocket断开连接且无法重连通知邮件
     *
     * @param accountId 闲鱼账号ID
     * @param accountNote 账号备注
     */
    void sendWsDisconnectNotifyEmail(Long accountId, String accountNote);

    /**
     * 检查WebSocket断开连接邮件通知是否启用
     *
     * @return 是否启用
     */
    boolean isWsDisconnectNotifyEnabled();

    /**
     * 检查邮箱配置是否完整
     *
     * @return 是否已配置
     */
    boolean isEmailConfigured();

    /**
     * 发送测试邮件（同步，返回结果）
     *
     * @return 发送结果，成功返回null，失败返回错误信息
     */
    String sendTestEmail();
}
