package org.dromara.procurement.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 流程审批人查询（轻量 Mapper，直接查 flow_task + flow_user + sys_user）
 *
 * @author procurement
 */
@Mapper
public interface PmsFlowApproverMapper {

    /**
     * 查询某个流程实例的当前审批人昵称（waiting 状态任务下的办理人）
     *
     * @param instanceId 流程实例ID
     * @return 审批人昵称列表
     */
    @Select("""
        SELECT DISTINCT u.nick_name
        FROM flow_task t
        JOIN flow_user fu ON fu.associated = t.id AND fu.del_flag = '0'
        JOIN sys_user u ON u.user_id = CAST(fu.processed_by AS UNSIGNED) AND u.del_flag = '0'
        WHERE t.instance_id = #{instanceId}
          AND t.flow_status = 'waiting'
          AND t.del_flag = '0'
          AND fu.type = '1'
        """)
    List<String> selectCurrentApproverNames(@Param("instanceId") Long instanceId);
}
