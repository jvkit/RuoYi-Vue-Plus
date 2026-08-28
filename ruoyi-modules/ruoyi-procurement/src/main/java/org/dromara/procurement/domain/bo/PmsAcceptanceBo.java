package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsAcceptance;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购管理-采购验收单业务对象 pms_acceptance
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsAcceptance.class, reverseConvertGenerate = false)
public class PmsAcceptanceBo extends BaseEntity {

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 验收单ID
     */
    @NotNull(message = "验收单ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 验收单编码
     */
    private String acceptanceCode;

    /**
     * 采购申请ID
     */
    private Long requestId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 验收人
     */
    private String operator;

    /**
     * 验收日期
     */
    private Date acceptanceDate;

    /**
     * 状态（pending待验收 partial部分验收 finished完成 rejected驳回）
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
     * 验收明细列表
     */
    private List<PmsAcceptanceItemBo> items;

}
