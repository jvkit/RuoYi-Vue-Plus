package org.dromara.procurement.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.dromara.procurement.domain.PmsFundFlow;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资金流水视图对象 pms_fund_flow
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsFundFlow.class)
public class PmsFundFlowVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "流水编号")
    private String flowNo;

    /**
     * 主键
     */
    private Long id;

    /**
     * 类型（out=流出 in=流入）
     */
    @ExcelProperty(value = "类型")
    private String flowType;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 项目名快照
     */
    @ExcelProperty(value = "项目")
    private String projectName;

    /**
     * 采购申请ID
     */
    private Long requestId;

    /**
     * 申请编号快照
     */
    @ExcelProperty(value = "申请编号")
    private String requestCode;

    /**
     * 申请标题快照
     */
    @ExcelProperty(value = "申请标题")
    private String requestTitle;

    /**
     * 金额（正数）
     */
    @ExcelProperty(value = "金额")
    private BigDecimal amount;

    /**
     * 发生日期
     */
    @ExcelProperty(value = "发生日期")
    private LocalDate occurDate;

    /**
     * 审批人ID
     */
    private Long operatorId;

    /**
     * 审批人姓名
     */
    @ExcelProperty(value = "审批人")
    private String operatorName;

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
    private LocalDateTime updateTime;

}
