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
     * 发送Cookie过期且无法续期通知邮件
     *
     * @param accountId 闲鱼账号ID
     * @param accountNote 账号备注
     */
    void sendCookieExpireNotifyEmail(Long accountId, String accountNote);

    /**
     * 检查Cookie过期邮件通知是否启用
     *
     * @return 是否启用
     */
    boolean isCookieExpireNotifyEnabled();

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

    /**
     * 发送卡密预警邮件
     *
     * @param toEmail 收件人邮箱
     * @param configName 卡密配置名称
     * @param availableCount 可用数量
     * @param totalCount 总数量
     */
    void sendKamiAlertEmail(String toEmail, String configName, int availableCount, int totalCount);

    /**
     * 发送卡密库存不足邮件（卡密耗尽，无法发货）
     *
     * @param toEmail 收件人邮箱
     * @param configName 卡密配置名称
     * @param orderId 触发的订单ID
     */
    void sendKamiStockOutEmail(String toEmail, String configName, String orderId);

    /**
     * 发送自动发货失败邮件
     *
     * @param toEmail 收件人邮箱（null则用系统配置邮箱）
     * @param xyGoodsId 商品ID
     * @param orderId 订单ID
     * @param failReason 失败原因
     */
    void sendAutoDeliveryFailEmail(String toEmail, String xyGoodsId, String orderId, String failReason);
}
