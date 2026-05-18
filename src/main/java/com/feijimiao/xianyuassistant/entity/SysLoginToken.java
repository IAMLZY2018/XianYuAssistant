package com.feijimiao.xianyuassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 登录Token实体类
 * @author IAMLZY
 * @date 2026/4/22
 */
@Data
@TableName("sys_login_token")
public class SysLoginToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联用户ID */
    private Long userId;

    /** JWT Token */
    private String token;

    /** 设备标识 */
    private String deviceId;

    /** 登录IP */
    private String loginIp;

    /** 过期时间 */
    private String expireTime;

    /** 创建时间 */
    private String createdTime;

    /** 更新时间 */
    private String updatedTime;
}
