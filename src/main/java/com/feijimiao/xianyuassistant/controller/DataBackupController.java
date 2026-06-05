package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.*;
import com.feijimiao.xianyuassistant.service.DataBackupService;
import com.feijimiao.xianyuassistant.service.bo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/api/backup")
@CrossOrigin(origins = "*")
public class DataBackupController {

    @Autowired
    private DataBackupService dataBackupService;

    @PostMapping("/modules")
    public ResultObject<List<BackupModuleRespDTO>> getModules() {
        try {
            List<BackupModuleRespBO> boList = dataBackupService.getModules();
            List<BackupModuleRespDTO> result = new ArrayList<>();
            for (BackupModuleRespBO bo : boList) {
                BackupModuleRespDTO dto = new BackupModuleRespDTO();
                dto.setModuleKey(bo.getModuleKey());
                dto.setModuleName(bo.getModuleName());
                result.add(dto);
            }
            return ResultObject.success(result);
        } catch (Exception e) {
            log.error("获取备份模块列表失败", e);
            return ResultObject.failed("获取备份模块列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/export")
    public ResultObject<BackupExportRespDTO> exportData(@RequestBody BackupExportReqDTO reqDTO) {
        try {
            BackupExportReqBO reqBO = new BackupExportReqBO();
            reqBO.setModules(reqDTO.getModules());

            BackupExportRespBO respBO = dataBackupService.exportData(reqBO);

            BackupExportRespDTO respDTO = new BackupExportRespDTO();
            respDTO.setJsonData(respBO.getJsonData());
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("导出备份数据失败", e);
            return ResultObject.failed("导出备份数据失败: " + e.getMessage());
        }
    }

    @PostMapping("/import")
    public ResultObject<BackupImportRespDTO> importData(@RequestBody BackupImportReqDTO reqDTO) {
        try {
            if (reqDTO == null || reqDTO.getJsonData() == null || reqDTO.getJsonData().trim().isEmpty()) {
                return ResultObject.validateFailed("备份数据不能为空");
            }

            BackupImportReqBO reqBO = new BackupImportReqBO();
            reqBO.setJsonData(reqDTO.getJsonData());
            reqBO.setModules(reqDTO.getModules());

            BackupImportRespBO respBO = dataBackupService.importData(reqBO);

            BackupImportRespDTO respDTO = new BackupImportRespDTO();
            respDTO.setTotalCount(respBO.getTotalCount());
            respDTO.setSuccessCount(respBO.getSuccessCount());
            respDTO.setFailedModules(respBO.getFailedModules());
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("导入备份数据失败", e);
            return ResultObject.failed("导入备份数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/log-dates")
    public ResultObject<List<String>> getLogDates() {
        try {
            Path logDir = Paths.get("logs");
            if (!Files.exists(logDir)) {
                return ResultObject.success(new ArrayList<>());
            }

            File[] dirs = logDir.toFile().listFiles(File::isDirectory);
            if (dirs == null || dirs.length == 0) {
                return ResultObject.success(new ArrayList<>());
            }

            List<String> dates = new ArrayList<>();
            for (File dir : dirs) {
                String name = dir.getName();
                if (name.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    dates.add(name);
                }
            }

            dates.sort(Comparator.reverseOrder());
            return ResultObject.success(dates);
        } catch (Exception e) {
            log.error("获取日志日期列表失败", e);
            return ResultObject.failed("获取日志日期列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/log-download")
    public ResponseEntity<Resource> downloadLog(@RequestParam("date") String date) {
        try {
            Path logDir = Paths.get("logs", date);
            if (!Files.exists(logDir) || !Files.isDirectory(logDir)) {
                return ResponseEntity.notFound().build();
            }

            File[] logFiles = logDir.toFile().listFiles((dir, name) -> name.endsWith(".log"));
            if (logFiles == null || logFiles.length == 0) {
                return ResponseEntity.notFound().build();
            }

            Path zipPath = Paths.get("logs", date + ".zip");
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
                for (File logFile : logFiles) {
                    ZipEntry entry = new ZipEntry(logFile.getName());
                    zos.putNextEntry(entry);
                    try (FileInputStream fis = new FileInputStream(logFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                    }
                    zos.closeEntry();
                }
            }

            Resource resource = new FileSystemResource(zipPath);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + date + ".zip\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("下载日志文件失败: date={}", date, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
