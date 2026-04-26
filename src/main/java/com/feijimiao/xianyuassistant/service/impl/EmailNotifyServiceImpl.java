package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.service.EmailNotifyService;
import com.feijimiao.xianyuassistant.service.SysSettingService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

@Slf4j
@Service
public class EmailNotifyServiceImpl implements EmailNotifyService {

    private static final String KEY_SMTP_HOST = "email_smtp_host";
    private static final String KEY_SMTP_PORT = "email_smtp_port";
    private static final String KEY_SMTP_USERNAME = "email_smtp_username";
    private static final String KEY_SMTP_PASSWORD = "email_smtp_password";
    private static final String KEY_SMTP_FROM = "email_smtp_from";
    private static final String KEY_SMTP_SSL = "email_smtp_ssl";
    private static final String KEY_WS_DISCONNECT_NOTIFY_ENABLED = "email_notify_ws_disconnect_enabled";

    @Autowired
    private SysSettingService sysSettingService;

    @Override
    @Async
    public void sendWsDisconnectNotifyEmail(Long accountId, String accountNote) {
        if (!isEmailConfigured()) {
            log.warn("邮箱未配置，跳过发送WebSocket断开连接通知邮件");
            return;
        }
        if (!isWsDisconnectNotifyEnabled()) {
            log.debug("WebSocket断开连接邮件通知未启用，跳过");
            return;
        }

        try {
            JavaMailSenderImpl mailSender = buildMailSender();
            String from = getSettingValue(KEY_SMTP_USERNAME);
            String to = getSettingValue(KEY_SMTP_FROM);

            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
            String subject = "【闲鱼助手】服务器无法连接 - 账号" + accountId;
            String content = buildWsDisconnectEmailContent(accountId, accountNote, time);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("WebSocket断开连接通知邮件发送成功: accountId={}, to={}", accountId, to);
        } catch (Exception e) {
            log.error("WebSocket断开连接通知邮件发送失败: accountId={}", accountId, e);
        }
    }

    @Override
    public boolean isWsDisconnectNotifyEnabled() {
        String value = getSettingValue(KEY_WS_DISCONNECT_NOTIFY_ENABLED);
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    @Override
    public boolean isEmailConfigured() {
        String host = getSettingValue(KEY_SMTP_HOST);
        String port = getSettingValue(KEY_SMTP_PORT);
        String username = getSettingValue(KEY_SMTP_USERNAME);
        String password = getSettingValue(KEY_SMTP_PASSWORD);
        String from = getSettingValue(KEY_SMTP_FROM);
        return isNotEmpty(host) && isNotEmpty(port) && isNotEmpty(username)
                && isNotEmpty(password) && isNotEmpty(from);
    }

    @Override
    public String sendTestEmail() {
        if (!isEmailConfigured()) {
            return "邮箱配置不完整，请先配置SMTP服务器、端口、用户名、密码和发件人邮箱";
        }

        try {
            JavaMailSenderImpl mailSender = buildMailSender();
            String from = getSettingValue(KEY_SMTP_USERNAME);
            String to = getSettingValue(KEY_SMTP_FROM);

            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
            String subject = "【闲鱼助手】邮箱配置测试";
            String content = buildTestEmailContent(time);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("测试邮件发送成功: to={}", to);
            return null;
        } catch (Exception e) {
            log.error("测试邮件发送失败", e);
            return "发送失败: " + e.getMessage();
        }
    }

    private String buildTestEmailContent(String time) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;'>");
        sb.append("<h2 style='color:#34c759;border-bottom:2px solid #34c759;padding-bottom:10px;'>✅ 闲鱼助手 - 邮箱配置测试</h2>");
        sb.append("<div style='background:#f0f9ff;border-radius:8px;padding:16px;margin:16px 0;'>");
        sb.append("<p style='margin:8px 0;'><strong>发送时间：</strong>").append(time).append("</p>");
        sb.append("<p style='margin:8px 0;color:#34c759;'>恭喜！您的邮箱通知配置已正常工作，可以正常接收系统通知邮件。</p>");
        sb.append("</div>");
        sb.append("<div style='color:#999;font-size:12px;margin-top:20px;border-top:1px solid #eee;padding-top:10px;'>");
        sb.append("此邮件由闲鱼助手自动发送，请勿回复");
        sb.append("</div>");
        sb.append("</div>");
        return sb.toString();
    }

    private JavaMailSenderImpl buildMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(getSettingValue(KEY_SMTP_HOST));
        sender.setPort(Integer.parseInt(getSettingValue(KEY_SMTP_PORT)));
        sender.setUsername(getSettingValue(KEY_SMTP_USERNAME));
        sender.setPassword(getSettingValue(KEY_SMTP_PASSWORD));
        sender.setDefaultEncoding("UTF-8");

        String ssl = getSettingValue(KEY_SMTP_SSL);
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        if ("1".equals(ssl) || "true".equalsIgnoreCase(ssl)) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.connectiontimeout", "10000");
        return sender;
    }

    private String buildWsDisconnectEmailContent(Long accountId, String accountNote, String time) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;'>");
        sb.append("<h2 style='color:#f56c6c;border-bottom:2px solid #f56c6c;padding-bottom:10px;'>🔴 闲鱼助手 - 服务器无法连接</h2>");
        sb.append("<div style='background:#fef0f0;border-radius:8px;padding:16px;margin:16px 0;'>");
        sb.append("<p style='margin:8px 0;'><strong>账号ID：</strong>").append(accountId).append("</p>");
        if (accountNote != null && !accountNote.isEmpty()) {
            sb.append("<p style='margin:8px 0;'><strong>账号备注：</strong>").append(accountNote).append("</p>");
        }
        sb.append("<p style='margin:8px 0;'><strong>触发时间：</strong>").append(time).append("</p>");
        sb.append("</div>");
        sb.append("<div style='background:#f0f9ff;border-radius:8px;padding:16px;margin:16px 0;'>");
        sb.append("<p style='margin:4px 0;color:#666;'>该账号的WebSocket连接已断开，且多次重连均失败，无法连接到闲鱼服务器。</p>");
        sb.append("<p style='margin:4px 0;color:#666;'>请检查网络连接或Cookie是否有效，并尝试手动重新启动连接。</p>");
        sb.append("</div>");
        sb.append("<div style='color:#999;font-size:12px;margin-top:20px;border-top:1px solid #eee;padding-top:10px;'>");
        sb.append("此邮件由闲鱼助手自动发送，请勿回复");
        sb.append("</div>");
        sb.append("</div>");
        return sb.toString();
    }

    private String getSettingValue(String key) {
        String value = sysSettingService.getSettingValue(key);
        return value != null ? value : "";
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
