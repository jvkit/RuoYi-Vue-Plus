package org.dromara.procurement.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 备用金汇总视图对象（资金管理页「备用金」卡片 + 嵌入 PmsFundSummaryVo.reserve）
 *
 * @author procurement
 */
@Data
public class PmsReserveSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 总额度（所有人额度之和）
     */
    private BigDecimal totalQuota = BigDecimal.ZERO;

    /**
     * 已占用（所有人自购未回笼金额之和）
     */
    private BigDecimal occupied = BigDecimal.ZERO;

    /**
     * 可用（总额度 - 已占用）
     */
    private BigDecimal available = BigDecimal.ZERO;

    /**
     * 已回笼（所有人已报销已汇款金额之和）
     */
    private BigDecimal recycled = BigDecimal.ZERO;

}
