package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.entity.XianyuAccount;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;
import com.feijimiao.xianyuassistant.entity.XianyuHumanInterventionRecord;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageData;
import com.feijimiao.xianyuassistant.mapper.XianyuAccountMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuChatMessageMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuHumanInterventionRecordMapper;
import com.feijimiao.xianyuassistant.service.AutoReplyDelayService;
import com.feijimiao.xianyuassistant.service.AutoReplyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Autowired
    private XianyuGoodsAutoDeliveryConfigMapper autoDeliveryConfigMapper;

    @Autowired
    private XianyuGoodsConfigMapper goodsConfigMapper;

    @Autowired
    private XianyuAccountMapper accountMapper;

    @Autowired
    private XianyuChatMessageMapper chatMessageMapper;

    @Autowired
    private XianyuHumanInterventionRecordMapper humanInterventionRecordMapper;
    
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
                    // 人工干预检查
                    long firstTriggerTime = messageList.stream()
                            .mapToLong(m -> m.getMessageTime() != null ? m.getMessageTime() : 0L)
                            .min().orElse(0L);
                    
                    if (isHumanInterventionEnabled(accountId, messageData.getXyGoodsId())) {
                        // 检查1：该会话是否在人工干预有效期内
                        if (isInHumanInterventionPeriod(sId)) {
                            log.info("【账号{}】人工干预生效：该会话在人工干预有效期内，取消自动回复: sId={}", accountId, sId);
                            return;
                        }
                        // 检查2：卖家是否在延时期间已回复
                        if (hasSellerRepliedAfter(accountId, sId, firstTriggerTime)) {
                            log.info("【账号{}】人工干预生效：卖家已在延时期间回复，取消自动回复: sId={}", accountId, sId);
                            recordHumanIntervention(accountId, messageData.getXyGoodsId(), sId);
                            return;
                        }
                    }
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
     * <p>优先从数据库配置获取，否则使用默认值</p>
     */
    private int getDelaySeconds(ChatMessageData messageData) {
        try {
            Long accountId = messageData.getXianyuAccountId();
            String xyGoodsId = messageData.getXyGoodsId();
            
            if (accountId == null || xyGoodsId == null) {
                return DEFAULT_DELAY_SECONDS;
            }
            
            XianyuGoodsAutoDeliveryConfig config = autoDeliveryConfigMapper.findByAccountIdAndGoodsIdNoSku(accountId, xyGoodsId);
            if (config != null && config.getRagDelaySeconds() != null && config.getRagDelaySeconds() > 0) {
                return config.getRagDelaySeconds();
            }
        } catch (Exception e) {
            log.warn("获取延时配置失败，使用默认值: {}", e.getMessage());
        }
        return DEFAULT_DELAY_SECONDS;
    }

    /**
     * 检查人工干预开关是否开启
     */
    private boolean isHumanInterventionEnabled(Long accountId, String xyGoodsId) {
        try {
            if (accountId == null || xyGoodsId == null) return false;
            XianyuGoodsConfig config = goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
            return config != null && config.getHumanInterventionOn() != null && config.getHumanInterventionOn() == 1;
        } catch (Exception e) {
            log.warn("检查人工干预开关失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查卖家是否在指定会话中、指定时间之后发送过消息
     */
    private boolean hasSellerRepliedAfter(Long accountId, String sId, long afterTime) {
        try {
            XianyuAccount account = accountMapper.selectById(accountId);
            if (account == null || account.getUnb() == null) return false;
            List<com.feijimiao.xianyuassistant.entity.XianyuChatMessage> messages = chatMessageMapper.findBySId(sId);
            String sellerUnb = account.getUnb();
            for (com.feijimiao.xianyuassistant.entity.XianyuChatMessage msg : messages) {
                if (sellerUnb.equals(msg.getSenderUserId())
                        && msg.getMessageTime() != null
                        && msg.getMessageTime() > afterTime) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("检查卖家回复失败: {}", e.getMessage());
            return false;
        }
    }

    private boolean isInHumanInterventionPeriod(String sId) {
        try {
            XianyuHumanInterventionRecord active = humanInterventionRecordMapper.findActiveBySId(sId);
            if (active != null) {
                log.debug("会话 {} 在人工干预有效期内，endTime={}", sId, active.getEndTime());
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("检查人工干预有效期失败: {}", e.getMessage());
            return false;
        }
    }

    private void recordHumanIntervention(Long accountId, String xyGoodsId, String sId) {
        try {
            XianyuGoodsConfig config = goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
            int minutes = (config != null && config.getHumanInterventionMinutes() != null && config.getHumanInterventionMinutes() > 0)
                    ? config.getHumanInterventionMinutes() : 10;
            LocalDateTime endTime = LocalDateTime.now().plusMinutes(minutes);
            String endTimeStr = endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            XianyuHumanInterventionRecord record = new XianyuHumanInterventionRecord();
            record.setXianyuAccountId(accountId);
            record.setXyGoodsId(xyGoodsId);
            record.setSId(sId);
            record.setEndTime(endTimeStr);
            humanInterventionRecordMapper.insert(record);
            log.info("记录人工干预: sId={}, minutes={}, endTime={}", sId, minutes, endTimeStr);
        } catch (Exception e) {
            log.warn("记录人工干预失败: {}", e.getMessage());
        }
    }

    @Override
    public void recordSellerManualReply(Long accountId, String xyGoodsId, String sId) {
        if (accountId == null || sId == null) return;
        try {
            if (!isHumanInterventionEnabled(accountId, xyGoodsId)) return;
            recordHumanIntervention(accountId, xyGoodsId, sId);
        } catch (Exception e) {
            log.warn("记录卖家手动回复干预失败: {}", e.getMessage());
        }
    }
}
