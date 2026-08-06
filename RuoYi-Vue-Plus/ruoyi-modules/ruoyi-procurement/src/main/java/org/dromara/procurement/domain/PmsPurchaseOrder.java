package org.dromara.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 采购管理-采购订单对象 pms_purchase_order
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_purchase_order")
public class PmsPurchaseOrder extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单标题
     */
    private String title;

    /**
     * 关联采购申请ID
     */
    private Long requestId;

    /**
     * 项目ID
     */
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
     * 删除标志
     */
    @TableLogic
    private Long delFlag;

}
