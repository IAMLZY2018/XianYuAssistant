package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.config.rag.DynamicAIChatClientManager;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoReplyRecord;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;
import com.feijimiao.xianyuassistant.entity.XianyuChatMessage;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import com.feijimiao.xianyuassistant.entity.bo.AutoReplyTriggerContext;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageData;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoReplyRecordMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsInfoMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuChatMessageMapper;
import com.feijimiao.xianyuassistant.service.AIService;
import com.feijimiao.xianyuassistant.service.AutoReplyService;
import com.feijimiao.xianyuassistant.service.WebSocketService;
import com.feijimiao.xianyuassistant.service.bo.RAGReplyResult;
import com.feijimiao.xianyuassistant.service.reply.ReplyStrategy;
import com.feijimiao.xianyuassistant.service.reply.ReplyStrategyResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class AutoReplyServiceImpl implements AutoReplyService {
    
    @Autowired
    private XianyuGoodsConfigMapper goodsConfigMapper;
    
    @Autowired
    private XianyuGoodsAutoReplyRecordMapper autoReplyRecordMapper;
    
    @Autowired
    private XianyuGoodsInfoMapper goodsInfoMapper;
    
    @Autowired
    private XianyuChatMessageMapper chatMessageMapper;
    
    @Autowired
    private WebSocketService webSocketService;
    
    @Autowired(required = false)
    private AIService aiService;

    @Autowired
    private DynamicAIChatClientManager dynamicAIChatClientManager;
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.SentMessageSaveService sentMessageSaveService;
    
    @Autowired
    private com.feijimiao.xianyuassistant.service.AccountService accountService;

    @Autowired
    private ReplyStrategyResolver replyStrategyResolver;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void executeAutoReply(ChatMessageData messageData) {
        if (messageData == null) {
            log.warn("消息数据为空，无法执行自动回复");
            return;
        }
        executeAutoReply(Collections.singletonList(messageData));
    }
    
    @Override
    public void executeAutoReply(List<ChatMessageData> messageList) {
        if (messageList == null || messageList.isEmpty()) {
            log.warn("消息列表为空，无法执行自动回复");
            return;
        }
        
        ChatMessageData lastMessage = messageList.get(messageList.size() - 1);
        Long accountId = lastMessage.getXianyuAccountId();
        String xyGoodsId = lastMessage.getXyGoodsId();
        String sId = lastMessage.getSId();
        String pnmId = lastMessage.getPnmId();
        
        String buyerMessage = messageList.stream()
                .map(ChatMessageData::getMsgContent)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
        
        log.info("【账号{}】开始执行自动回复: xyGoodsId={}, sId={}, 触发消息数={}, buyerMessage={}", 
                accountId, xyGoodsId, sId, messageList.size(), buyerMessage);
        
        try {
            // 1. 检查是否有任何回复开关开启
            if (!isAnyReplyEnabled(accountId, xyGoodsId)) {
                log.info("【账号{}】商品未开启任何回复开关: xyGoodsId={}", accountId, xyGoodsId);
                return;
            }
            
            // 2. 获取商品本地ID
            XianyuGoodsInfo goodsInfo = goodsInfoMapper.selectOne(
                    new LambdaQueryWrapper<XianyuGoodsInfo>()
                            .eq(XianyuGoodsInfo::getXyGoodId, xyGoodsId)
                            .eq(XianyuGoodsInfo::getXianyuAccountId, accountId)
            );
            if (goodsInfo == null) {
                log.warn("【账号{}】未找到商品信息: xyGoodsId={}", accountId, xyGoodsId);
                return;
            }
            
            // 3. 解析回复策略
            ReplyStrategy strategy = replyStrategyResolver.resolve(messageList);
            if (strategy == null) {
                log.info("【账号{}】无可用回复策略: xyGoodsId={}", accountId, xyGoodsId);
                return;
            }
            
            // 4. 构建触发上下文
            AutoReplyTriggerContext triggerContext = new AutoReplyTriggerContext();
            List<AutoReplyTriggerContext.TriggerMessage> triggerMessages = new ArrayList<>();
            for (ChatMessageData msg : messageList) {
                AutoReplyTriggerContext.TriggerMessage tm = new AutoReplyTriggerContext.TriggerMessage();
                tm.setPnmId(msg.getPnmId());
                tm.setSenderUserId(msg.getSenderUserId());
                tm.setSenderUserName(msg.getSenderUserName());
                tm.setMsgContent(msg.getMsgContent());
                tm.setMessageTime(msg.getMessageTime());
                triggerMessages.add(tm);
            }
            triggerContext.setTriggerMessages(triggerMessages);
            
            // 5. 创建回复记录（状态=0，待回复）
            XianyuGoodsAutoReplyRecord record = new XianyuGoodsAutoReplyRecord();
            record.setXianyuAccountId(accountId);
            record.setXianyuGoodsId(goodsInfo.getId());
            record.setXyGoodsId(xyGoodsId);
            record.setSId(sId);
            record.setPnmId(pnmId);
            record.setBuyerUserId(lastMessage.getSenderUserId());
            record.setBuyerUserName(lastMessage.getSenderUserName());
            record.setBuyerMessage(buyerMessage);
            record.setState(0);
            
            int insertResult;
            try {
                insertResult = autoReplyRecordMapper.insert(record);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                    log.info("【账号{}】该消息已处理过，跳过自动回复: sId={}, pnmId={}", accountId, sId, pnmId);
                    return;
                }
                throw e;
            }
            
            if (insertResult <= 0) {
                log.error("【账号{}】创建回复记录失败", accountId);
                return;
            }
            
            // 6. 执行回复策略
            ReplyStrategy.ReplyResult replyResult = strategy.execute(messageList);
            
            if (!replyResult.isSuccess() || replyResult.getItems() == null || replyResult.getItems().isEmpty()) {
                log.warn("【账号{}】回复策略未生成有效内容", accountId);
                updateRecordState(record.getId(), -1, null);
                return;
            }
            
            if (replyResult.getMatchedKeyword() != null) {
                record.setMatchedKeyword(replyResult.getMatchedKeyword());
            }
            
            String allReplyText = replyResult.getItems().stream()
                    .map(ReplyStrategy.ReplyResult.ReplyItem::getTextContent)
                    .filter(t -> t != null && !t.trim().isEmpty())
                    .collect(java.util.stream.Collectors.joining("\n"));
            record.setReplyType(replyResult.getItems().get(0).getReplyType());
            
            log.info("【账号{}】回复策略生成内容: type={}, keyword={}, itemCount={}", 
                    accountId, replyResult.getItems().get(0).getReplyType(), replyResult.getMatchedKeyword(),
                    replyResult.getItems().size());
            
            // 7. 保存触发上下文
            try {
                XianyuGoodsConfig goodsConfig = goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
                if (goodsConfig != null && goodsConfig.getFixedMaterial() != null && !goodsConfig.getFixedMaterial().isEmpty()) {
                    triggerContext.setFixedMaterial(goodsConfig.getFixedMaterial());
                }
                
                XianyuGoodsInfo goodsInfoForContext = goodsInfoMapper.selectOne(
                    new LambdaQueryWrapper<XianyuGoodsInfo>().eq(XianyuGoodsInfo::getXyGoodId, xyGoodsId)
                );
                if (goodsInfoForContext != null && goodsInfoForContext.getDetailInfo() != null && !goodsInfoForContext.getDetailInfo().isEmpty()) {
                    triggerContext.setGoodsDetail(goodsInfoForContext.getDetailInfo());
                }
            } catch (Exception e) {
                log.warn("【账号{}】获取固定资料和商品详情失败: {}", accountId, e.getMessage());
            }
            
            try {
                String triggerContextJson = objectMapper.writeValueAsString(triggerContext);
                record.setTriggerContext(triggerContextJson);
                autoReplyRecordMapper.updateTriggerContext(record.getId(), triggerContextJson);
            } catch (Exception e) {
                log.warn("【账号{}】序列化触发上下文失败，跳过保存: {}", accountId, e.getMessage());
            }
            
            // 8. 发送回复消息
            boolean sendSuccess = false;
            String cid = sId.replace("@goofish", "");
            String toId = cid;
            
            for (ReplyStrategy.ReplyResult.ReplyItem item : replyResult.getItems()) {
                if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                    boolean imageSent = webSocketService.sendImageMessageWithResult(accountId, cid, toId, item.getImageUrl(), 0, 0);
                    if (!imageSent) {
                        log.warn("【账号{}】发送回复图片失败: {}", accountId, item.getImageUrl());
                    } else {
                        sendSuccess = true;
                        sentMessageSaveService.saveAiImageReply(accountId, cid, toId, item.getImageUrl(), xyGoodsId);
                    }
                }
                if (item.getTextContent() != null && !item.getTextContent().trim().isEmpty()) {
                    boolean textSent = webSocketService.sendMessage(accountId, cid, toId, item.getTextContent());
                    if (textSent) {
                        sendSuccess = true;
                    }
                }
            }
            
            // 9. 更新记录状态
            if (sendSuccess) {
                log.info("【账号{}】自动回复成功: xyGoodsId={}, sId={}", accountId, xyGoodsId, sId);
                updateRecordState(record.getId(), 1, allReplyText);
                
                if (allReplyText != null && !allReplyText.trim().isEmpty()) {
                    sentMessageSaveService.saveAiAssistantReply(accountId, cid, toId, allReplyText, xyGoodsId);
                }
            } else {
                log.error("【账号{}】自动回复发送失败: xyGoodsId={}, sId={}", accountId, xyGoodsId, sId);
                updateRecordState(record.getId(), -1, allReplyText);
            }
            
        } catch (Exception e) {
            log.error("【账号{}】执行自动回复异常: xyGoodsId={}, sId={}", accountId, xyGoodsId, sId, e);
        }
    }
    
    @Override
    public boolean isAutoReplyEnabled(Long accountId, String xyGoodsId) {
        return isAnyReplyEnabled(accountId, xyGoodsId);
    }
    
    private boolean isAnyReplyEnabled(Long accountId, String xyGoodsId) {
        if (accountId == null || xyGoodsId == null) {
            return false;
        }
        try {
            XianyuGoodsConfig goodsConfig = goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
            if (goodsConfig == null) {
                return false;
            }
            boolean aiOn = goodsConfig.getXianyuAutoReplyOn() != null && goodsConfig.getXianyuAutoReplyOn() == 1;
            boolean keywordOn = goodsConfig.getXianyuKeywordReplyOn() != null && goodsConfig.getXianyuKeywordReplyOn() == 1;
            return aiOn || keywordOn;
        } catch (Exception e) {
            log.error("【账号{}】检查回复开关异常: xyGoodsId={}", accountId, xyGoodsId, e);
            return false;
        }
    }
    
    private void updateRecordState(Long recordId, Integer state, String replyContent) {
        try {
            autoReplyRecordMapper.updateStateAndContent(recordId, state, replyContent);
        } catch (Exception e) {
            log.error("更新回复记录状态失败: recordId={}, state={}", recordId, state, e);
        }
    }
}
