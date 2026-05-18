package com.feijimiao.xianyuassistant.backup;

import java.util.Map;

public interface DataBackupHandler {

    String getModuleKey();

    String getModuleName();

    Map<String, Object> exportData();

    default void importData(Map<String, Object> data) {
        importData(data, new java.util.LinkedHashMap<>());
    }

    void importData(Map<String, Object> data, Map<String, Object> context);
}
