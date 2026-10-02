package org.dromara.procurement.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 人工资金流水登记业务对象
 * <p>
 * 非采购订单的资金消耗（设计 docs/9.20/设计探讨-备用金与报销流程.md §2）：
 * 自购 = 扣备用金，payers 必填（有序，顺序即扣款顺序，靠后者兜尾差）；
 * 对公 = 直支项目资金，一条流水，无备用金人。
 *
 * @author procurement
 */
@Data
public class PmsManualFundFlowBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 采购方式（自购/对公）
     */
    @NotBlank(message = "采购方式不能为空")
    private String titleType;

    /**
     * 项目ID
     */
    @NotNull(message = "项目不能为空")
    private Long projectId;

    /**
     * 金额（正数）
     */
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    private BigDecimal amount;

    /**
     * 备注（默认「非采购订单资金消耗」）
     */
    private String remark;

    /**
     * 备用金出纳人（仅自购需要，有序）
     */
    @Valid
    private List<Payer> payers;

    /**
     * 备用金出纳人
     */
    @Data
    public static class Payer implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 人员ID
         */
        @NotNull(message = "备用金出纳人不能有空人员")
        private Long personId;

        /**
         * 人员姓名（为空时按 personId 取昵称快照）
         */
        private String personName;
    }

}
