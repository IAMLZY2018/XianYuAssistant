package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

@Data
public class VersionInfoRespDTO {
    private String currentVersion;
    private String latestVersion;
    private Boolean hasUpdate;
    private String updateContent;
    private String publishedAt;
    private String downloadUrl;
}
