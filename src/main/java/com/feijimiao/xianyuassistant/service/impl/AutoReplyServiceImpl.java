package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.config.rag.DynamicAIChatClientManager;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoReplyConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoReplyRecord;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;
import com.feijimiao.xianyuassistant.entity.XianyuChatMessage;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import com.feijimiao.xianyuassistant.entity.bo.AutoReplyTriggerContext;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageData;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoReplyConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoReplyRecordMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsInfoMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuChatMessageMapper;
import com.feijimiao.xianyuassistant.service.AIService;
import com.feijimiao.xianyuassistant.service.AutoReplyService;
import com.feijimiao.xianyuassistant.service.WebSocketService;
import com.feijimiao.xianyuassistant.service.bo.RAGReplyResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 自动回复类型
     */
    private static final int REPLY_TYPE_AUTO = 2;
    
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
        
        // 取最后一条消息作为主消息（用于获取accountId、xyGoodsId、sId等）
        ChatMessageData lastMessage = messageList.get(messageList.size() - 1);
        Long accountId = lastMessage.getXianyuAccountId();
        String xyGoodsId = lastMessage.getXyGoodsId();
        String sId = lastMessage.getSId();
        String pnmId = lastMessage.getPnmId();
        
        // 拼接所有消息内容作为AI输入
        String buyerMessage = messageList.stream()
                .map(ChatMessageData::getMsgContent)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
        
        log.info("【账号{}】开始执行自动回复: xyGoodsId={}, sId={}, 触发消息数={}, buyerMessage={}", 
                accountId, xyGoodsId, sId, messageList.size(), buyerMessage);
        
        try {
            // 1. 检查AI服务是否可用
            if (!dynamicAIChatClientManager.isAvailable() || aiService == null) {
                log.warn("【账号{}】AI服务未启用或API Key未配置，跳过自动回复", accountId);
                return;
            }
            
            // 2. 检查商品是否开启自动回复开关
            if (!isAutoReplyEnabled(accountId, xyGoodsId)) {
                log.info("【账号{}】商品未开启自动回复开关: xyGoodsId={}", accountId, xyGoodsId);
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
            
            // 4. 构建触发上下文 - 触发消息列表
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
            record.setReplyType(REPLY_TYPE_AUTO);
            record.setState(0); // 待回复
            
            int insertResult;
            try {
                insertResult = autoReplyRecordMapper.insert(record);
            } catch (Exception e) {
                // 检查是否是唯一约束冲突
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
            
            log.info("【账号{}】创建回复记录成功: recordId={}", accountId, record.getId());
            
            // 6. 检查是否开启携带上下文
            boolean useContext = isContextEnabled(accountId, xyGoodsId);
            String contextMessages = null;
            if (useContext) {
                contextMessages = buildContextMessages(accountId, sId);
            }
            
            // 7. 调用AI服务生成回复（带命中资料）
            RAGReplyResult ragResult = generateReplyWithDetails(buyerMessage, xyGoodsId, accountId, contextMessages);
            
            String replyContent = ragResult != null ? ragResult.getReplyContent() : null;
            
            if (replyContent == null || replyContent.trim().isEmpty()) {
                log.warn("【账号{}】自动回复生成的回复内容为空", accountId);
                updateRecordState(record.getId(), -1, null);
                return;
            }
            
            log.info("【账号{}】自动回复生成内容: {}", accountId, 
                    replyContent.length() > 100 ? replyContent.substring(0, 100) + "..." : replyContent);
            
            // 7. 构建触发上下文 - RAG命中资料列表
            if (ragResult.getHitDetails() != null && !ragResult.getHitDetails().isEmpty()) {
                List<AutoReplyTriggerContext.RAGHitDetail> ragHitDetails = new ArrayList<>();
                for (RAGReplyResult.RAGHitDetail hit : ragResult.getHitDetails()) {
                    AutoReplyTriggerContext.RAGHitDetail detail = new AutoReplyTriggerContext.RAGHitDetail();
                    detail.setDocumentId(hit.getDocumentId());
                    detail.setContent(hit.getContent());
                    detail.setScore(hit.getScore());
                    ragHitDetails.add(detail);
                }
                triggerContext.setRagHitDetails(ragHitDetails);
            }
            
            // 7.1 保存携带的上下文消息到triggerContext
            if (contextMessages != null && !contextMessages.isEmpty()) {
                triggerContext.setContextMessages(contextMessages);
            }
            
            // 8. 序列化triggerContext为JSON并更新记录
            try {
                String triggerContextJson = objectMapper.writeValueAsString(triggerContext);
                record.setTriggerContext(triggerContextJson);
                autoReplyRecordMapper.updateTriggerContext(record.getId(), triggerContextJson);
                log.info("【账号{}】保存触发上下文成功: recordId={}, 触发消息数={}, RAG命中数={}, 携带上下文={}", 
                        accountId, record.getId(), triggerMessages.size(), 
                        triggerContext.getRagHitDetails() != null ? triggerContext.getRagHitDetails().size() : 0,
                        contextMessages != null ? "是" : "否");
            } catch (Exception e) {
                log.warn("【账号{}】序列化触发上下文失败，跳过保存: {}", accountId, e.getMessage());
            }
            
            // 9. 发送回复消息
            boolean sendSuccess = sendReplyMessage(accountId, sId, replyContent);
            
            // 10. 更新记录状态
            if (sendSuccess) {
                log.info("【账号{}】自动回复成功: xyGoodsId={}, sId={}", accountId, xyGoodsId, sId);
                updateRecordState(record.getId(), 1, replyContent);
                
                // AI自动回复成功后，将消息入库（contentType=888，AI助手回复）
                String cid = sId.replace("@goofish", "");
                String toId = cid;
                sentMessageSaveService.saveAiAssistantReply(accountId, cid, toId, replyContent, xyGoodsId);
            } else {
                log.error("【账号{}】自动回复发送失败: xyGoodsId={}, sId={}", accountId, xyGoodsId, sId);
                updateRecordState(record.getId(), -1, replyContent);
            }
            
        } catch (Exception e) {
            log.error("【账号{}】执行自动回复异常: xyGoodsId={}, sId={}", accountId, xyGoodsId, sId, e);
        }
    }
    
    @Override
    public boolean isAutoReplyEnabled(Long accountId, String xyGoodsId) {
        if (accountId == null || xyGoodsId == null) {
            return false;
        }
        
        try {
            // 检查商品是否开启自动回复开关
            XianyuGoodsConfig goodsConfig = goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
            if (goodsConfig == null || goodsConfig.getXianyuAutoReplyOn() == null || goodsConfig.getXianyuAutoReplyOn() != 1) {
                log.debug("【账号{}】商品未开启自动回复开关: xyGoodsId={}", accountId, xyGoodsId);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("【账号{}】检查自动回复开关异常: xyGoodsId={}", accountId, xyGoodsId, e);
            return false;
        }
    }
    
    /**
     * 调用AI服务生成回复（带RAG命中资料详情）
     */
    private RAGReplyResult generateReplyWithDetails(String buyerMessage, String xyGoodsId, Long accountId, String contextMessages) {
        try {
            log.info("【账号{}】调用AI服务生成回复(带命中资料): xyGoodsId={}, message={}, useContext={}", 
                    accountId, xyGoodsId, buyerMessage, contextMessages != null);
            
            RAGReplyResult result;
            if (contextMessages != null && !contextMessages.isEmpty()) {
                result = aiService.chatByRAGWithDetails(buyerMessage, xyGoodsId, contextMessages);
            } else {
                result = aiService.chatByRAGWithDetails(buyerMessage, xyGoodsId);
            }
            
            if (result != null && result.getReplyContent() != null) {
                log.info("【账号{}】响应完成，内容长度: {}, RAG命中数: {}", 
                        accountId, result.getReplyContent().length(), 
                        result.getHitDetails() != null ? result.getHitDetails().size() : 0);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("【账号{}】生成回复异常: xyGoodsId={}", accountId, xyGoodsId, e);
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
    
    /**
     * 检查商品是否开启携带上下文
     */
    private boolean isContextEnabled(Long accountId, String xyGoodsId) {
        if (accountId == null || xyGoodsId == null) {
            return false;
        }
        try {
            XianyuGoodsConfig goodsConfig = goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
            if (goodsConfig == null) {
                return true;
            }
            return goodsConfig.getXianyuAutoReplyContextOn() == null || goodsConfig.getXianyuAutoReplyContextOn() == 1;
        } catch (Exception e) {
            log.error("【账号{}】检查携带上下文开关异常: xyGoodsId={}", accountId, xyGoodsId, e);
            return false;
        }
    }
    
    /**
     * 根据会话ID构建上下文消息
     * 将买家消息(contentType=1,非自己发送)标记为user，卖家消息(contentType=1,自己发送)和AI消息(contentType=888)标记为assistant
     */
    private String buildContextMessages(Long accountId, String sId) {
        try {
            List<XianyuChatMessage> messages = chatMessageMapper.findBySId(sId);
            if (messages == null || messages.isEmpty()) {
                log.debug("【账号{}】会话{}无历史消息", accountId, sId);
                return null;
            }
            
            String ownUserId = accountService.getXianyuUserId(accountId);
            
            StringBuilder sb = new StringBuilder();
            int count = 0;
            int maxContextMessages = 20;
            
            for (XianyuChatMessage msg : messages) {
                if (count >= maxContextMessages) break;
                if (msg.getMsgContent() == null || msg.getMsgContent().trim().isEmpty()) continue;
                
                String role;
                if (msg.getContentType() != null && msg.getContentType() == 888) {
                    role = "assistant";
                } else if (msg.getContentType() != null && msg.getContentType() == 1) {
                    if (ownUserId != null && ownUserId.equals(msg.getSenderUserId())) {
                        role = "assistant";
                    } else {
                        role = "user";
                    }
                } else {
                    continue;
                }
                
                if (count > 0) {
                    sb.append("\n");
                }
                sb.append(role).append(": ").append(msg.getMsgContent());
                count++;
            }
            
            String result = sb.toString();
            if (result.isEmpty()) {
                return null;
            }
            
            log.info("【账号{}】构建上下文消息: sId={}, 消息数={}", accountId, sId, count);
            return result;
        } catch (Exception e) {
            log.error("【账号{}】构建上下文消息异常: sId={}", accountId, sId, e);
            return null;
        }
    }
}
