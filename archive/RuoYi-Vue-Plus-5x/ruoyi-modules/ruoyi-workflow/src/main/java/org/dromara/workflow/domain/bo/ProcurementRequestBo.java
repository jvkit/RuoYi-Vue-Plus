package org.dromara.workflow.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.workflow.domain.ProcurementRequest;

import java.math.BigDecimal;

/**
 * 采购申请业务对象 procurement_request
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ProcurementRequest.class, reverseConvertGenerate = false)
public class ProcurementRequestBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 流程code
     */
    private String flowCode;

    /**
     * 申请单号
     */
    private String applyCode;

    /**
     * 采购标题
     */
    @NotBlank(message = "采购标题不能为空", groups = {AddGroup.class, EditGroup.class})
    private String title;

    /**
     * 采购金额
     */
    @NotNull(message = "采购金额不能为空", groups = {AddGroup.class, EditGroup.class})
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
