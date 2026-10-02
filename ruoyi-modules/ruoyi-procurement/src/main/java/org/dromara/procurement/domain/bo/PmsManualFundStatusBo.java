package org.dromara.procurement.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 人工资金流水状态推进业务对象（仅人工备用金流水可操作）
 *
 * @author procurement
 */
@Data
public class PmsManualFundStatusBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 目标资金状态（reimbursed_unpaid 已报销未汇款 / reimbursed_paid 已报销已汇款）
     */
    @NotBlank(message = "目标资金状态不能为空")
    private String fundStatus;

}
