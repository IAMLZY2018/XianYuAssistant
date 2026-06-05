package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class BackupExportReqDTO {
    private List<String> modules;
}
