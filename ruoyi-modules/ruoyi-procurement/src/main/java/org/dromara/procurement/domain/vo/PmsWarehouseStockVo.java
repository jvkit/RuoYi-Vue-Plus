package org.dromara.procurement.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.dromara.procurement.domain.PmsWarehouseStock;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.time.LocalDateTime;

/**
 * 采购管理-仓库库存视图对象 pms_warehouse_stock
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsWarehouseStock.class)
public class PmsWarehouseStockVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 库存ID
     */
    @ExcelProperty(value = "库存ID")
    private Long id;

    /**
     * 物品名称
     */
    @ExcelProperty(value = "物品名称")
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
     * 可用数量
     */
    @ExcelProperty(value = "可用数量")
    private BigDecimal qtyAvailable;

    /**
     * 来源明细ID（采购申请明细）
     */
    @ExcelProperty(value = "来源明细ID")
    private Long sourceItemId;

    /**
     * 项目ID
     */
    @ExcelProperty(value = "项目ID")
    private Long projectId;

    /**
     * 项目名称（回显）
     */
    private String projectName;

    /**
     * 入库日期
     */
    @ExcelProperty(value = "入库日期")
    private Date inboundDate;

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
