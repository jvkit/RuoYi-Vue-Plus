package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsFundFlow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 资金流水业务对象 pms_fund_flow
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsFundFlow.class, reverseConvertGenerate = false)
public class PmsFundFlowBo extends BaseEntity {

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 流水编号
     */
    private String flowNo;

    /**
     * 类型（out=流出 in=流入）
     */
    private String flowType;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 项目名快照
     */
    private String projectName;

    /**
     * 采购申请ID
     */
    private Long requestId;

    /**
     * 申请编号快照
     */
    private String requestCode;

    /**
     * 申请标题快照
     */
    private String requestTitle;

    /**
     * 金额（正数）
     */
    private BigDecimal amount;

    /**
     * 发生日期
     */
    private LocalDate occurDate;

    /**
     * 审批人ID
     */
    private Long operatorId;

    /**
     * 审批人姓名
     */
    private String operatorName;

    /**
     * 备注
     */
    private String remark;

}
