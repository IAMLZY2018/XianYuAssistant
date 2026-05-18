package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.service.bo.*;

import java.util.List;

public interface DataBackupService {

    List<BackupModuleRespBO> getModules();

    BackupExportRespBO exportData(BackupExportReqBO reqBO);

    BackupImportRespBO importData(BackupImportReqBO reqBO);
}
