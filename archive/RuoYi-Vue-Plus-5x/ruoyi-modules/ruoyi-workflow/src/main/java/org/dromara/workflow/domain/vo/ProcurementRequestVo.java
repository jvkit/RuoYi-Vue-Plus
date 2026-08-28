package org.dromara.workflow.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.workflow.domain.ProcurementRequest;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 采购申请视图对象 procurement_request
 *
 * @author Lion Li
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ProcurementRequest.class)
public class ProcurementRequestVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 申请单号
     */
    @ExcelProperty(value = "申请单号")
    private String applyCode;

    /**
     * 采购标题
     */
    @ExcelProperty(value = "采购标题")
    private String title;

    /**
     * 采购金额
     */
    @ExcelProperty(value = "采购金额")
    private BigDecimal amount;

    /**
     * 供应商名称
     */
    @ExcelProperty(value = "供应商名称")
    private String supplierName;

    /**
     * 采购类型（below:1万以下 above:1万以上）
     */
    @ExcelProperty(value = "采购类型")
    private String purchaseType;

    /**
     * 是否固定资产（0否 1是）
     */
    @ExcelProperty(value = "是否固定资产")
    private String isFixedAsset;

    /**
     * 资产使用寿命（年）
     */
    @ExcelProperty(value = "资产使用寿命")
    private Integer assetLifeYear;

    /**
     * 资产价格
     */
    @ExcelProperty(value = "资产价格")
    private BigDecimal assetPrice;

    /**
     * 申请原因
     */
    @ExcelProperty(value = "申请原因")
    private String applyReason;

    /**
     * 状态（draft草稿 waiting审批中 approved已通过 rejected已驳回）
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 流程实例ID
     */
    @ExcelProperty(value = "流程实例ID")
    private String processInstanceId;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

}
