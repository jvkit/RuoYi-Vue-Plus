package org.dromara.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 采购管理-采购申请对象 pms_procurement_request
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_procurement_request")
public class PmsProcurementRequest extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 申请ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 申请编号
     */
    private String requestCode;

    /**
     * 申请标题
     */
    private String title;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 总金额
     */
    private BigDecimal amount;

    /**
     * 采购类型（goods物资 service服务 fixed_asset固定资产）
     */
    private String purchaseType;

    /**
     * 申请原因
     */
    private String applyReason;

    /**
     * 状态（draft草稿 waiting待审核 finish已完成 back已退回 cancel已撤销 invalid已作废 termination已终止）
     */
    private String status;

    /**
     * 流程实例ID
     */
    private Long processInstanceId;

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
