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
import org.dromara.procurement.domain.PmsProcurementRequest;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购管理-采购申请业务对象 pms_procurement_request
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsProcurementRequest.class, reverseConvertGenerate = false)
public class PmsProcurementRequestBo extends BaseEntity {

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 申请ID
     */
    @NotNull(message = "申请ID不能为空", groups = {EditGroup.class})
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
    @NotNull(message = "项目不能为空", groups = {AddGroup.class, EditGroup.class})
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
     * 状态
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
     * 申请明细
     */
    @Valid
    private List<PmsProcurementRequestItemBo> items;

}
