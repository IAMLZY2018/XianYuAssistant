package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 当前用户信息响应DTO
 * @author IAMLZY
 * @date 2026/4/22
 */
@Data
public class CurrentUserRespDTO {
    private String username;
    private String lastLoginTime;
}
