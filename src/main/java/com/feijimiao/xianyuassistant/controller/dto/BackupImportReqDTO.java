package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class BackupImportReqDTO {
    private String jsonData;
    private List<String> modules;
}
