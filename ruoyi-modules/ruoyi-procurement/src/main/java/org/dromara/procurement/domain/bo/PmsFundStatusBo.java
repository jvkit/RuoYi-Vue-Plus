package org.dromara.procurement.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 资金状态变更业务对象
 * <p>
 * 状态机单向不可回溯（业务要求），因此本对象只接受「向前推进」的 action：
 * reimburse = 标记已报销，paid = 确认已汇款（可从已采购未报销直接跳级）。
 *
 * @author procurement
 */
@Data
public class PmsFundStatusBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 采购申请ID集合（支持批量）
     */
    @NotEmpty(message = "请选择采购申请")
    private List<Long> ids;

    /**
     * 动作：reimburse=标记已报销，paid=确认已汇款
     */
    @NotBlank(message = "操作类型不能为空")
    private String action;

}
