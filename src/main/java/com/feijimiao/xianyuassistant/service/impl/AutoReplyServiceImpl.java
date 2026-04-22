package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoReplyConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoReplyRecord;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageData;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoReplyConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoReplyRecordMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsInfoMapper;
import com.feijimiao.xianyuassistant.service.AIService;
import com.feijimiao.xianyuassistant.service.AutoReplyService;
import com.feijimiao.xianyuassistant.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 自动回复服务实现
 * 
 * @author IAMLZY
 * @date 2026/4/22
 */
@Slf4j
@Service
public class AutoReplyServiceImpl implements AutoReplyService {
    
    @Autowired
    private XianyuGoodsConfigMapper goodsConfigMapper;
    
    @Autowired
    private XianyuGoodsAutoReplyConfigMapper autoReplyConfigMapper;
    
    @Autowired
    private XianyuGoodsAutoReplyRecordMapper autoReplyRecordMapper;
    
    @Autowired
    private XianyuGoodsInfoMapper goodsInfoMapper;
    
    @Autowired
    private WebSocketService webSocketService;
    
    @Autowired(required = false)
    private AIService aiService;
    
    @Value("${ai.enabled:false}")
    private boolean aiEnabled;
    
    /**
     * RAG回复类型
     */
    private static final int REPLY_TYPE_RAG = 2;
    
    @Override
    public void executeRagAutoReply(ChatMessageData messageData) {
        if (messageData == null) {
            log.warn("消息数据为空，无法执行RAG自动回复");
            return;
        }
        
        Long accountId = messageData.getXianyuAccountId();
        String xyGoodsId = messageData.getXyGoodsId();
        String sId = messageData.getSId();
        String pnmId = messageData.getPnmId();
        String buyerMessage = messageData.getMsgContent();
        
        log.info("【账号{}】开始执行RAG自动回复: xyGoodsId={}, sId={}, buyerMessage={}", 
                accountId, xyGoodsId, sId, buyerMessage);
        
        try {
            // 1. 检查AI服务是否可用
            if (!aiEnabled || aiService == null) {
                log.warn("【账号{}】AI服务未启用或不可用，跳过RAG自动回复", accountId);
                return;
            }
            
            // 2. 检查商品是否开启RAG自动回复
            if (!isRagAutoReplyEnabled(accountId, xyGoodsId)) {
                log.info("【账号{}】商品未开启RAG自动回复: xyGoodsId={}", accountId, xyGoodsId);
                return;
            }
            
            // 3. 获取商品本地ID
            XianyuGoodsInfo goodsInfo = goodsInfoMapper.selectOne(
                    new LambdaQueryWrapper<XianyuGoodsInfo>()
                            .eq(XianyuGoodsInfo::getXyGoodId, xyGoodsId)
                            .eq(XianyuGoodsInfo::getXianyuAccountId, accountId)
            );
            if (goodsInfo == null) {
                log.warn("【账号{}】未找到商品信息: xyGoodsId={}", accountId, xyGoodsId);
                return;
            }
            
            // 4. 创建回复记录（状态=0，待回复）
            XianyuGoodsAutoReplyRecord record = new XianyuGoodsAutoReplyRecord();
            record.setXianyuAccountId(accountId);
            record.setXianyuGoodsId(goodsInfo.getId());
            record.setXyGoodsId(xyGoodsId);
            record.setSId(sId);
            record.setPnmId(pnmId);
            record.setBuyerUserId(messageData.getSenderUserId());
            record.setBuyerUserName(messageData.getSenderUserName());
            record.setBuyerMessage(buyerMessage);
            record.setReplyType(REPLY_TYPE_RAG);
            record.setState(0); // 待回复
            
            int insertResult;
            try {
                insertResult = autoReplyRecordMapper.insert(record);
            } catch (Exception e) {
                // 检查是否是唯一约束冲突
                if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                    log.info("【账号{}】该消息已处理过，跳过RAG自动回复: sId={}, pnmId={}", accountId, sId, pnmId);
                    return;
                }
                throw e;
            }
            
            if (insertResult <= 0) {
                log.error("【账号{}】创建回复记录失败", accountId);
                return;
            }
            
            log.info("【账号{}】创建回复记录成功: recordId={}", accountId, record.getId());
            
            // 5. 调用AI服务生成回复
            String replyContent = generateRagReply(buyerMessage, xyGoodsId, accountId);
            
            if (replyContent == null || replyContent.trim().isEmpty()) {
                log.warn("【账号{}】RAG生成的回复内容为空", accountId);
                updateRecordState(record.getId(), -1, null);
                return;
            }
            
            log.info("【账号{}】RAG生成回复内容: {}", accountId, 
                    replyContent.length() > 100 ? replyContent.substring(0, 100) + "..." : replyContent);
            
            // 6. 发送回复消息
            boolean sendSuccess = sendReplyMessage(accountId, sId, replyContent);
            
            // 7. 更新记录状态
            if (sendSuccess) {
                log.info("【账号{}】✅ RAG自动回复成功: xyGoodsId={}, sId={}", accountId, xyGoodsId, sId);
                updateRecordState(record.getId(), 1, replyContent);
            } else {
                log.error("【账号{}】❌ RAG自动回复发送失败: xyGoodsId={}, sId={}", accountId, xyGoodsId, sId);
                updateRecordState(record.getId(), -1, replyContent);
            }
            
        } catch (Exception e) {
            log.error("【账号{}】执行RAG自动回复异常: xyGoodsId={}, sId={}", accountId, xyGoodsId, sId, e);
        }
    }
    
    @Override
    public boolean isRagAutoReplyEnabled(Long accountId, String xyGoodsId) {
        if (accountId == null || xyGoodsId == null) {
            return false;
        }
        
        try {
            // 检查商品是否开启自动回复
            XianyuGoodsConfig goodsConfig = goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
            if (goodsConfig == null || goodsConfig.getXianyuAutoReplyOn() == null || goodsConfig.getXianyuAutoReplyOn() != 1) {
                log.debug("【账号{}】商品未开启自动回复: xyGoodsId={}", accountId, xyGoodsId);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("【账号{}】检查自动回复开关异常: xyGoodsId={}", accountId, xyGoodsId, e);
            return false;
        }
    }
    
    /**
     * 调用AI服务生成RAG回复
     */
    private String generateRagReply(String buyerMessage, String xyGoodsId, Long accountId) {
        try {
            log.info("【账号{}】调用AI服务生成RAG回复: xyGoodsId={}, message={}", accountId, xyGoodsId, buyerMessage);
            
            // 调用AIService的chatByRAG方法
            Flux<String> responseFlux = aiService.chatByRAG(buyerMessage, xyGoodsId);
            
            // 收集流式响应
            AtomicReference<StringBuilder> contentBuilder = new AtomicReference<>(new StringBuilder());
            
            // 阻塞等待流式响应完成（设置超时时间30秒）
            responseFlux
                    .timeout(Duration.ofSeconds(30))
                    .doOnNext(chunk -> contentBuilder.get().append(chunk))
                    .doOnError(e -> log.error("【账号{}】RAG响应流异常: {}", accountId, e.getMessage()))
                    .blockLast();
            
            String content = contentBuilder.get().toString();
            log.info("【账号{}】RAG响应完成，内容长度: {}", accountId, content.length());
            
            return content;
            
        } catch (Exception e) {
            log.error("【账号{}】生成RAG回复异常: xyGoodsId={}", accountId, xyGoodsId, e);
            return null;
        }
    }
    
    /**
     * 发送回复消息
     */
    private boolean sendReplyMessage(Long accountId, String sId, String content) {
        try {
            // 从sId中提取cid和toId
            String cid = sId.replace("@goofish", "");
            String toId = cid;
            
            log.info("【账号{}】发送回复消息: cid={}, toId={}, content={}", 
                    accountId, cid, toId, content.length() > 50 ? content.substring(0, 50) + "..." : content);
            
            return webSocketService.sendMessage(accountId, cid, toId, content);
            
        } catch (Exception e) {
            log.error("【账号{}】发送回复消息异常: sId={}", accountId, sId, e);
            return false;
        }
    }
    
    /**
     * 更新记录状态
     */
    private void updateRecordState(Long recordId, Integer state, String replyContent) {
        try {
            autoReplyRecordMapper.updateStateAndContent(recordId, state, replyContent);
            log.debug("更新回复记录状态: recordId={}, state={}", recordId, state);
        } catch (Exception e) {
            log.error("更新回复记录状态失败: recordId={}, state={}", recordId, state, e);
        }
    }
}
