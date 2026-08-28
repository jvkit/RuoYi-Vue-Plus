package org.dromara.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 出入库流水对象 pms_stock_movement
 *
 * @author procurement
 */
@Data
@TableName("pms_stock_movement")
public class PmsStockMovement implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流水ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 库存ID
     */
    private Long stockId;

    /**
     * 类型（in入库/out出库）
     */
    private String movementType;

    /**
     * 数量
     */
    private BigDecimal qty;

    /**
     * 来源单据ID
     */
    private Long relateId;

    /**
     * 来源类型（acceptance验收入库/issue领用出库/manual手动）
     */
    private String relateType;

    /**
     * 操作人
     */
    private Long operator;

    /**
     * 操作时间
     */
    private Date operateTime;

    /**
     * 备注
     */
    private String remark;

}
