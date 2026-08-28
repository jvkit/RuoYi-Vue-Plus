package org.dromara.procurement.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 资金汇总视图对象（面板顶部卡片 + 按项目维度）
 *
 * @author procurement
 */
@Data
public class PmsFundSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总预算（所有项目预算之和）
     */
    private BigDecimal totalBudget;

    /**
     * 已用金额（所有项目已用之和）
     */
    private BigDecimal totalUsed;

    /**
     * 剩余金额（总预算 - 已用）
     */
    private BigDecimal totalRemaining;

    /**
     * 本月流出
     */
    private BigDecimal monthOut;

    /**
     * 本月流出笔数
     */
    private Long monthOutCount;

    /**
     * 按项目维度汇总
     */
    private List<PmsFundProjectSummaryVo> projects = new ArrayList<>();

    /**
     * 按项目汇总
     */
    @Data
    public static class PmsFundProjectSummaryVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 项目ID
         */
        private Long projectId;

        /**
         * 项目名称
         */
        private String projectName;

        /**
         * 预算
         */
        private BigDecimal budget;

        /**
         * 已用
         */
        private BigDecimal used;

        /**
         * 剩余
         */
        private BigDecimal remaining;

        /**
         * 本月流出
         */
        private BigDecimal monthOut;

        /**
         * 本月流出笔数
         */
        private Long monthOutCount;
    }
}
