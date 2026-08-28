package org.dromara.procurement.domain.vo;

import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.procurement.domain.PmsPurchaseOrder;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购管理-采购订单视图对象 pms_purchase_order
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsPurchaseOrder.class)
public class PmsPurchaseOrderVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    @ExcelProperty(value = "订单ID")
    private Long id;

    /**
     * 订单编号
     */
    @ExcelProperty(value = "订单编号")
    private String orderNo;

    /**
     * 订单标题
     */
    @ExcelProperty(value = "订单标题")
    private String title;

    /**
     * 关联采购申请ID
     */
    @ExcelProperty(value = "申请ID")
    private Long requestId;

    /**
     * 申请编号
     */
    @ExcelProperty(value = "申请编号")
    private String requestCode;

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
     * 订单总金额
     */
    @ExcelProperty(value = "总金额")
    private BigDecimal amount;

    /**
     * 订单状态
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 下单日期
     */
    @ExcelProperty(value = "下单日期")
    private Date orderDate;

    /**
     * 预计到货日期
     */
    @ExcelProperty(value = "预计到货日期")
    private Date deliveryDate;

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
     * 订单明细
     */
    private List<PmsPurchaseOrderItemVo> items;

}
