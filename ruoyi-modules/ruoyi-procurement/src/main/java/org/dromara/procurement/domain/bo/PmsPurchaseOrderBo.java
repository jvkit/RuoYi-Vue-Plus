package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsPurchaseOrder;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购管理-采购订单业务对象 pms_purchase_order
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsPurchaseOrder.class, reverseConvertGenerate = false)
public class PmsPurchaseOrderBo extends BaseEntity {

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单标题
     */
    @NotBlank(message = "订单标题不能为空", groups = {AddGroup.class, EditGroup.class})
    private String title;

    /**
     * 关联采购申请ID
     */
    private Long requestId;

    /**
     * 项目ID
     */
    @NotNull(message = "项目不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long projectId;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 订单总金额
     */
    private BigDecimal amount;

    /**
     * 订单状态（draft草稿 ordered已下单 partial_received部分收货 received已收货 cancelled已取消）
     */
    private String status;

    /**
     * 下单日期
     */
    private Date orderDate;

    /**
     * 预计到货日期
     */
    private Date deliveryDate;

    /**
     * 备注
     */
    private String remark;

    /**
     * 订单明细
     */
    @Valid
    private List<PmsPurchaseOrderItemBo> items;

}
