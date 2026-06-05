package com.feijimiao.xianyuassistant.service.delivery;

import com.feijimiao.xianyuassistant.entity.XianyuKamiItem;
import com.feijimiao.xianyuassistant.entity.XianyuKamiUsageRecord;
import com.feijimiao.xianyuassistant.mapper.XianyuKamiUsageRecordMapper;
import com.feijimiao.xianyuassistant.service.KamiConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 卡密发货策略（deliveryMode=2）
 *
 * <p>从卡密仓库获取可用卡密，用模板替换 {kmKey} 占位符后返回发货内容。</p>
 *
 * <h3>流程：</h3>
 * <ol>
 *   <li>遍历绑定的卡密配置ID列表（逗号分隔）</li>
 *   <li>调用 {@link KamiConfigService#acquireKami} 获取一条未使用卡密</li>
 *   <li>记录卡密使用记录</li>
 *   <li>用模板替换 {kmKey} 占位符</li>
 * </ol>
 */
@Slf4j
@Component
public class KamiDeliveryStrategy implements DeliveryContentStrategy {

    @Autowired
    private KamiConfigService kamiConfigService;

    @Autowired
    private XianyuKamiUsageRecordMapper kamiUsageRecordMapper;

    @Override
    public boolean supports(int deliveryMode) {
        return deliveryMode == 2;
    }

    @Override
    public String resolve(DeliveryContext context) {
        String content = acquireKamiContent(
                context.getDeliveryConfig().getKamiConfigIds(),
                context.getDeliveryConfig().getKamiDeliveryTemplate(),
                context.getOrderId(),
                context.getAccountId(),
                context.getXyGoodsId(),
                context.getSId(),
                context.getBuyerUserName()
        );
        if (content == null) {
            log.warn("【账号{}】卡密发货模式下无可用卡密: xyGoodsId={}, kamiConfigIds={}",
                    context.getAccountId(), context.getXyGoodsId(), context.getDeliveryConfig().getKamiConfigIds());
            return null;
        }
        log.info("【账号{}】卡密发货模式: content长度={}", context.getAccountId(), content.length());
        return content;
    }

    private String acquireKamiContent(String kamiConfigIds, String kamiDeliveryTemplate,
                                       String orderId, Long accountId, String xyGoodsId, String sId, String buyerUserName) {
        if (kamiConfigIds == null || kamiConfigIds.trim().isEmpty()) {
            log.warn("【账号{}】卡密发货未绑定卡密配置: xyGoodsId={}", accountId, xyGoodsId);
            return null;
        }

        String[] configIdArr = kamiConfigIds.split(",");
        for (String configIdStr : configIdArr) {
            try {
                Long configId = Long.parseLong(configIdStr.trim());
                XianyuKamiItem kamiItem = kamiConfigService.acquireKami(configId, orderId);
                if (kamiItem != null) {
                    XianyuKamiUsageRecord usageRecord = new XianyuKamiUsageRecord();
                    usageRecord.setKamiConfigId(configId);
                    usageRecord.setKamiItemId(kamiItem.getId());
                    usageRecord.setXianyuAccountId(accountId);
                    usageRecord.setXyGoodsId(xyGoodsId);
                    usageRecord.setOrderId(orderId);
                    usageRecord.setKamiContent(kamiItem.getKamiContent());
                    String cid = sId.replace("@goofish", "");
                    usageRecord.setBuyerUserId(cid);
                    usageRecord.setBuyerUserName(buyerUserName);
                    kamiUsageRecordMapper.insert(usageRecord);
                    log.info("【账号{}】卡密扣减成功: configId={}, itemId={}, orderId={}", accountId, configId, kamiItem.getId(), orderId);

                    String kamiContent = kamiItem.getKamiContent();
                    if (kamiDeliveryTemplate != null && !kamiDeliveryTemplate.trim().isEmpty()) {
                        kamiContent = kamiDeliveryTemplate.replace("{kmKey}", kamiContent);
                    }
                    return kamiContent;
                }
            } catch (NumberFormatException e) {
                log.warn("【账号{}】卡密配置ID格式错误: {}", accountId, configIdStr);
            }
        }
        return null;
    }
}
