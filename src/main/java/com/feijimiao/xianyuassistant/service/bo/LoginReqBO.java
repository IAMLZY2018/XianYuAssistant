package com.feijimiao.xianyuassistant.service.bo;

import lombok.Data;

/**
 * 登录请求BO
 * @author IAMLZY
 * @date 2026/4/22
 */
@Data
public class LoginReqBO {
    private String username;
    private String password;
    private String ip;
    private String deviceId;
}
