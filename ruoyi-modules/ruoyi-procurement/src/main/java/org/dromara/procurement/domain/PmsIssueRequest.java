package org.dromara.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 采购管理-领用申请对象 pms_issue_request
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_issue_request")
public class PmsIssueRequest extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 领用申请ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 领用申请编码
     */
    private String issueCode;

    /**
     * 库存ID
     */
    private Long stockId;

    /**
     * 物品名称
     */
    private String itemName;

    /**
     * 规格型号
     */
    private String spec;

    /**
     * 库存可用数量
     */
    private BigDecimal qtyAvailable;

    /**
     * 申请领用数量
     */
    private BigDecimal qtyRequested;

    /**
     * 领用用途
     */
    private String purpose;

    /**
     * 申请人
     */
    private String applicant;

    /**
     * 审批人
     */
    private String approver;

    /**
     * 状态（pending待审批 approved已通过 rejected已驳回 issued已发放）
     */
    private String status;

    /**
     * 流程实例ID
     */
    private Long processInstanceId;

    /**
     * 审批时间
     */
    private Date approveTime;

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
