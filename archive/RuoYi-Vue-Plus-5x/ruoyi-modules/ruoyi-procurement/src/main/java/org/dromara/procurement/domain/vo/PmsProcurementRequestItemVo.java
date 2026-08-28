package org.dromara.procurement.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.procurement.domain.PmsProcurementRequestItem;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 采购管理-采购申请明细视图对象 pms_procurement_request_item
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsProcurementRequestItem.class)
public class PmsProcurementRequestItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 明细ID
     */
    @ExcelProperty(value = "明细ID")
    private Long id;

    /**
     * 申请ID
     */
    @ExcelProperty(value = "申请ID")
    private Long requestId;

    /**
     * 品名
     */
    @ExcelProperty(value = "品名")
    private String itemName;

    /**
     * 规格型号
     */
    @ExcelProperty(value = "规格型号")
    private String spec;

    /**
     * 品牌
     */
    @ExcelProperty(value = "品牌")
    private String brand;

    /**
     * 单位
     */
    @ExcelProperty(value = "单位")
    private String unit;

    /**
     * 数量
     */
    @ExcelProperty(value = "数量")
    private BigDecimal quantity;

    /**
     * 单价
     */
    @ExcelProperty(value = "单价")
    private BigDecimal unitPrice;

    /**
     * 金额
     */
    @ExcelProperty(value = "金额")
    private BigDecimal amount;

    /**
     * 关联BOM条目ID
     */
    @ExcelProperty(value = "BOM条目ID")
    private Long bomItemId;

    /**
     * 排序号
     */
    @ExcelProperty(value = "排序号")
    private Integer sortNo;

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

}
