package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsIssueRequest;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 采购管理-领用申请业务对象 pms_issue_request
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsIssueRequest.class, reverseConvertGenerate = false)
public class PmsIssueRequestBo extends BaseEntity {

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 领用申请ID
     */
    @NotNull(message = "领用申请ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 领用申请编码
     */
    private String issueCode;

    /**
     * 库存ID
     */
    @NotNull(message = "库存ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long stockId;

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
     * 库存可用数量
     */
    private BigDecimal qtyAvailable;

    /**
     * 申请领用数量
     */
    private BigDecimal qtyRequested;

    /**
     * 领用用途
     */
    private String purpose;

    /**
     * 申请人
     */
    private String applicant;

    /**
     * 审批人
     */
    private String approver;

    /**
     * 状态（pending待审批 approved已通过 rejected已驳回 issued已发放）
     */
    private String status;

    /**
     * 流程实例ID
     */
    private Long processInstanceId;

    /**
     * 审批时间
     */
    private Date approveTime;

    /**
     * 备注
     */
    private String remark;

}
