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

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
            Path logsDir = Paths.get("logs");
            if (!Files.exists(logsDir) || !Files.isDirectory(logsDir)) {
                return ResultObject.success(new ArrayList<>());
            }
            List<String> dates = Files.list(logsDir)
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.matches("\\d{4}-\\d{2}-\\d{2}"))
                    .sorted()
                    .collect(Collectors.toList());
            return ResultObject.success(dates);
        } catch (Exception e) {
            log.error("获取日志日期列表失败", e);
            return ResultObject.failed("获取日志日期列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/log-download")
    public ResponseEntity<Resource> downloadLog(@RequestParam("date") String date) {
        try {
            if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return ResponseEntity.badRequest().build();
            }
            Path logDir = Paths.get("logs", date);
            if (!Files.exists(logDir) || !Files.isDirectory(logDir)) {
                return ResponseEntity.notFound().build();
            }

            Path zipFile = Files.createTempFile("logs-" + date + "-", ".zip");
            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipFile)))) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(logDir)) {
                    for (Path logFile : stream) {
                        if (Files.isRegularFile(logFile)) {
                            zos.putNextEntry(new ZipEntry(logFile.getFileName().toString()));
                            Files.copy(logFile, zos);
                            zos.closeEntry();
                        }
                    }
                }
            }

            byte[] zipBytes = Files.readAllBytes(zipFile);
            Files.deleteIfExists(zipFile);

            Resource resource = new org.springframework.core.io.ByteArrayResource(zipBytes);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"logs-" + date + ".zip\"")
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                    .contentLength(zipBytes.length)
                    .body(resource);
        } catch (Exception e) {
            log.error("打包下载日志失败: date={}", date, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
