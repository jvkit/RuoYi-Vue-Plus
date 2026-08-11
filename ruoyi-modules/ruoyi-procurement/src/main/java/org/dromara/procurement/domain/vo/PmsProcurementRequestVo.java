package org.dromara.procurement.domain.vo;

import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.procurement.domain.PmsProcurementRequest;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
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
     * 申请标题
     */
    @ExcelProperty(value = "申请标题")
    private String title;

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
     * 供应商ID
     */
    @ExcelProperty(value = "供应商ID")
    private Long supplierId;

    /**
     * 供应商名称
     */
    @ExcelProperty(value = "供应商")
    private String supplierName;

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
    private Date createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

    /**
     * 申请明细
     */
    private List<PmsProcurementRequestItemVo> items;

}
