package com.feijimiao.xianyuassistant.service.reply;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 人工接管管理器
 *
 * <p>使用内存 ConcurrentHashMap 管理会话的人工接管状态，替代原来基于数据库和延时任务的检查方式。</p>
 *
 * <h3>核心机制：</h3>
 * <ul>
 *   <li>key = accountId_sId（账号ID_会话ID）</li>
 *   <li>value = 接管过期时间戳（毫秒）</li>
 *   <li>卖家手动回复 → 标记接管（默认10分钟，可配置）</li>
 *   <li>买家消息到来 → 查询是否接管中，是则跳过AI自动回复</li>
 *   <li>定时清理过期标记（每分钟一次）</li>
 * </ul>
 *
 * <h3>优势：</h3>
 * <ul>
 *   <li>实时性：卖家回复后立即生效，无需等延时任务到期</li>
 *   <li>可靠性：买家消息到来时先查接管状态，直接拦截不提交延时任务</li>
 *   <li>性能：内存操作，无数据库查询开销</li>
 * </ul>
 */
@Slf4j
@Component
public class HumanTakeoverManager {

    /** 默认接管时长（分钟） */
    private static final int DEFAULT_MINUTES = 10;

    /**
     * 人工接管状态映射
     * <ul>
     *   <li>Key: accountId_sId</li>
     *   <li>Value: 接管过期时间戳（System.currentTimeMillis() + N分钟）</li>
     * </ul>
     */
    private final ConcurrentHashMap<String, Long> takeoverMap = new ConcurrentHashMap<>();

    /** 过期清理定时调度器 */
    private ScheduledExecutorService cleanupScheduler;

    @PostConstruct
    public void init() {
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "human-takeover-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupScheduler.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
        log.info("人工接管管理器初始化完成");
    }

    @PreDestroy
    public void destroy() {
        takeoverMap.clear();
        if (cleanupScheduler != null) {
            cleanupScheduler.shutdown();
        }
    }

    /**
     * 标记会话为人工接管
     *
     * @param accountId 闲鱼账号ID
     * @param sId       会话ID
     * @param minutes   接管持续时长（分钟），到期后自动恢复AI回复
     */
    public void takeover(Long accountId, String sId, int minutes) {
        String key = buildKey(accountId, sId);
        long expireTime = System.currentTimeMillis() + minutes * 60 * 1000L;
        takeoverMap.put(key, expireTime);
        log.info("【账号{}】人工接管会话: sId={}, {}分钟后恢复自动回复", accountId, sId, minutes);
    }

    /**
     * 标记会话为人工接管（使用默认时长10分钟）
     */
    public void takeover(Long accountId, String sId) {
        takeover(accountId, sId, DEFAULT_MINUTES);
    }

    /**
     * 检查会话是否处于人工接管中
     *
     * <p>如果接管已过期，自动移除标记并返回false</p>
     *
     * @param accountId 闲鱼账号ID
     * @param sId       会话ID
     * @return true=接管中（不触发AI回复），false=未接管（可触发AI回复）
     */
    public boolean isTakenOver(Long accountId, String sId) {
        String key = buildKey(accountId, sId);
        Long expireTime = takeoverMap.get(key);
        if (expireTime == null) return false;
        if (System.currentTimeMillis() > expireTime) {
            takeoverMap.remove(key, expireTime);
            return false;
        }
        return true;
    }

    /**
     * 获取当前活跃的接管会话数量
     */
    public int getActiveCount() {
        return (int) takeoverMap.entrySet().stream()
                .filter(e -> e.getValue() > System.currentTimeMillis())
                .count();
    }

    /** 定时清理过期的接管标记 */
    private void cleanup() {
        try {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<String, Long>> it = takeoverMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> entry = it.next();
                if (entry.getValue() <= now) {
                    it.remove();
                }
            }
        } catch (Exception e) {
            log.warn("清理过期接管标记失败: {}", e.getMessage());
        }
    }

    private String buildKey(Long accountId, String sId) {
        return accountId + "_" + sId;
    }
}
