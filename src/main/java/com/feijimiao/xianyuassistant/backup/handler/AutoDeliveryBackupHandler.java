package com.feijimiao.xianyuassistant.backup.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feijimiao.xianyuassistant.backup.DataBackupHandler;
import com.feijimiao.xianyuassistant.entity.XianyuAccount;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import com.feijimiao.xianyuassistant.mapper.XianyuAccountMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class AutoDeliveryBackupHandler implements DataBackupHandler {

    @Autowired
    private XianyuGoodsAutoDeliveryConfigMapper autoDeliveryConfigMapper;

    @Autowired
    private XianyuAccountMapper accountMapper;

    @Override
    public String getModuleKey() {
        return "autoDelivery";
    }

    @Override
    public String getModuleName() {
        return "自动发货";
    }

    @Override
    public Map<String, Object> exportData() {
        List<XianyuGoodsAutoDeliveryConfig> configs = autoDeliveryConfigMapper.selectList(null);

        List<Map<String, Object>> result = new ArrayList<>();
        for (XianyuGoodsAutoDeliveryConfig config : configs) {
            XianyuAccount account = accountMapper.selectById(config.getXianyuAccountId());
            if (account == null) continue;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("unb", account.getUnb());
            map.put("xyGoodsId", config.getXyGoodsId());
            map.put("skuId", config.getSkuId());
            map.put("skuName", config.getSkuName());
            map.put("deliveryMode", config.getDeliveryMode());
            map.put("autoDeliveryContent", config.getAutoDeliveryContent());
            map.put("kamiConfigIds", config.getKamiConfigIds());
            map.put("kamiDeliveryTemplate", config.getKamiDeliveryTemplate());
            map.put("autoDeliveryImageUrl", config.getAutoDeliveryImageUrl());
            map.put("autoConfirmShipment", config.getAutoConfirmShipment());
            result.add(map);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("autoDeliveryConfigs", result);
        return data;
    }

    @Override
    public void importData(Map<String, Object> data, Map<String, Object> context) {
        if (data == null) return;

        @SuppressWarnings("unchecked")
        Map<String, Long> unbToAccountId = context.get("unbToAccountId") != null
                ? (Map<String, Long>) context.get("unbToAccountId")
                : Collections.emptyMap();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> configMaps = (List<Map<String, Object>>) data.get("autoDeliveryConfigs");
        if (configMaps == null) return;

        int skippedCount = 0;
        for (Map<String, Object> map : configMaps) {
            try {
                String unb = (String) map.get("unb");
                String xyGoodsId = (String) map.get("xyGoodsId");
                if (unb == null || xyGoodsId == null) continue;

                Long accountId = unbToAccountId.get(unb);
                if (accountId == null) {
                    log.warn("[AutoDeliveryBackup] 跳过: 找不到账号, unb={}, xyGoodsId={}", unb, xyGoodsId);
                    skippedCount++;
                    continue;
                }

                String skuId = (String) map.get("skuId");
                LambdaQueryWrapper<XianyuGoodsAutoDeliveryConfig> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(XianyuGoodsAutoDeliveryConfig::getXianyuAccountId, accountId)
                       .eq(XianyuGoodsAutoDeliveryConfig::getXyGoodsId, xyGoodsId)
                       .eq(skuId == null, XianyuGoodsAutoDeliveryConfig::getSkuId, null)
                       .eq(skuId != null, XianyuGoodsAutoDeliveryConfig::getSkuId, skuId);
                XianyuGoodsAutoDeliveryConfig existing = autoDeliveryConfigMapper.selectOne(wrapper);

                XianyuGoodsAutoDeliveryConfig config = new XianyuGoodsAutoDeliveryConfig();
                config.setXianyuAccountId(accountId);
                config.setXyGoodsId(xyGoodsId);
                config.setSkuId(skuId);
                config.setSkuName((String) map.get("skuName"));
                config.setDeliveryMode(map.get("deliveryMode") != null ? ((Number) map.get("deliveryMode")).intValue() : null);
                config.setAutoDeliveryContent((String) map.get("autoDeliveryContent"));
                config.setKamiConfigIds((String) map.get("kamiConfigIds"));
                config.setKamiDeliveryTemplate((String) map.get("kamiDeliveryTemplate"));
                config.setAutoDeliveryImageUrl((String) map.get("autoDeliveryImageUrl"));
                config.setAutoConfirmShipment(map.get("autoConfirmShipment") != null ? ((Number) map.get("autoConfirmShipment")).intValue() : null);

                if (existing == null) {
                    autoDeliveryConfigMapper.insert(config);
                } else {
                    config.setId(existing.getId());
                    autoDeliveryConfigMapper.updateById(config);
                }
            } catch (Exception e) {
                log.warn("[AutoDeliveryBackup] 导入单条自动发货配置失败: {}", e.getMessage());
            }
        }
        if (skippedCount > 0) {
            log.warn("[AutoDeliveryBackup] 共跳过 {} 条数据（账号不存在）", skippedCount);
        }
    }
}
