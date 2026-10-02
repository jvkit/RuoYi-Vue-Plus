package org.dromara.procurement.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.procurement.domain.PmsReserveAccount;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 备用金额度账户视图对象 pms_reserve_account
 * <p>
 * occupied / available / recycled / unreimbursedCount 均为实时聚合值（非持久化字段），
 * 口径（多人拆账）：按资金流水 applicant_id 聚合——采购流水看关联申请的资金状态，
 * 人工流水看自身 fund_status；
 * 占用 = 状态 ∈ (已采购未报销, 已报销未汇款)，回笼 = 已报销已汇款，可用 = 额度 − 占用。
 *
 * @author procurement
 */
@Data
@AutoMapper(target = PmsReserveAccount.class)
public class PmsReserveAccountVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long personId;

    /**
     * 姓名
     */
    private String personName;

    /**
     * 备用金额度（元）
     */
    private BigDecimal quota;

    /**
     * 已占用（自购未回笼金额合计）
     */
    private BigDecimal occupied;

    /**
     * 可用（额度 - 已占用）
     */
    private BigDecimal available;

    /**
     * 已回笼（已报销已汇款金额合计）
     */
    private BigDecimal recycled;

    /**
     * 未报销笔数（已采购未报销 + 已报销未汇款）
     */
    private Long unreimbursedCount;

    /**
     * 备注
     */
    private String remark;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
