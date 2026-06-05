package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 注册请求DTO
 * @author IAMLZY
 * @date 2026/4/22
 */
@Data
public class RegisterReqDTO {
    private String username;
    private String password;
    private String confirmPassword;
}
