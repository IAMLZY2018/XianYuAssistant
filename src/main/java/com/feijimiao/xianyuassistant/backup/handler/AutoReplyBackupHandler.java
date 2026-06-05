package com.feijimiao.xianyuassistant.backup.handler;

import com.feijimiao.xianyuassistant.backup.DataBackupHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class AutoReplyBackupHandler implements DataBackupHandler {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public String getModuleKey() {
        return "autoReply";
    }

    @Override
    public String getModuleName() {
        return "自动回复";
    }

    @Override
    public Map<String, Object> exportData() {
        List<Map<String, Object>> configs = jdbcTemplate.queryForList(
                "SELECT c.xy_goods_id, c.xianyu_auto_reply_on, c.xianyu_auto_reply_context_on, c.fixed_material, a.unb " +
                "FROM xianyu_goods_config c " +
                "LEFT JOIN xianyu_account a ON c.xianyu_account_id = a.id " +
                "WHERE c.xianyu_auto_reply_on IS NOT NULL");

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> config : configs) {
            if (config.get("unb") == null) continue;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("unb", config.get("unb"));
            map.put("xyGoodsId", config.get("xy_goods_id"));
            map.put("autoReplyOn", config.get("xianyu_auto_reply_on"));
            map.put("autoReplyContextOn", config.get("xianyu_auto_reply_context_on"));
            map.put("fixedMaterial", config.get("fixed_material"));
            result.add(map);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("autoReplyConfigs", result);
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
        List<Map<String, Object>> configMaps = (List<Map<String, Object>>) data.get("autoReplyConfigs");
        if (configMaps == null) return;

        int skippedCount = 0;
        for (Map<String, Object> map : configMaps) {
            try {
                String unb = (String) map.get("unb");
                String xyGoodsId = (String) map.get("xyGoodsId");
                if (unb == null || xyGoodsId == null) continue;

                Long accountId = unbToAccountId.get(unb);
                if (accountId == null) {
                    log.warn("[AutoReplyBackup] 跳过: 找不到账号, unb={}, xyGoodsId={}", unb, xyGoodsId);
                    skippedCount++;
                    continue;
                }

                Integer autoReplyOn = map.get("autoReplyOn") != null ? ((Number) map.get("autoReplyOn")).intValue() : null;
                Integer autoReplyContextOn = map.get("autoReplyContextOn") != null ? ((Number) map.get("autoReplyContextOn")).intValue() : null;
                String fixedMaterial = (String) map.get("fixedMaterial");

                List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                        "SELECT * FROM xianyu_goods_config WHERE xianyu_account_id = ? AND xy_goods_id = ?",
                        accountId, xyGoodsId);

                if (existing.isEmpty()) {
                    jdbcTemplate.update(
                            "INSERT INTO xianyu_goods_config (xianyu_account_id, xy_goods_id, xianyu_auto_reply_on, xianyu_auto_reply_context_on, fixed_material) VALUES (?, ?, ?, ?, ?)",
                            accountId, xyGoodsId, autoReplyOn, autoReplyContextOn, fixedMaterial);
                } else {
                    jdbcTemplate.update(
                            "UPDATE xianyu_goods_config SET xianyu_auto_reply_on = ?, xianyu_auto_reply_context_on = ?, fixed_material = ? WHERE xianyu_account_id = ? AND xy_goods_id = ?",
                            autoReplyOn, autoReplyContextOn, fixedMaterial, accountId, xyGoodsId);
                }
            } catch (Exception e) {
                log.warn("[AutoReplyBackup] 导入单条自动回复配置失败: {}", e.getMessage());
            }
        }
        if (skippedCount > 0) {
            log.warn("[AutoReplyBackup] 共跳过 {} 条数据（账号不存在）", skippedCount);
        }
    }
}
