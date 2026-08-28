package org.dromara.procurement.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.dromara.procurement.domain.PmsIssueRequest;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.time.LocalDateTime;

/**
 * 采购管理-领用申请视图对象 pms_issue_request
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsIssueRequest.class)
public class PmsIssueRequestVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 领用申请ID
     */
    @ExcelProperty(value = "领用申请ID")
    private Long id;

    /**
     * 领用申请编码
     */
    @ExcelProperty(value = "领用申请编码")
    private String issueCode;

    /**
     * 库存ID
     */
    @ExcelProperty(value = "库存ID")
    private Long stockId;

    /**
     * 物品名称
     */
    @ExcelProperty(value = "物品名称")
    private String itemName;

    /**
     * 规格型号
     */
    @ExcelProperty(value = "规格型号")
    private String spec;

    /**
     * 库存可用数量
     */
    @ExcelProperty(value = "库存可用数量")
    private BigDecimal qtyAvailable;

    /**
     * 申请领用数量
     */
    @ExcelProperty(value = "申请领用数量")
    private BigDecimal qtyRequested;

    /**
     * 领用用途
     */
    @ExcelProperty(value = "领用用途")
    private String purpose;

    /**
     * 申请人
     */
    @ExcelProperty(value = "申请人")
    private String applicant;

    /**
     * 审批人
     */
    @ExcelProperty(value = "审批人")
    private String approver;

    /**
     * 状态（pending待审批 approved已通过 rejected已驳回 issued已发放）
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 流程实例ID
     */
    private Long processInstanceId;

    /**
     * 当前审批人（流程 waiting 任务动态查询）
     */
    private String currentApprover;

    /**
     * 审批时间
     */
    @ExcelProperty(value = "审批时间")
    private Date approveTime;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private LocalDateTime updateTime;

}
