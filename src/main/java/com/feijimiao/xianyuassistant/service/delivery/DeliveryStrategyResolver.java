package com.feijimiao.xianyuassistant.service.delivery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 发货内容策略解析器
 *
 * <p>根据 deliveryMode 从策略列表中找到匹配的策略执行。</p>
 * <p>策略列表由Spring自动注入所有 {@link DeliveryContentStrategy} 实现。</p>
 */
@Slf4j
@Component
public class DeliveryStrategyResolver {

    @Autowired
    private List<DeliveryContentStrategy> strategies;

    /**
     * 根据发货模式解析发货内容
     *
     * @param deliveryMode 发货模式（1=文本，2=卡密）
     * @param context      发货上下文
     * @return 发货内容文本，null表示无法发货
     */
    public String resolve(int deliveryMode, DeliveryContext context) {
        for (DeliveryContentStrategy strategy : strategies) {
            if (strategy.supports(deliveryMode)) {
                return strategy.resolve(context);
            }
        }
        log.warn("【账号{}】未知的发货模式: deliveryMode={}", context.getAccountId(), deliveryMode);
        return null;
    }
}
