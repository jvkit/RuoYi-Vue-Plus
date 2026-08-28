package org.dromara.procurement.domain.vo;

import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.procurement.domain.PmsBomCatalog;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购管理-BOM物料库视图对象 pms_bom_catalog
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsBomCatalog.class)
public class PmsBomCatalogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 物料库ID
     */
    @ExcelProperty(value = "物料库ID")
    private Long id;

    /**
     * 物料名称
     */
    @ExcelProperty(value = "物料名称")
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
     * 参考单价
     */
    @ExcelProperty(value = "参考单价")
    private BigDecimal refPrice;

    /**
     * 物料分类
     */
    @ExcelProperty(value = "物料分类")
    private String category;

    /**
     * 关联链接
     */
    @ExcelProperty(value = "关联链接")
    private String link;

    /**
     * 项目ID
     */
    @ExcelProperty(value = "项目ID")
    private Long projectId;

    /**
     * 库存ID
     */
    @ExcelProperty(value = "库存ID")
    private Long stockId;

    /**
     * 状态（0停用 1正常）
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
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private LocalDateTime updateTime;

}
