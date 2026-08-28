package org.dromara.procurement.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.procurement.domain.PmsBomItem;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 采购管理-BOM/物料清单视图对象 pms_bom_item
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsBomItem.class)
public class PmsBomItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * BOM条目ID
     */
    @ExcelProperty(value = "BOM条目ID")
    private Long id;

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
     * 物料分类
     */
    @ExcelProperty(value = "物料分类")
    private String category;

    /**
     * 品名
     */
    @ExcelProperty(value = "品名")
    private String name;

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
     * 数量
     */
    @ExcelProperty(value = "数量")
    private BigDecimal qty;

    /**
     * 单位
     */
    @ExcelProperty(value = "单位")
    private String unit;

    /**
     * 预估单价
     */
    @ExcelProperty(value = "预估单价")
    private BigDecimal estPrice;

    /**
     * 预估总价
     */
    @ExcelProperty(value = "预估总价")
    private BigDecimal estTotal;

    /**
     * 建议供应商ID
     */
    @ExcelProperty(value = "建议供应商ID")
    private Long supplierId;

    /**
     * 建议供应商名称
     */
    @ExcelProperty(value = "建议供应商")
    private String supplierName;

    /**
     * 状态（0待采购 1已下单 2已到货）
     */
    @ExcelProperty(value = "状态")
    private Integer status;

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
