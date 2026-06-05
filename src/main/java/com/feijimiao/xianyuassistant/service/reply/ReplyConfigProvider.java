package com.feijimiao.xianyuassistant.service.reply;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 自动回复配置提供者
 *
 * <p>从数据库查询自动回复相关的配置项，封装为简单方法供调度器使用。</p>
 *
 * <h3>提供三个配置项：</h3>
 * <ul>
 *   <li>{@link #getDelaySeconds} - AI回复延时秒数（买家消息到来后等N秒再回复，收集多条消息）</li>
 *   <li>{@link #isHumanInterventionEnabled} - 人工干预开关（是否开启卖家回复后暂停AI）</li>
 *   <li>{@link #getInterventionMinutes} - 人工干预持续时长（卖家回复后N分钟内不触发AI）</li>
 * </ul>
 */
@Slf4j
@Component
public class ReplyConfigProvider {

    /** 默认AI回复延时（秒），等待期间收集买家多条消息一并回复 */
    private static final int DEFAULT_DELAY_SECONDS = 15;

    /** 默认人工干预持续时长（分钟） */
    private static final int DEFAULT_INTERVENTION_MINUTES = 10;

    @Autowired
    private XianyuGoodsAutoDeliveryConfigMapper autoDeliveryConfigMapper;

    @Autowired
    private XianyuGoodsConfigMapper goodsConfigMapper;

    /**
     * 获取AI回复延时秒数
     *
     * <p>从自动发货配置表读取 ragDelaySeconds，未配置则默认15秒。</p>
     * <p>延时作用：买家连续发多条消息时，等N秒后一次性传给AI，避免逐条回复。</p>
     *
     * @param accountId 闲鱼账号ID
     * @param xyGoodsId 闲鱼商品ID
     * @return 延时秒数
     */
    public int getDelaySeconds(Long accountId, String xyGoodsId) {
        try {
            if (accountId == null || xyGoodsId == null) return DEFAULT_DELAY_SECONDS;
            XianyuGoodsAutoDeliveryConfig config = autoDeliveryConfigMapper.findByAccountIdAndGoodsIdNoSku(accountId, xyGoodsId);
            if (config != null && config.getRagDelaySeconds() != null && config.getRagDelaySeconds() > 0) {
                return config.getRagDelaySeconds();
            }
        } catch (Exception e) {
            log.warn("获取延时配置失败: {}", e.getMessage());
        }
        return DEFAULT_DELAY_SECONDS;
    }

    /**
     * 检查人工干预开关是否开启
     *
     * <p>从商品配置表读取 humanInterventionOn，1=开启，0或null=关闭。</p>
     *
     * @param accountId 闲鱼账号ID
     * @param xyGoodsId 闲鱼商品ID
     * @return true=开启人工干预
     */
    public boolean isHumanInterventionEnabled(Long accountId, String xyGoodsId) {
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
     * 获取人工干预持续时长（分钟）
     *
     * <p>从商品配置表读取 humanInterventionMinutes，未配置则默认10分钟。</p>
     * <p>含义：卖家手动回复后，N分钟内该会话不触发AI自动回复。</p>
     *
     * @param accountId 闲鱼账号ID
     * @param xyGoodsId 闲鱼商品ID
     * @return 干预时长（分钟）
     */
    public int getInterventionMinutes(Long accountId, String xyGoodsId) {
        try {
            if (accountId == null || xyGoodsId == null) return DEFAULT_INTERVENTION_MINUTES;
            XianyuGoodsConfig config = goodsConfigMapper.selectByAccountAndGoodsId(accountId, xyGoodsId);
            if (config != null && config.getHumanInterventionMinutes() != null && config.getHumanInterventionMinutes() > 0) {
                return config.getHumanInterventionMinutes();
            }
        } catch (Exception e) {
            log.warn("获取人工干预分钟数失败: {}", e.getMessage());
        }
        return DEFAULT_INTERVENTION_MINUTES;
    }
}
