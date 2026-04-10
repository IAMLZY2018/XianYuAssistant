package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryRecord;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoReplyConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoReplyRecord;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryRecordMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoReplyConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoReplyRecordMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsConfigMapper;
import com.feijimiao.xianyuassistant.service.AutoDeliveryService;
import com.feijimiao.xianyuassistant.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 自动发货服务实现类
 */
@Slf4j
@Service
public class AutoDeliveryServiceImpl implements AutoDeliveryService {
    
    @Autowired
    private XianyuGoodsConfigMapper goodsConfigMapper;
    
    @Autowired
    private XianyuGoodsAutoDeliveryConfigMapper autoDeliveryConfigMapper;
    
    @Autowired
    private XianyuGoodsAutoDeliveryRecordMapper autoDeliveryRecordMapper;
    
    @Autowired
    private XianyuGoodsAutoReplyConfigMapper autoReplyConfigMapper;
    
    @Autowired
    private XianyuGoodsAutoReplyRecordMapper autoReplyRecordMapper;
    
    @Lazy
    @Autowired
    private WebSocketService webSocketService;
    
    @Override
    public XianyuGoodsConfig getGoodsConfig(Long accountId, String xyGoodsId) {
        return goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
    }
    
    @Override
    public XianyuGoodsAutoDeliveryConfig getAutoDeliveryConfig(Long accountId, String xyGoodsId) {
        return autoDeliveryConfigMapper.findByAccountIdAndGoodsId(accountId, xyGoodsId);
    }
    
    @Override
    public void saveOrUpdateGoodsConfig(XianyuGoodsConfig config) {
        XianyuGoodsConfig existing = goodsConfigMapper.selectByAccountAndGoodsId(
                config.getXianyuAccountId(), config.getXyGoodsId());
        
        if (existing == null) {
            goodsConfigMapper.insert(config);
        } else {
            config.setId(existing.getId());
            goodsConfigMapper.update(config);
        }
    }
    
    @Override
    public void saveOrUpdateAutoDeliveryConfig(XianyuGoodsAutoDeliveryConfig config) {
        XianyuGoodsAutoDeliveryConfig existing = autoDeliveryConfigMapper.findByAccountIdAndGoodsId(
                config.getXianyuAccountId(), config.getXyGoodsId());
        
        if (existing == null) {
            autoDeliveryConfigMapper.insert(config);
        } else {
            config.setId(existing.getId());
            autoDeliveryConfigMapper.updateById(config);
        }
    }
    
    @Override
    public void recordAutoDelivery(Long accountId, String xyGoodsId, String buyerUserId, String buyerUserName, String content, Integer state) {
        XianyuGoodsAutoDeliveryRecord record = new XianyuGoodsAutoDeliveryRecord();
        record.setXianyuAccountId(accountId);
        record.setXyGoodsId(xyGoodsId);
        record.setBuyerUserId(buyerUserId);
        record.setBuyerUserName(buyerUserName);
        record.setContent(content);
        record.setState(state);
        
        autoDeliveryRecordMapper.insert(record);
    }
    
    /**
     * 处理自动发货（带买家用户ID和用户名）
     */
    @Override
    public void handleAutoDelivery(Long accountId, String xyGoodsId, String sId, String buyerUserId, String buyerUserName) {
        try {
            log.info("【账号{}】处理自动发货: xyGoodsId={}, sId={}, buyerUserId={}, buyerUserName={}", 
                    accountId, xyGoodsId, sId, buyerUserId, buyerUserName);
            
            // 1. 检查商品是否开启自动发货
            XianyuGoodsConfig goodsConfig = getGoodsConfig(accountId, xyGoodsId);
            if (goodsConfig == null || goodsConfig.getXianyuAutoDeliveryOn() != 1) {
                log.info("【账号{}】商品未开启自动发货: xyGoodsId={}", accountId, xyGoodsId);
                return;
            }
            
            // 2. 获取自动发货配置
            XianyuGoodsAutoDeliveryConfig deliveryConfig = getAutoDeliveryConfig(accountId, xyGoodsId);
            if (deliveryConfig == null || deliveryConfig.getAutoDeliveryContent() == null || 
                    deliveryConfig.getAutoDeliveryContent().isEmpty()) {
                log.warn("【账号{}】商品未配置自动发货内容: xyGoodsId={}", accountId, xyGoodsId);
                recordAutoDelivery(accountId, xyGoodsId, buyerUserId, buyerUserName, null, 0);
                return;
            }
            
            String content = deliveryConfig.getAutoDeliveryContent();
            log.info("【账号{}】准备发送自动发货消息: content={}", accountId, content);

            // 3. 模拟人工操作：阅读消息 + 思考 + 打字延迟

            // 3.1 阅读买家消息的延迟（1-3秒）
            com.feijimiao.xianyuassistant.utils.HumanLikeDelayUtils.mediumDelay();

            // 3.2 思考延迟（1-4秒）
            com.feijimiao.xianyuassistant.utils.HumanLikeDelayUtils.thinkingDelay();

            // 3.3 打字延迟（根据内容长度）
            com.feijimiao.xianyuassistant.utils.HumanLikeDelayUtils.typingDelay(content.length());
            
            // 4. 从sId中提取cid和toId
            // sId格式: "55435931514@goofish"
            String cid = sId.replace("@goofish", "");
            String toId = cid; // 对于闲鱼，cid和toId通常相同
            
            // 5. 发送消息
            boolean success = webSocketService.sendMessage(accountId, cid, toId, content);
            
            // 6. 记录发货结果（传递买家用户ID和用户名）
            recordAutoDelivery(accountId, xyGoodsId, buyerUserId, buyerUserName, content, success ? 1 : 0);
            
            if (success) {
                log.info("【账号{}】自动发货成功: xyGoodsId={}, buyerUserName={}, content={}", 
                        accountId, xyGoodsId, buyerUserName, content);
            } else {
                log.error("【账号{}】自动发货失败: xyGoodsId={}", accountId, xyGoodsId);
            }
            
        } catch (Exception e) {
            log.error("【账号{}】自动发货异常: xyGoodsId={}", accountId, xyGoodsId, e);
            recordAutoDelivery(accountId, xyGoodsId, buyerUserId, buyerUserName, null, 0);
        }
    }
    
    @Override
    public void handleAutoReply(Long accountId, String xyGoodsId, String sId, String buyerMessage) {
        try {
            log.info("【账号{}】处理自动回复: xyGoodsId={}, sId={}, buyerMessage={}", 
                    accountId, xyGoodsId, sId, buyerMessage);
            
            // 1. 检查商品是否开启自动回复
            XianyuGoodsConfig goodsConfig = getGoodsConfig(accountId, xyGoodsId);
            if (goodsConfig == null || goodsConfig.getXianyuAutoReplyOn() != 1) {
                log.info("【账号{}】商品未开启自动回复: xyGoodsId={}", accountId, xyGoodsId);
                return;
            }
            
            // 2. 获取自动回复配置列表
            XianyuGoodsAutoReplyConfig replyConfig = autoReplyConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
            List<XianyuGoodsAutoReplyConfig> replyConfigs = new ArrayList<>();
            if (replyConfig != null) {
                replyConfigs.add(replyConfig);
            }
            if (replyConfigs.isEmpty()) {
                log.info("【账号{}】商品未配置自动回复规则: xyGoodsId={}", accountId, xyGoodsId);
                return;
            }
            
            // 3. 匹配关键词
            XianyuGoodsAutoReplyConfig matchedConfig = null;
            String matchedKeyword = null;
            
            for (XianyuGoodsAutoReplyConfig config : replyConfigs) {
                String[] keywords = config.getKeyword().split(",");
                
                for (String keyword : keywords) {
                    keyword = keyword.trim();
                    
                    boolean matched = false;
                    Integer matchType = config.getMatchType();
                    
                    if (matchType == null || matchType == 1) {
                        // 包含匹配
                        matched = buyerMessage.contains(keyword);
                    } else if (matchType == 2) {
                        // 完全匹配
                        matched = buyerMessage.equals(keyword);
                    } else if (matchType == 3) {
                        // 正则匹配
                        try {
                            matched = Pattern.matches(keyword, buyerMessage);
                        } catch (Exception e) {
                            log.warn("【账号{}】正则表达式错误: {}", accountId, keyword, e);
                        }
                    }
                    
                    if (matched) {
                        matchedConfig = config;
                        matchedKeyword = keyword;
                        log.info("【账号{}】匹配到关键词: {}, 匹配类型: {}", accountId, keyword, matchType);
                        break;
                    }
                }
                
                if (matchedConfig != null) {
                    break;
                }
            }
            
            // 4. 如果没有匹配到关键词，不回复
            if (matchedConfig == null) {
                log.info("【账号{}】买家消息未匹配到任何关键词: {}", accountId, buyerMessage);
                return;
            }
            
            String replyContent = matchedConfig.getReplyContent();
            log.info("【账号{}】准备发送自动回复: content={}", accountId, replyContent);
            
            // 5. 模拟人工操作：阅读消息 + 思考 + 打字延迟

            // 5.1 阅读买家消息的延迟
            com.feijimiao.xianyuassistant.utils.HumanLikeDelayUtils.readingDelay(buyerMessage.length());

            // 5.2 思考延迟（1-4秒）
            com.feijimiao.xianyuassistant.utils.HumanLikeDelayUtils.thinkingDelay();

            // 5.3 打字延迟（根据回复内容长度）
            com.feijimiao.xianyuassistant.utils.HumanLikeDelayUtils.typingDelay(replyContent.length());
            
            // 6. 从sId中提取cid和toId
            String cid = sId.replace("@goofish", "");
            String toId = cid;
            
            // 7. 发送消息
            boolean success = webSocketService.sendMessage(accountId, cid, toId, replyContent);
            
            // 8. 记录回复结果
            recordAutoReply(accountId, xyGoodsId, buyerMessage, replyContent, matchedKeyword, success ? 1 : 0);
            
            if (success) {
                log.info("【账号{}】自动回复成功: xyGoodsId={}, keyword={}, reply={}", 
                        accountId, xyGoodsId, matchedKeyword, replyContent);
            } else {
                log.error("【账号{}】自动回复失败: xyGoodsId={}", accountId, xyGoodsId);
            }
            
        } catch (Exception e) {
            log.error("【账号{}】自动回复异常: xyGoodsId={}", accountId, xyGoodsId, e);
        }
    }
    
    /**
     * 记录自动回复
     */
    private void recordAutoReply(Long accountId, String xyGoodsId, String buyerMessage, 
                                  String replyContent, String matchedKeyword, Integer state) {
        try {
            XianyuGoodsAutoReplyRecord record = new XianyuGoodsAutoReplyRecord();
            record.setXianyuAccountId(accountId);
            record.setXyGoodsId(xyGoodsId);
            record.setBuyerMessage(buyerMessage);
            record.setReplyContent(replyContent);
            record.setMatchedKeyword(matchedKeyword);
            record.setState(state);
            
            autoReplyRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("【账号{}】记录自动回复失败", accountId, e);
        }
    }
    
    @Override
    public com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryRecordRespDTO getAutoDeliveryRecords(
            com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryRecordReqDTO reqDTO) {
        
        Long accountId = reqDTO.getXianyuAccountId();
        String xyGoodsId = reqDTO.getXyGoodsId();
        int pageNum = reqDTO.getPageNum() != null ? reqDTO.getPageNum() : 1;
        int pageSize = reqDTO.getPageSize() != null ? reqDTO.getPageSize() : 20;
        
        // 计算偏移量
        int offset = (pageNum - 1) * pageSize;
        
        // 查询记录
        List<XianyuGoodsAutoDeliveryRecord> records = autoDeliveryRecordMapper.selectByAccountIdWithPage(
                accountId, xyGoodsId, pageSize, offset);
        
        // 统计总数
        long total = autoDeliveryRecordMapper.countByAccountId(accountId, xyGoodsId);
        
        // 转换为DTO
        List<com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryRecordDTO> recordDTOs = new ArrayList<>();
        for (XianyuGoodsAutoDeliveryRecord record : records) {
            com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryRecordDTO dto = 
                    new com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryRecordDTO();
            dto.setId(record.getId());
            dto.setXyGoodsId(record.getXyGoodsId());
            dto.setGoodsTitle(record.getGoodsTitle());
            dto.setBuyerUserName(record.getBuyerUserName());
            dto.setContent(record.getContent());
            dto.setState(record.getState());
            dto.setOrderId(record.getOrderId());
            dto.setCreateTime(record.getCreateTime());
            recordDTOs.add(dto);
        }
        
        // 构建响应
        com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryRecordRespDTO respDTO = 
                new com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryRecordRespDTO();
        respDTO.setRecords(recordDTOs);
        respDTO.setTotal(total);
        respDTO.setPageNum(pageNum);
        respDTO.setPageSize(pageSize);
        
        return respDTO;
    }

    @Override
    public com.feijimiao.xianyuassistant.common.ResultObject<String> triggerAutoDelivery(
            com.feijimiao.xianyuassistant.controller.dto.TriggerAutoDeliveryReqDTO reqDTO) {
        try {
            Long accountId = reqDTO.getXianyuAccountId();
            String xyGoodsId = reqDTO.getXyGoodsId();
            String orderId = reqDTO.getOrderId();

            log.info("【账号{}】触发自动发货: xyGoodsId={}, orderId={}", accountId, xyGoodsId, orderId);

            // 1. 获取订单信息
            com.feijimiao.xianyuassistant.entity.XianyuOrder order = 
                    autoDeliveryRecordMapper.selectOrderByOrderId(accountId, orderId);
            
            if (order == null) {
                log.warn("【账号{}】订单不存在: orderId={}", accountId, orderId);
                return com.feijimiao.xianyuassistant.common.ResultObject.failed("订单不存在");
            }

            // 2. 获取会话ID (sId)
            String sId = order.getSId();
            if (sId == null || sId.isEmpty()) {
                log.warn("【账号{}】订单没有会话ID: orderId={}", accountId, orderId);
                return com.feijimiao.xianyuassistant.common.ResultObject.failed("订单没有会话ID");
            }

            // 3. 获取买家信息
            String buyerUserId = order.getBuyerUserId();
            String buyerUserName = order.getBuyerUserName();

            // 4. 调用handleAutoDelivery触发自动发货
            handleAutoDelivery(accountId, xyGoodsId, sId, buyerUserId, buyerUserName);

            return com.feijimiao.xianyuassistant.common.ResultObject.success("触发自动发货成功");

        } catch (Exception e) {
            log.error("【账号{}】触发自动发货失败: xyGoodsId={}, orderId={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getOrderId(), e);
            return com.feijimiao.xianyuassistant.common.ResultObject.failed("触发自动发货失败: " + e.getMessage());
        }
    }
}
