package com.feijimiao.xianyuassistant.service;

/**
 * 邮件通知服务接口
 */
public interface EmailNotifyService {

    /**
     * 发送滑块验证通知邮件
     *
     * @param accountId 闲鱼账号ID
     * @param accountNote 账号备注
     * @param captchaUrl 验证URL
     */
    void sendCaptchaNotifyEmail(Long accountId, String accountNote, String captchaUrl);

    /**
     * 检查滑块验证邮件通知是否启用
     *
     * @return 是否启用
     */
    boolean isCaptchaNotifyEnabled();

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
