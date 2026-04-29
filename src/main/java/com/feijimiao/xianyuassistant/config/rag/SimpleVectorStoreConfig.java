package com.feijimiao.xianyuassistant.config.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * SimpleVectorStore 配置
 * 仅在 ai.enabled=true 时加载
 * VectorStore 由 DynamicVectorStoreManager 动态管理，支持延迟初始化和热更新
 *
 * @author IAMLZY
 * @date 2026/4/23
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class SimpleVectorStoreConfig {

    public SimpleVectorStoreConfig() {
        log.info("[SimpleVectorStoreConfig] AI 功能已启用，VectorStore 将延迟初始化");
    }
}
