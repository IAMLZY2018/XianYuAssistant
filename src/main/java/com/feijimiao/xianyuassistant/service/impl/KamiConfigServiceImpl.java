package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.*;
import com.feijimiao.xianyuassistant.entity.XianyuKamiConfig;
import com.feijimiao.xianyuassistant.entity.XianyuKamiItem;
import com.feijimiao.xianyuassistant.mapper.XianyuKamiConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuKamiItemMapper;
import com.feijimiao.xianyuassistant.service.EmailNotifyService;
import com.feijimiao.xianyuassistant.service.KamiConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KamiConfigServiceImpl implements KamiConfigService {

    @Autowired
    private XianyuKamiConfigMapper kamiConfigMapper;

    @Autowired
    private XianyuKamiItemMapper kamiItemMapper;

    @Autowired
    private EmailNotifyService emailNotifyService;

    private final ReentrantLock acquireLock = new ReentrantLock();

    @Override
    public ResultObject<KamiConfigRespDTO> createOrUpdateConfig(KamiConfigReqDTO reqDTO) {
        try {
            XianyuKamiConfig config;
            if (reqDTO.getId() != null) {
                config = kamiConfigMapper.selectById(reqDTO.getId());
                if (config == null) {
                    return ResultObject.failed("卡密配置不存在");
                }
            } else {
                config = new XianyuKamiConfig();
                config.setXianyuAccountId(reqDTO.getXianyuAccountId());
                config.setTotalCount(0);
                config.setUsedCount(0);
            }
            if (reqDTO.getAliasName() != null) {
                config.setAliasName(reqDTO.getAliasName());
            }
            if (reqDTO.getAlertEnabled() != null) {
                config.setAlertEnabled(reqDTO.getAlertEnabled());
            }
            if (reqDTO.getAlertThresholdType() != null) {
                config.setAlertThresholdType(reqDTO.getAlertThresholdType());
            }
            if (reqDTO.getAlertThresholdValue() != null) {
                config.setAlertThresholdValue(reqDTO.getAlertThresholdValue());
            }
            if (reqDTO.getAlertEmail() != null) {
                config.setAlertEmail(reqDTO.getAlertEmail());
            }
            if (reqDTO.getId() != null) {
                kamiConfigMapper.updateById(config);
            } else {
                kamiConfigMapper.insert(config);
            }
            return ResultObject.success(toConfigRespDTO(config));
        } catch (Exception e) {
            log.error("创建/更新卡密配置失败", e);
            return ResultObject.failed("创建/更新卡密配置失败: " + e.getMessage());
        }
    }

    @Override
    public ResultObject<List<KamiConfigRespDTO>> getConfigsByAccountId(Long xianyuAccountId) {
        try {
            List<XianyuKamiConfig> configs = kamiConfigMapper.findByAccountId(xianyuAccountId);
            List<KamiConfigRespDTO> result = configs.stream()
                    .map(this::toConfigRespDTO)
                    .collect(Collectors.toList());
            return ResultObject.success(result);
        } catch (Exception e) {
            log.error("查询卡密配置列表失败", e);
            return ResultObject.failed("查询卡密配置列表失败: " + e.getMessage());
        }
    }

    @Override
    public ResultObject<KamiConfigRespDTO> getConfigById(Long id) {
        try {
            XianyuKamiConfig config = kamiConfigMapper.selectById(id);
            if (config == null) {
                return ResultObject.failed("卡密配置不存在");
            }
            return ResultObject.success(toConfigRespDTO(config));
        } catch (Exception e) {
            log.error("查询卡密配置失败", e);
            return ResultObject.failed("查询卡密配置失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResultObject<Void> deleteConfig(Long id) {
        try {
            List<XianyuKamiItem> items = kamiItemMapper.findByConfigId(id);
            for (XianyuKamiItem item : items) {
                kamiItemMapper.deleteById(item.getId());
            }
            kamiConfigMapper.deleteById(id);
            return ResultObject.success(null);
        } catch (Exception e) {
            log.error("删除卡密配置失败", e);
            return ResultObject.failed("删除卡密配置失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResultObject<KamiItemRespDTO> addKamiItem(KamiItemReqDTO reqDTO) {
        try {
            XianyuKamiConfig config = kamiConfigMapper.selectById(reqDTO.getKamiConfigId());
            if (config == null) {
                return ResultObject.failed("卡密配置不存在");
            }
            XianyuKamiItem item = new XianyuKamiItem();
            item.setKamiConfigId(reqDTO.getKamiConfigId());
            item.setKamiContent(reqDTO.getKamiContent().trim());
            item.setStatus(0);
            item.setSortOrder(kamiItemMapper.countByConfigId(reqDTO.getKamiConfigId()));
            try {
                kamiItemMapper.insert(item);
            } catch (Exception e) {
                return ResultObject.failed("卡密内容重复，已存在相同卡密");
            }
            refreshConfigCounts(reqDTO.getKamiConfigId());
            return ResultObject.success(toItemRespDTO(item));
        } catch (Exception e) {
            log.error("添加卡密失败", e);
            return ResultObject.failed("添加卡密失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResultObject<Integer> batchImportKamiItems(KamiBatchImportReqDTO reqDTO) {
        try {
            XianyuKamiConfig config = kamiConfigMapper.selectById(reqDTO.getKamiConfigId());
            if (config == null) {
                return ResultObject.failed("卡密配置不存在");
            }
            String[] lines = reqDTO.getKamiContents().split("\\r?\\n");
            int baseOrder = kamiItemMapper.countByConfigId(reqDTO.getKamiConfigId());
            int added = 0;
            int duplicated = 0;
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                XianyuKamiItem item = new XianyuKamiItem();
                item.setKamiConfigId(reqDTO.getKamiConfigId());
                item.setKamiContent(trimmed);
                item.setStatus(0);
                item.setSortOrder(baseOrder + added);
                try {
                    kamiItemMapper.insert(item);
                    added++;
                } catch (Exception e) {
                    duplicated++;
                }
            }
            refreshConfigCounts(reqDTO.getKamiConfigId());
            String msg = duplicated > 0
                    ? String.format("成功导入%d条，跳过重复%d条", added, duplicated)
                    : String.format("成功导入%d条", added);
            return ResultObject.success(added, msg);
        } catch (Exception e) {
            log.error("批量导入卡密失败", e);
            return ResultObject.failed("批量导入卡密失败: " + e.getMessage());
        }
    }

    @Override
    public ResultObject<List<KamiItemRespDTO>> getKamiItemsByConfigId(Long kamiConfigId) {
        try {
            List<XianyuKamiItem> items = kamiItemMapper.findByConfigId(kamiConfigId);
            List<KamiItemRespDTO> result = items.stream()
                    .map(this::toItemRespDTO)
                    .collect(Collectors.toList());
            return ResultObject.success(result);
        } catch (Exception e) {
            log.error("查询卡密列表失败", e);
            return ResultObject.failed("查询卡密列表失败: " + e.getMessage());
        }
    }

    @Override
    public ResultObject<List<KamiItemRespDTO>> getKamiItemsByConfigIdWithFilter(KamiItemQueryReqDTO reqDTO) {
        try {
            List<XianyuKamiItem> items = kamiItemMapper.findByConfigIdWithFilter(
                    reqDTO.getKamiConfigId(), 
                    reqDTO.getStatus(), 
                    reqDTO.getKeyword());
            List<KamiItemRespDTO> result = items.stream()
                    .map(this::toItemRespDTO)
                    .collect(Collectors.toList());
            return ResultObject.success(result);
        } catch (Exception e) {
            log.error("查询卡密列表失败", e);
            return ResultObject.failed("查询卡密列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResultObject<Void> deleteKamiItem(Long id) {
        try {
            XianyuKamiItem item = kamiItemMapper.selectById(id);
            if (item == null) {
                return ResultObject.failed("卡密不存在");
            }
            kamiItemMapper.deleteById(id);
            refreshConfigCounts(item.getKamiConfigId());
            return ResultObject.success(null);
        } catch (Exception e) {
            log.error("删除卡密失败", e);
            return ResultObject.failed("删除卡密失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResultObject<Void> resetKamiItem(Long id) {
        try {
            int rows = kamiItemMapper.markUnused(id);
            if (rows == 0) {
                return ResultObject.failed("卡密状态重置失败，可能已是未使用状态");
            }
            return ResultObject.success(null);
        } catch (Exception e) {
            log.error("重置卡密状态失败", e);
            return ResultObject.failed("重置卡密状态失败: " + e.getMessage());
        }
    }

    @Override
    public XianyuKamiItem acquireKami(Long kamiConfigId, String orderId) {
        acquireLock.lock();
        try {
            XianyuKamiConfig config = kamiConfigMapper.selectById(kamiConfigId);
            if (config == null) {
                log.warn("卡密配置不存在: kamiConfigId={}", kamiConfigId);
                return null;
            }
            XianyuKamiItem item = kamiItemMapper.findNextUnused(kamiConfigId);
            
            if (item == null) {
                log.info("卡密配置[{}]无可用未使用卡密，尝试获取已使用卡密（允许重复）", kamiConfigId);
                List<XianyuKamiItem> allItems = kamiItemMapper.findByConfigId(kamiConfigId);
                if (allItems.isEmpty()) {
                    log.warn("卡密配置[{}]没有任何卡密", kamiConfigId);
                    return null;
                }
                item = allItems.get(0);
            } else {
                int marked = kamiItemMapper.markUsed(item.getId(), orderId);
                if (marked == 0) {
                    log.warn("卡密[{}]已被其他订单占用，并发冲突", item.getId());
                    return null;
                }
            }
            refreshConfigCounts(kamiConfigId);
            checkAndSendAlert(config, kamiConfigId);
            return item;
        } finally {
            acquireLock.unlock();
        }
    }

    @Override
    public XianyuKamiConfig getConfig(Long kamiConfigId) {
        return kamiConfigMapper.selectById(kamiConfigId);
    }

    private void refreshConfigCounts(Long kamiConfigId) {
        int total = kamiItemMapper.countByConfigId(kamiConfigId);
        int used = kamiItemMapper.countUsed(kamiConfigId);
        XianyuKamiConfig config = kamiConfigMapper.selectById(kamiConfigId);
        if (config != null) {
            config.setTotalCount(total);
            config.setUsedCount(used);
            kamiConfigMapper.updateById(config);
        }
    }

    private KamiConfigRespDTO toConfigRespDTO(XianyuKamiConfig config) {
        KamiConfigRespDTO dto = new KamiConfigRespDTO();
        dto.setId(config.getId());
        dto.setXianyuAccountId(config.getXianyuAccountId());
        dto.setAliasName(config.getAliasName());
        dto.setAlertEnabled(config.getAlertEnabled());
        dto.setAlertThresholdType(config.getAlertThresholdType());
        dto.setAlertThresholdValue(config.getAlertThresholdValue());
        dto.setAlertEmail(config.getAlertEmail());
        dto.setTotalCount(config.getTotalCount());
        dto.setUsedCount(config.getUsedCount());
        int unused = kamiItemMapper.countUnused(config.getId());
        dto.setAvailableCount(unused);
        dto.setCreateTime(config.getCreateTime());
        dto.setUpdateTime(config.getUpdateTime());
        return dto;
    }

    private KamiItemRespDTO toItemRespDTO(XianyuKamiItem item) {
        KamiItemRespDTO dto = new KamiItemRespDTO();
        dto.setId(item.getId());
        dto.setKamiConfigId(item.getKamiConfigId());
        dto.setKamiContent(item.getKamiContent());
        dto.setStatus(item.getStatus());
        dto.setOrderId(item.getOrderId());
        dto.setUsedTime(item.getUsedTime());
        dto.setSortOrder(item.getSortOrder());
        dto.setCreateTime(item.getCreateTime());
        return dto;
    }

    private void checkAndSendAlert(XianyuKamiConfig config, Long kamiConfigId) {
        if (config == null || config.getAlertEnabled() == null || config.getAlertEnabled() != 1) {
            return;
        }

        int availableCount = kamiItemMapper.countUnused(kamiConfigId);
        int totalCount = config.getTotalCount() != null ? config.getTotalCount() : 0;
        int thresholdValue = config.getAlertThresholdValue() != null ? config.getAlertThresholdValue() : 10;
        int thresholdType = config.getAlertThresholdType() != null ? config.getAlertThresholdType() : 1;

        boolean shouldAlert = false;
        if (thresholdType == 1) {
            shouldAlert = availableCount < thresholdValue;
        } else {
            if (totalCount > 0) {
                int percentage = (availableCount * 100) / totalCount;
                shouldAlert = percentage < thresholdValue;
            }
        }

        if (shouldAlert) {
            log.info("卡密库存触发预警: configId={}, available={}, total={}, thresholdType={}, thresholdValue={}",
                    kamiConfigId, availableCount, totalCount, thresholdType, thresholdValue);
            emailNotifyService.sendKamiAlertEmail(
                    config.getAlertEmail(),
                    config.getAliasName(),
                    availableCount,
                    totalCount
            );
        }
    }
}
