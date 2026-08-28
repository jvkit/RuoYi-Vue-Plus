package org.dromara.procurement.domain.vo;

import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.procurement.domain.PmsBomNode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购管理-BOM节点视图对象 pms_bom_node
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsBomNode.class)
public class PmsBomNodeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 节点ID
     */
    @ExcelProperty(value = "节点ID")
    private Long id;

    /**
     * BOM表ID
     */
    @ExcelProperty(value = "BOM表ID")
    private Long bomTableId;

    /**
     * 上级节点ID（0=顶层）
     */
    @ExcelProperty(value = "上级节点ID")
    private Long parentId;

    /**
     * 节点类型（group分组 item物料 product子产品）
     */
    @ExcelProperty(value = "节点类型")
    private String nodeType;

    /**
     * 分组名称（nodeType=group时使用）
     */
    @ExcelProperty(value = "分组名称")
    private String groupName;

    /**
     * 物料库ID（nodeType=item时使用）
     */
    @ExcelProperty(value = "物料库ID")
    private Long catalogId;

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
     * 单件用量
     */
    @ExcelProperty(value = "单件用量")
    private BigDecimal qtyPerUnit;

    /**
     * 参考单价
     */
    @ExcelProperty(value = "参考单价")
    private BigDecimal refPrice;

    /**
     * 库存数量
     */
    @ExcelProperty(value = "库存数量")
    private BigDecimal stockQty;

    /**
     * 引用BOM表ID（nodeType=product时使用）
     */
    @ExcelProperty(value = "引用BOM表ID")
    private Long refBomTableId;

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
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private LocalDateTime updateTime;

}
