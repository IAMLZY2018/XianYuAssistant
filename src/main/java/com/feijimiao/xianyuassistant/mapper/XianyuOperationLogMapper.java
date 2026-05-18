package com.feijimiao.xianyuassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.feijimiao.xianyuassistant.entity.XianyuOperationLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作记录 Mapper
 */
@Mapper
public interface XianyuOperationLogMapper extends BaseMapper<XianyuOperationLog> {
    
    /**
     * 分页查询操作记录
     */
    @Select("<script>" +
            "SELECT * FROM xianyu_operation_log " +
            "WHERE xianyu_account_id = #{accountId} " +
            "<if test='operationType != null and operationType != \"\"'>" +
            "  AND operation_type = #{operationType} " +
            "</if>" +
            "<if test='operationModule != null and operationModule != \"\"'>" +
            "  AND operation_module = #{operationModule} " +
            "</if>" +
            "<if test='operationStatus != null'>" +
            "  AND operation_status = #{operationStatus} " +
            "</if>" +
            "ORDER BY create_time DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<XianyuOperationLog> selectByPage(
            @Param("accountId") Long accountId,
            @Param("operationType") String operationType,
            @Param("operationModule") String operationModule,
            @Param("operationStatus") Integer operationStatus,
            @Param("pageSize") Integer pageSize,
            @Param("offset") Integer offset
    );
    
    /**
     * 统计操作记录数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM xianyu_operation_log " +
            "WHERE xianyu_account_id = #{accountId} " +
            "<if test='operationType != null and operationType != \"\"'>" +
            "  AND operation_type = #{operationType} " +
            "</if>" +
            "<if test='operationModule != null and operationModule != \"\"'>" +
            "  AND operation_module = #{operationModule} " +
            "</if>" +
            "<if test='operationStatus != null'>" +
            "  AND operation_status = #{operationStatus} " +
            "</if>" +
            "</script>")
    Integer countByCondition(
            @Param("accountId") Long accountId,
            @Param("operationType") String operationType,
            @Param("operationModule") String operationModule,
            @Param("operationStatus") Integer operationStatus
    );
    
    /**
     * 根据账号ID删除操作记录
     */
    @Delete("DELETE FROM xianyu_operation_log WHERE xianyu_account_id = #{accountId}")
    int deleteByAccountId(@Param("accountId") Long accountId);
}
