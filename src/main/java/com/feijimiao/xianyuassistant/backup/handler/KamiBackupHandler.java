package com.feijimiao.xianyuassistant.backup.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feijimiao.xianyuassistant.backup.DataBackupHandler;
import com.feijimiao.xianyuassistant.entity.XianyuAccount;
import com.feijimiao.xianyuassistant.entity.XianyuKamiConfig;
import com.feijimiao.xianyuassistant.entity.XianyuKamiItem;
import com.feijimiao.xianyuassistant.mapper.XianyuAccountMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuKamiConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuKamiItemMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class KamiBackupHandler implements DataBackupHandler {

    @Autowired
    private XianyuKamiConfigMapper kamiConfigMapper;

    @Autowired
    private XianyuKamiItemMapper kamiItemMapper;

    @Autowired
    private XianyuAccountMapper accountMapper;

    @Override
    public String getModuleKey() {
        return "kami";
    }

    @Override
    public String getModuleName() {
        return "卡密仓库";
    }

    @Override
    public Map<String, Object> exportData() {
        List<XianyuKamiConfig> kamiConfigs = kamiConfigMapper.selectList(null);

        List<Map<String, Object>> configList = new ArrayList<>();
        Map<Long, String> configIdToUnb = new HashMap<>();
        for (XianyuKamiConfig config : kamiConfigs) {
            XianyuAccount account = accountMapper.selectById(config.getXianyuAccountId());
            if (account == null) continue;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("unb", account.getUnb());
            map.put("aliasName", config.getAliasName());
            map.put("alertEnabled", config.getAlertEnabled());
            map.put("alertThresholdType", config.getAlertThresholdType());
            map.put("alertThresholdValue", config.getAlertThresholdValue());
            map.put("alertEmail", config.getAlertEmail());
            configList.add(map);
            configIdToUnb.put(config.getId(), account.getUnb());
        }

        List<Map<String, Object>> itemList = new ArrayList<>();
        for (XianyuKamiConfig config : kamiConfigs) {
            List<XianyuKamiItem> items = kamiItemMapper.findByConfigIdAndStatus(config.getId(), 0);
            String unb = configIdToUnb.get(config.getId());
            if (unb == null) continue;

            for (XianyuKamiItem item : items) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("unb", unb);
                map.put("aliasName", config.getAliasName());
                map.put("kamiContent", item.getKamiContent());
                itemList.add(map);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("kamiConfigs", configList);
        data.put("kamiItems", itemList);
        return data;
    }

    @Override
    public void importData(Map<String, Object> data, Map<String, Object> context) {
        if (data == null) return;

        @SuppressWarnings("unchecked")
        Map<String, Long> unbToAccountId = context.get("unbToAccountId") != null
                ? (Map<String, Long>) context.get("unbToAccountId")
                : Collections.emptyMap();

        Map<String, Long> configKeyToId = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> configMaps = (List<Map<String, Object>>) data.get("kamiConfigs");
        if (configMaps != null) {
            int skippedCount = 0;
            for (Map<String, Object> map : configMaps) {
                try {
                    String unb = (String) map.get("unb");
                    String aliasName = (String) map.get("aliasName");
                    if (unb == null || aliasName == null) continue;

                    Long accountId = unbToAccountId.get(unb);
                    if (accountId == null) {
                        log.warn("[KamiBackup] 跳过配置: 找不到账号, unb={}, aliasName={}", unb, aliasName);
                        skippedCount++;
                        continue;
                    }

                    LambdaQueryWrapper<XianyuKamiConfig> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(XianyuKamiConfig::getXianyuAccountId, accountId)
                           .eq(XianyuKamiConfig::getAliasName, aliasName);
                    XianyuKamiConfig existing = kamiConfigMapper.selectOne(wrapper);

                    XianyuKamiConfig config = new XianyuKamiConfig();
                    config.setXianyuAccountId(accountId);
                    config.setAliasName(aliasName);
                    config.setAlertEnabled(map.get("alertEnabled") != null ? ((Number) map.get("alertEnabled")).intValue() : null);
                    config.setAlertThresholdType(map.get("alertThresholdType") != null ? ((Number) map.get("alertThresholdType")).intValue() : null);
                    config.setAlertThresholdValue(map.get("alertThresholdValue") != null ? ((Number) map.get("alertThresholdValue")).intValue() : null);
                    config.setAlertEmail((String) map.get("alertEmail"));

                    if (existing == null) {
                        config.setTotalCount(0);
                        config.setUsedCount(0);
                        kamiConfigMapper.insert(config);
                    } else {
                        config.setId(existing.getId());
                        config.setTotalCount(existing.getTotalCount());
                        config.setUsedCount(existing.getUsedCount());
                        kamiConfigMapper.updateById(config);
                    }
                    configKeyToId.put(unb + ":" + aliasName, config.getId());
                } catch (Exception e) {
                    log.warn("[KamiBackup] 导入单条卡密配置失败: {}", e.getMessage());
                }
            }
            if (skippedCount > 0) {
                log.warn("[KamiBackup] 共跳过 {} 条配置数据（账号不存在）", skippedCount);
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemMaps = (List<Map<String, Object>>) data.get("kamiItems");
        if (itemMaps != null) {
            int skippedCount = 0;
            for (Map<String, Object> map : itemMaps) {
                try {
                    String unb = (String) map.get("unb");
                    String aliasName = (String) map.get("aliasName");
                    String kamiContent = (String) map.get("kamiContent");
                    if (unb == null || aliasName == null || kamiContent == null) continue;

                    Long configId = configKeyToId.get(unb + ":" + aliasName);
                    if (configId == null) {
                        skippedCount++;
                        continue;
                    }

                    LambdaQueryWrapper<XianyuKamiItem> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(XianyuKamiItem::getKamiConfigId, configId)
                           .eq(XianyuKamiItem::getKamiContent, kamiContent);
                    XianyuKamiItem existing = kamiItemMapper.selectOne(wrapper);
                    if (existing != null) continue;

                    XianyuKamiItem item = new XianyuKamiItem();
                    item.setKamiConfigId(configId);
                    item.setKamiContent(kamiContent);
                    item.setStatus(0);
                    item.setSortOrder(0);
                    kamiItemMapper.insert(item);
                } catch (Exception e) {
                    log.warn("[KamiBackup] 导入单条卡密项失败: {}", e.getMessage());
                }
            }
            if (skippedCount > 0) {
                log.warn("[KamiBackup] 共跳过 {} 条卡密项数据（配置不存在）", skippedCount);
            }
        }
    }
}
