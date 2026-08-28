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
 * 采购管理-采购订单明细对象 pms_purchase_order_item
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_purchase_order_item")
public class PmsPurchaseOrderItem extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 明细ID
     */
    @TableId(value = "id")
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
    private BigDecimal quantity;

    /**
     * 单价
     */
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

    /**
     * 删除标志
     */
    @TableLogic
    private Long delFlag;

}
