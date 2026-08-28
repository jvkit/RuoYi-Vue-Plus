package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsWarehouseStock;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 采购管理-仓库库存业务对象 pms_warehouse_stock
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsWarehouseStock.class, reverseConvertGenerate = false)
public class PmsWarehouseStockBo extends BaseEntity {

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 库存ID
     */
    @NotNull(message = "库存ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 物品名称
     */
    @NotBlank(message = "物品名称不能为空", groups = {AddGroup.class, EditGroup.class})
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

}
