package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 修改密码请求DTO
 * @author IAMLZY
 * @date 2026/4/22
 */
@Data
public class ChangePasswordReqDTO {
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
}
