package org.dromara.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 采购申请对象 procurement_request
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("procurement_request")
public class ProcurementRequest extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 申请单号
     */
    private String applyCode;

    /**
     * 采购标题
     */
    private String title;

    /**
     * 采购金额
     */
    private BigDecimal amount;

    /**
     * 供应商名称
     */
    private String supplierName;

    /**
     * 采购类型（below:1万以下 above:1万以上）
     */
    private String purchaseType;

    /**
     * 是否固定资产（0否 1是）
     */
    private String isFixedAsset;

    /**
     * 资产使用寿命（年）
     */
    private Integer assetLifeYear;

    /**
     * 资产价格
     */
    private BigDecimal assetPrice;

    /**
     * 申请原因
     */
    private String applyReason;

    /**
     * 状态（draft草稿 waiting审批中 approved已通过 rejected已驳回）
     */
    private String status;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 备注
     */
    private String remark;

}
