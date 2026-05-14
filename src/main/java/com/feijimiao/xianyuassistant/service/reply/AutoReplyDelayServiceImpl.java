package com.feijimiao.xianyuassistant.service.reply;

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
 * 自动回复延时调度服务
 *
 * <p>核心职责：管理买家消息的延时任务，到期后触发AI自动回复。</p>
 *
 * <h3>延时机制：</h3>
 * <ol>
 *   <li>买家消息到来 → 检查人工接管 → 未接管则提交延时任务</li>
 *   <li>延时期间收到新消息 → 取消旧任务，追加消息，重新计时</li>
 *   <li>延时到期 → 再次检查人工接管 → 未接管则执行AI回复</li>
 * </ol>
 *
 * <h3>人工干预拦截（两道防线）：</h3>
 * <ul>
 *   <li>防线1：消息到来时立即检查 {@link HumanTakeoverManager#isTakenOver}，接管中则不提交任务</li>
 *   <li>防线2：延时任务到期时再次检查，防止并发竞态导致漏拦</li>
 * </ul>
 *
 * <h3>依赖组件：</h3>
 * <ul>
 *   <li>{@link HumanTakeoverManager} - 人工接管状态管理（内存Map + 过期清理）</li>
 *   <li>{@link ReplyConfigProvider} - 回复配置查询（延时秒数、干预开关、干预时长）</li>
 *   <li>{@link AutoReplyService} - AI自动回复执行</li>
 * </ul>
 */
@Slf4j
@Service
public class AutoReplyDelayServiceImpl implements AutoReplyDelayService {
    
    @Autowired
    private AutoReplyService autoReplyService;

    @Autowired
    private HumanTakeoverManager takeoverManager;

    @Autowired
    private ReplyConfigProvider configProvider;
    
    /** 延时任务调度线程池 */
    private ScheduledExecutorService scheduler;
    
    /**
     * 待执行的延时任务映射
     * <ul>
     *   <li>Key: accountId_sId</li>
     *   <li>Value: ScheduledFuture（延时任务句柄）</li>
     * </ul>
     */
    private final Map<String, ScheduledFuture<?>> pendingTasks = new ConcurrentHashMap<>();
    
    /**
     * 延时期间收集的买家消息列表
     * <ul>
     *   <li>Key: accountId_sId</li>
     *   <li>Value: 该会话在延时期间收到的所有买家消息</li>
     * </ul>
     */
    private final Map<String, List<ChatMessageData>> pendingMessages = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
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
        pendingTasks.forEach((key, future) -> {
            if (future != null && !future.isDone()) {
                future.cancel(false);
            }
        });
        pendingTasks.clear();
        pendingMessages.clear();
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
    
    /**
     * 提交延时回复任务
     *
     * <p>流程：</p>
     * <ol>
     *   <li>检查人工接管 → 接管中则直接跳过</li>
     *   <li>取消该会话之前的延时任务</li>
     *   <li>追加当前消息到待处理列表</li>
     *   <li>提交新的延时任务（到期后执行AI回复）</li>
     * </ol>
     */
    @Override
    public void submitDelayTask(ChatMessageData messageData) {
        if (messageData == null || messageData.getSId() == null) {
            log.warn("消息数据无效，无法提交延时任务");
            return;
        }
        
        Long accountId = messageData.getXianyuAccountId();
        String sId = messageData.getSId();
        String taskKey = buildTaskKey(accountId, sId);
        
        // 防线1：消息到来时立即检查人工接管
        boolean isTakenOver = takeoverManager.isTakenOver(accountId, sId);
        log.info("【账号{}】提交延时任务前检查人工接管: sId={}, isTakenOver={}", accountId, sId, isTakenOver);
        
        if (isTakenOver) {
            log.info("【账号{}】会话已被人工接管，跳过自动回复: sId={}", accountId, sId);
            return;
        }

        int delaySeconds = configProvider.getDelaySeconds(accountId, messageData.getXyGoodsId());
        log.info("【账号{}】提交延时回复任务: sId={}, delay={}s", accountId, sId, delaySeconds);
        
        // 取消该会话之前的延时任务（买家连续发消息时重新计时）
        cancelDelayTask(accountId, sId);
        
        // 追加消息到待处理列表
        pendingMessages.compute(taskKey, (key, existingList) -> {
            if (existingList == null) existingList = new ArrayList<>();
            existingList.add(messageData);
            return existingList;
        });
        
        // 提交新的延时任务
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                pendingTasks.remove(taskKey);
                
                // 防线2：延时到期时再次检查人工接管（防止并发竞态）
                if (takeoverManager.isTakenOver(accountId, sId)) {
                    log.info("【账号{}】延时任务到期时会话已被人工接管，取消自动回复: sId={}", accountId, sId);
                    pendingMessages.remove(taskKey);
                    return;
                }

                List<ChatMessageData> messageList = pendingMessages.remove(taskKey);
                if (messageList != null && !messageList.isEmpty()) {
                    log.info("【账号{}】延时任务到期，执行自动回复: sId={}, 消息数={}", accountId, sId, messageList.size());
                    autoReplyService.executeAutoReply(messageList);
                }
            } catch (Exception e) {
                log.error("【账号{}】执行延时回复任务异常: sId={}", accountId, sId, e);
            }
        }, delaySeconds, TimeUnit.SECONDS);
        
        pendingTasks.put(taskKey, future);
    }
    
    @Override
    public void cancelDelayTask(Long accountId, String sId) {
        if (accountId == null || sId == null) return;
        String taskKey = buildTaskKey(accountId, sId);
        ScheduledFuture<?> future = pendingTasks.remove(taskKey);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }
    
    @Override
    public int getPendingTaskCount() {
        return pendingTasks.size();
    }

    /**
     * 记录卖家手动回复，触发人工接管
     *
     * <p>流程：</p>
     * <ol>
     *   <li>检查人工干预开关是否开启</li>
     *   <li>标记该会话为人工接管（持续N分钟）</li>
     *   <li>立即取消该会话的延时任务</li>
     *   <li>清除待处理消息</li>
     * </ol>
     */
    @Override
    public void recordSellerManualReply(Long accountId, String xyGoodsId, String sId) {
        if (accountId == null || sId == null) {
            log.warn("recordSellerManualReply参数无效: accountId={}, sId={}, xyGoodsId={}", accountId, sId, xyGoodsId);
            return;
        }

        boolean interventionEnabled = configProvider.isHumanInterventionEnabled(accountId, xyGoodsId);
        log.info("【账号{}】卖家手动回复，检查人工干预开关: sId={}, xyGoodsId={}, enabled={}", accountId, sId, xyGoodsId, interventionEnabled);

        if (!interventionEnabled) {
            log.info("【账号{}】人工干预未开启或xyGoodsId无效，跳过接管: sId={}, xyGoodsId={}", accountId, sId, xyGoodsId);
            return;
        }

        int minutes = configProvider.getInterventionMinutes(accountId, xyGoodsId);
        takeoverManager.takeover(accountId, sId, minutes);

        // 立即取消该会话的延时任务和待处理消息
        cancelDelayTask(accountId, sId);
        pendingMessages.remove(buildTaskKey(accountId, sId));

        log.info("【账号{}】卖家手动回复，人工接管: sId={}, xyGoodsId={}, {}分钟后恢复", accountId, sId, xyGoodsId, minutes);
    }
    
    private String buildTaskKey(Long accountId, String sId) {
        return accountId + "_" + sId;
    }
}
