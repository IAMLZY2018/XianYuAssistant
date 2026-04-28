package com.feijimiao.xianyuassistant.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.controller.dto.ItemDTO;
import com.feijimiao.xianyuassistant.controller.dto.SyncProgressRespDTO;
import com.feijimiao.xianyuassistant.service.AccountService;
import com.feijimiao.xianyuassistant.service.GoodsInfoService;
import com.feijimiao.xianyuassistant.service.ItemDetailSyncService;
import com.feijimiao.xianyuassistant.utils.XianyuApiUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ItemDetailSyncServiceImpl implements ItemDetailSyncService {

    @Autowired
    private AccountService accountService;

    @Autowired
    private GoodsInfoService goodsInfoService;

    @Autowired
    private ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, SyncProgress> progressMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> accountSyncMap = new ConcurrentHashMap<>();

    private static class SyncProgress {
        String syncId;
        Long accountId;
        int totalCount;
        int completedCount = 0;
        int successCount = 0;
        int failedCount = 0;
        boolean isCompleted = false;
        boolean isRunning = true;
        String currentItemId = null;
        String message = "同步中...";
        long startTime;
        boolean cancelled = false;
    }

    @Override
    public String startSync(Long accountId, List<ItemDTO> items) {
        if (isSyncing(accountId)) {
            String existingSyncId = accountSyncMap.get(accountId);
            log.info("账号已有同步任务进行中: accountId={}, syncId={}", accountId, existingSyncId);
            return existingSyncId;
        }

        String syncId = UUID.randomUUID().toString();
        SyncProgress progress = new SyncProgress();
        progress.syncId = syncId;
        progress.accountId = accountId;
        progress.totalCount = items.size();
        progress.startTime = System.currentTimeMillis();

        progressMap.put(syncId, progress);
        accountSyncMap.put(accountId, syncId);

        String cookieStr = accountService.getCookieByAccountId(accountId);

        executeSync(syncId, accountId, items, cookieStr);

        log.info("启动异步详情同步: syncId={}, accountId={}, itemCount={}", syncId, accountId, items.size());
        return syncId;
    }

    @Async
    public void executeSync(String syncId, Long accountId, List<ItemDTO> items, String cookieStr) {
        SyncProgress progress = progressMap.get(syncId);
        if (progress == null) {
            log.error("同步进度不存在: syncId={}", syncId);
            return;
        }

        try {
            for (ItemDTO item : items) {
                if (progress.cancelled) {
                    progress.message = "同步已取消";
                    break;
                }

                String itemId = item.getDetailParams() != null ? item.getDetailParams().getItemId() : item.getId();
                if (itemId == null || itemId.isEmpty()) {
                    progress.completedCount++;
                    progress.failedCount++;
                    continue;
                }

                progress.currentItemId = itemId;

                try {
                    Thread.sleep(new Random().nextInt(501));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                boolean success = fetchAndSaveDetail(itemId, cookieStr, accountId);
                
                progress.completedCount++;
                if (success) {
                    progress.successCount++;
                } else {
                    progress.failedCount++;
                }

                progress.message = String.format("同步进度: %d/%d", progress.completedCount, progress.totalCount);
            }

            progress.isCompleted = true;
            progress.isRunning = false;
            progress.currentItemId = null;
            progress.message = String.format("同步完成: 成功%d, 失败%d", progress.successCount, progress.failedCount);

        } catch (Exception e) {
            log.error("异步同步异常: syncId={}", syncId, e);
            progress.isCompleted = true;
            progress.isRunning = false;
            progress.message = "同步失败: " + e.getMessage();
        } finally {
            accountSyncMap.remove(accountId);
        }
    }

    private boolean fetchAndSaveDetail(String itemId, String cookieStr, Long accountId) {
        try {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("itemId", itemId);

            String response = XianyuApiUtils.callApi(
                "mtop.taobao.idle.pc.detail",
                dataMap,
                cookieStr
            );

            if (response == null) {
                log.warn("获取商品详情失败，响应为空: itemId={}", itemId);
                return false;
            }

            String extractedDesc = extractDescFromDetailJson(response);
            
            if (extractedDesc != null && !extractedDesc.isEmpty()) {
                goodsInfoService.updateDetailInfo(itemId, extractedDesc);
                log.debug("商品详情同步成功: itemId={}", itemId);
                return true;
            } else {
                log.warn("商品详情提取失败: itemId={}", itemId);
                return false;
            }

        } catch (Exception e) {
            log.error("获取商品详情异常: itemId={}", itemId, e);
            return false;
        }
    }

    private String extractDescFromDetailJson(String detailJson) {
        try {
            JsonNode rootNode = objectMapper.readTree(detailJson);
            
            JsonNode dataNode = rootNode.get("data");
            if (dataNode == null || dataNode.isNull()) {
                log.warn("未找到data字段");
                return null;
            }

            JsonNode itemDONode = dataNode.get("itemDO");
            if (itemDONode == null || itemDONode.isNull()) {
                log.warn("未找到itemDO字段");
                return null;
            }

            JsonNode descNode = itemDONode.get("desc");
            if (descNode != null && !descNode.isNull()) {
                return descNode.asText();
            } else {
                log.warn("itemDO中未找到desc字段");
                return null;
            }

        } catch (Exception e) {
            log.error("解析商品详情JSON失败", e);
            return null;
        }
    }

    @Override
    public SyncProgressRespDTO getProgress(String syncId) {
        SyncProgress progress = progressMap.get(syncId);
        if (progress == null) {
            return null;
        }

        SyncProgressRespDTO dto = new SyncProgressRespDTO();
        dto.setSyncId(progress.syncId);
        dto.setAccountId(progress.accountId);
        dto.setTotalCount(progress.totalCount);
        dto.setCompletedCount(progress.completedCount);
        dto.setSuccessCount(progress.successCount);
        dto.setFailedCount(progress.failedCount);
        dto.setIsCompleted(progress.isCompleted);
        dto.setIsRunning(progress.isRunning);
        dto.setCurrentItemId(progress.currentItemId);
        dto.setMessage(progress.message);
        dto.setStartTime(progress.startTime);

        if (progress.completedCount > 0 && progress.totalCount > 0) {
            long elapsed = System.currentTimeMillis() - progress.startTime;
            long avgTimePerItem = elapsed / progress.completedCount;
            long remainingItems = progress.totalCount - progress.completedCount;
            dto.setEstimatedRemainingTime(avgTimePerItem * remainingItems);
        }

        return dto;
    }

    @Override
    public void cancelSync(String syncId) {
        SyncProgress progress = progressMap.get(syncId);
        if (progress != null) {
            progress.cancelled = true;
            progress.message = "正在取消同步...";
            log.info("取消同步: syncId={}", syncId);
        }
    }

    @Override
    public boolean isSyncing(Long accountId) {
        String syncId = accountSyncMap.get(accountId);
        if (syncId == null) {
            return false;
        }
        SyncProgress progress = progressMap.get(syncId);
        return progress != null && progress.isRunning && !progress.isCompleted;
    }
}
