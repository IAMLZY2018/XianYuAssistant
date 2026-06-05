package com.feijimiao.xianyuassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统配置实体类
 * @author IAMLZY
 * @date 2026/4/22
 */
@Data
@TableName("xianyu_sys_setting")
public class XianyuSysSetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置键 */
    private String settingKey;

    /** 配置值 */
    private String settingValue;

    /** 配置描述 */
    private String settingDesc;

    /** 创建时间 */
    private String createdTime;

    /** 更新时间 */
    private String updatedTime;
}
