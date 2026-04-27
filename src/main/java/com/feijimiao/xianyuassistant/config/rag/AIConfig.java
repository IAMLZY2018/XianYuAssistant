package com.feijimiao.xianyuassistant.config.rag;

import org.springframework.context.annotation.Configuration;

/**
 * AI配置类
 * ChatClient的创建由DynamicAIChatClientManager管理，从数据库读取API Key动态创建
 * 不再依赖Spring AI自动配置，系统启动时无需配置API Key
 *
 * @author IAMLZY
 * @date 2026/4/10 22:43
 */
@Configuration
public class AIConfig {
    // ChatClient由DynamicAIChatClientManager动态管理
    // API Key从数据库xianyu_sys_setting表读取
    // 配置变更后自动重建，无需重启服务
}
