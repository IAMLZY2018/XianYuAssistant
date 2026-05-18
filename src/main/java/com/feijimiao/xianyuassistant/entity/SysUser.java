package com.feijimiao.xianyuassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统用户实体类
 * @author IAMLZY
 * @date 2026/4/22
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码（BCrypt加密） */
    private String password;

    /** 状态 1:正常 0:禁用 */
    private Integer status;

    /** 最后登录时间 */
    private String lastLoginTime;

    /** 最后登录IP */
    private String lastLoginIp;

    /** 创建时间 */
    private String createdTime;

    /** 更新时间 */
    private String updatedTime;
}
