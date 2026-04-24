package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageData;
import com.feijimiao.xianyuassistant.service.AutoReplyDelayService;
import com.feijimiao.xianyuassistant.service.AutoReplyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 自动回复延时调度服务实现
 * 
 * <p>使用ScheduledExecutorService实现延时任务调度</p>
 * <p>使用ConcurrentHashMap管理每个会话的延时任务</p>
 * 
 * @author IAMLZY
 * @date 2026/4/22
 */
@Slf4j
@Service
public class AutoReplyDelayServiceImpl implements AutoReplyDelayService {
    
    @Autowired
    private AutoReplyService autoReplyService;
    
    /**
     * 延时任务调度器
     */
    private ScheduledExecutorService scheduler;
    
    /**
     * 待执行的延时任务映射
     * Key: accountId_sId（账号ID_会话ID）
     * Value: ScheduledFuture（延时任务）
     */
    private final Map<String, ScheduledFuture<?>> pendingTasks = new ConcurrentHashMap<>();
    
    /**
     * 待处理的消息数据列表映射（用于延时任务执行时获取所有触发消息）
     * Key: accountId_sId
     * Value: 延时期间收到的所有消息列表
     */
    private final Map<String, List<ChatMessageData>> pendingMessages = new ConcurrentHashMap<>();
    
    /**
     * 默认延时秒数
     */
    private static final int DEFAULT_DELAY_SECONDS = 15;
    
    @PostConstruct
    public void init() {
        // 创建调度器，核心线程数为2，足够处理延时任务
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "auto-reply-delay-scheduler");
            t.setDaemon(true);
            return t;
        });
        log.info("自动回复延时调度器初始化完成");
    }
    
    @PreDestroy
    @Override
    public void shutdown() {
        log.info("关闭自动回复延时调度器...");
        
        // 取消所有待执行的任务
        pendingTasks.forEach((key, future) -> {
            if (future != null && !future.isDone()) {
                future.cancel(false);
            }
        });
        pendingTasks.clear();
        pendingMessages.clear();
        
        // 关闭调度器
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("自动回复延时调度器已关闭");
    }
    
    @Override
    public void submitDelayTask(ChatMessageData messageData) {
        if (messageData == null || messageData.getSId() == null) {
            log.warn("消息数据无效，无法提交延时任务");
            return;
        }
        
        Long accountId = messageData.getXianyuAccountId();
        String sId = messageData.getSId();
        String taskKey = buildTaskKey(accountId, sId);
        
        // 获取延时秒数（从配置或使用默认值）
        int delaySeconds = getDelaySeconds(messageData);
        
        log.info("【账号{}】提交延时回复任务: sId={}, delay={}s, 当前待执行任务数={}", 
                accountId, sId, delaySeconds, pendingTasks.size());
        
        // 先取消该会话之前的延时任务
        cancelDelayTask(accountId, sId);
        
        // 追加消息到列表（延时期间可能收到多条消息）
        pendingMessages.compute(taskKey, (key, existingList) -> {
            if (existingList == null) {
                existingList = new ArrayList<>();
            }
            existingList.add(messageData);
            return existingList;
        });
        
        // 提交新的延时任务
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                // 从映射中移除（任务开始执行）
                pendingTasks.remove(taskKey);
                List<ChatMessageData> messageList = pendingMessages.remove(taskKey);
                
                if (messageList != null && !messageList.isEmpty()) {
                    log.info("【账号{}】延时任务到期，开始执行自动回复: sId={}, 触发消息数={}", accountId, sId, messageList.size());
                    // 执行自动回复，传入消息列表
                    autoReplyService.executeAutoReply(messageList);
                }
            } catch (Exception e) {
                log.error("【账号{}】执行延时回复任务异常: sId={}", accountId, sId, e);
            }
        }, delaySeconds, TimeUnit.SECONDS);
        
        // 保存任务引用
        pendingTasks.put(taskKey, future);
        
        log.debug("【账号{}】延时任务已提交: taskKey={}, delay={}s", accountId, taskKey, delaySeconds);
    }
    
    @Override
    public void cancelDelayTask(Long accountId, String sId) {
        if (accountId == null || sId == null) {
            return;
        }
        
        String taskKey = buildTaskKey(accountId, sId);
        ScheduledFuture<?> future = pendingTasks.remove(taskKey);
        // 注意：不清除pendingMessages，因为新消息到来时需要保留之前收集的消息继续追加
        
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(false);
            log.debug("【账号{}】取消延时任务: taskKey={}, cancelled={}", accountId, taskKey, cancelled);
        }
    }
    
    @Override
    public int getPendingTaskCount() {
        return pendingTasks.size();
    }
    
    /**
     * 构建任务Key
     */
    private String buildTaskKey(Long accountId, String sId) {
        return accountId + "_" + sId;
    }
    
    /**
     * 获取延时秒数
     * 
     * <p>优先使用消息中携带的配置，否则使用默认值</p>
     */
    private int getDelaySeconds(ChatMessageData messageData) {
        // 这里可以从消息数据或配置中获取，暂时使用默认值
        // 后续可以通过查询数据库配置来获取
        return DEFAULT_DELAY_SECONDS;
    }
}
