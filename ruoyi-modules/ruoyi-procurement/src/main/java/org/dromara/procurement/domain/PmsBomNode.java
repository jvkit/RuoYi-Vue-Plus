package org.dromara.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 采购管理-BOM节点对象 pms_bom_node
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_bom_node")
public class PmsBomNode extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 节点ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * BOM表ID
     */
    private Long bomTableId;

    /**
     * 上级节点ID（0=顶层）
     */
    private Long parentId;

    /**
     * 节点类型（group分组 item物料 product子产品）
     */
    private String nodeType;

    /**
     * 分组名称（nodeType=group时使用）
     */
    private String groupName;

    /**
     * 物料库ID（nodeType=item时使用）
     */
    private Long catalogId;

    /**
     * 物料名称
     */
    private String itemName;

    /**
     * 规格型号
     */
    private String spec;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 单位
     */
    private String unit;

    /**
     * 单件用量
     */
    private BigDecimal qtyPerUnit;

    /**
     * 参考单价
     */
    private BigDecimal refPrice;

    /**
     * 库存数量
     */
    private BigDecimal stockQty;

    /**
     * 引用BOM表ID（nodeType=product时使用）
     */
    private Long refBomTableId;

    /**
     * 排序号
     */
    private Integer sortNo;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标志
     */
    @TableLogic
    private Long delFlag;

}
