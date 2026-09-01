package org.dromara.procurement.domain.vo;

import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.procurement.domain.PmsProcurementRequestItem;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
     * 采购种类（科研类/非科研类）
     */
    private String purchaseType;

    /**
     * 一级分类
     */
    private String category1;

    /**
     * 二级分类
     */
    private String category2;

    /**
     * 项目归属
     */
    private String projectBelong;

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
     * 物料用途
     */
    @ExcelProperty(value = "物料用途")
    private String materialUsage;

    /**
     * 采购原因（明细级，对应导出Excel的"采购理由"列）
     */
    @ExcelProperty(value = "采购原因")
    private String purchaseReason;

    /**
     * 商品链接
     */
    private String link;

    /**
     * 平台
     */
    private String platform;

    /**
     * 供应商ID（明细级）
     */
    private Long supplierId;

    /**
     * 供应商名称
     */
    private String supplierName;

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
