package org.dromara.procurement.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.dromara.procurement.domain.PmsAcceptance;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 采购管理-采购验收单视图对象 pms_acceptance
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsAcceptance.class)
public class PmsAcceptanceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 验收单ID
     */
    @ExcelProperty(value = "验收单ID")
    private Long id;

    /**
     * 验收单编码
     */
    @ExcelProperty(value = "验收单编码")
    private String acceptanceCode;

    /**
     * 采购申请ID
     */
    @ExcelProperty(value = "采购申请ID")
    private Long requestId;

    /**
     * 关联采购申请编号（列表/详情带出，供前端展示）
     */
    @ExcelProperty(value = "采购申请编号")
    private String requestCode;

    /**
     * 关联采购申请标题（列表/详情带出，供前端展示；订单标题落地前先用申请标题）
     */
    @ExcelProperty(value = "采购申请标题")
    private String requestTitle;

    /**
     * 项目ID
     */
    @ExcelProperty(value = "项目ID")
    private Long projectId;

    /**
     * 项目名称（列表/详情带出，供前端展示）
     */
    @ExcelProperty(value = "项目名称")
    private String projectName;

    /**
     * 验收人
     */
    @ExcelProperty(value = "验收人")
    private String operator;

    /**
     * 验收日期
     */
    @ExcelProperty(value = "验收日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date acceptanceDate;

    /**
     * 状态（pending待验收 partial部分验收 finished完成 rejected驳回）
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 流程实例ID
     */
    private Long processInstanceId;

    /**
     * 当前审批人（流程运行中动态查询）
     */
    private String currentApprover;

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

    /**
     * 验收明细列表
     */
    private List<PmsAcceptanceItemVo> items;

}
