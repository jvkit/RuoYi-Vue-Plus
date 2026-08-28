package org.dromara.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 采购管理-采购验收单对象 pms_acceptance
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_acceptance")
public class PmsAcceptance extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 验收单ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 验收单编码
     */
    private String acceptanceCode;

    /**
     * 采购申请ID
     */
    private Long requestId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 验收人
     */
    private String operator;

    /**
     * 验收日期
     */
    private Date acceptanceDate;

    /**
     * 状态（pending待验收 partial部分验收 finished完成 rejected驳回）
     */
    private String status;

    /**
     * 流程实例ID
     */
    private Long processInstanceId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标志
     */
    @TableLogic
    private Long delFlag;

}
