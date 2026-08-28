package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsBomNode;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 采购管理-BOM节点业务对象 pms_bom_node
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsBomNode.class, reverseConvertGenerate = false)
public class PmsBomNodeBo extends BaseEntity {

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 节点ID
     */
    @NotNull(message = "节点ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * BOM表ID
     */
    @NotNull(message = "BOM表不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long bomTableId;

    /**
     * 上级节点ID（0=顶层）
     */
    private Long parentId;

    /**
     * 节点类型（group分组 item物料 product子产品）
     */
    @NotBlank(message = "节点类型不能为空", groups = {AddGroup.class, EditGroup.class})
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

}
