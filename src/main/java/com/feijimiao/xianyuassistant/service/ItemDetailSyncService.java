package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.controller.dto.SyncProgressRespDTO;
import com.feijimiao.xianyuassistant.controller.dto.ItemDTO;
import java.util.List;

public interface ItemDetailSyncService {
    String startSync(Long accountId, List<ItemDTO> items);
    SyncProgressRespDTO getProgress(String syncId);
    void cancelSync(String syncId);
    boolean isSyncing(Long accountId);
}
