package org.dromara.procurement.domain.vo;

import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.procurement.domain.PmsProcurementRequest;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购管理-采购申请视图对象 pms_procurement_request
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsProcurementRequest.class)
public class PmsProcurementRequestVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 申请ID
     */
    @ExcelProperty(value = "申请ID")
    private Long id;

    /**
     * 申请编号
     */
    @ExcelProperty(value = "申请编号")
    private String requestCode;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 申请标题
     */
    @ExcelProperty(value = "申请标题")
    private String title;

    /**
     * 标题类型（自购/对公）
     */
    @ExcelProperty(value = "标题类型")
    private String titleType;

    /**
     * 标题名称
     */
    @ExcelProperty(value = "标题名称")
    private String titleName;

    /**
     * 项目ID
     */
    @ExcelProperty(value = "项目ID")
    private Long projectId;

    /**
     * 项目名称
     */
    @ExcelProperty(value = "项目名称")
    private String projectName;

    /**
     * 项目负责人（自动带出）
     */
    @ExcelProperty(value = "项目负责人")
    private String leader;

    /**
     * 采购对接人（自动带出）
     */
    @ExcelProperty(value = "采购对接人")
    private String procurementContact;

    /**
     * 当前审批人（流程运行中动态查询，不硬编码）
     */
    private String currentApprover;

    /**
     * 付款截图
     */
    private String paymentScreenshot;

    /**
     * 报价单
     */
    private String quotationUrl;

    /**
     * 开票信息JSON
     */
    private String invoiceInfoJson;

    /**
     * 项目剩余资金（前端提示用）
     */
    private BigDecimal remainingBudget;

    /**
     * 总金额
     */
    @ExcelProperty(value = "总金额")
    private BigDecimal amount;

    /**
     * 采购类型
     */
    @ExcelProperty(value = "采购类型")
    private String purchaseType;

    /**
     * 申请原因
     */
    @ExcelProperty(value = "申请原因")
    private String applyReason;

    /**
     * 状态
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 验收标志（none未验收/processing验收中/done已完成验收）
     */
    @ExcelProperty(value = "验收标志")
    private String acceptanceStatus;

    /**
     * 流程实例ID
     */
    @ExcelProperty(value = "流程实例ID")
    private Long processInstanceId;

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
     * 申请明细
     */
    private List<PmsProcurementRequestItemVo> items;

}
