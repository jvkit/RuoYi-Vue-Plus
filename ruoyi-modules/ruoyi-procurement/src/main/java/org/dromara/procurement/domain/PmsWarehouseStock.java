package org.dromara.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 采购管理-仓库库存对象 pms_warehouse_stock
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_warehouse_stock")
public class PmsWarehouseStock extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 库存ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 物品名称
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
     * 可用数量
     */
    private BigDecimal qtyAvailable;

    /**
     * 来源明细ID（采购申请明细）
     */
    private Long sourceItemId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 入库日期
     */
    private Date inboundDate;

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
