package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsPurchaseOrderItem;

import java.math.BigDecimal;

/**
 * 采购管理-采购订单明细业务对象 pms_purchase_order_item
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsPurchaseOrderItem.class, reverseConvertGenerate = false)
public class PmsPurchaseOrderItemBo extends BaseEntity {

    /**
     * 明细ID
     */
    @NotNull(message = "明细ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 关联采购申请明细ID
     */
    private Long requestItemId;

    /**
     * 品名
     */
    @NotBlank(message = "品名不能为空", groups = {AddGroup.class, EditGroup.class})
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
     * 数量
     */
    @NotNull(message = "数量不能为空", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal quantity;

    /**
     * 单价
     */
    @NotNull(message = "单价不能为空", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal unitPrice;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 排序号
     */
    private Integer sortNo;

    /**
     * 备注
     */
    private String remark;

}
