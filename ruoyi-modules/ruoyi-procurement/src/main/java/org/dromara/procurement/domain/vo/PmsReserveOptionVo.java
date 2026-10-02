package org.dromara.procurement.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 备用金平铺选项（登录即可读，采购申请表单备用金卡片/使用人下拉用）
 * <p>
 * occupied / available 与账户列表同口径：按资金流水 applicant_id 实时聚合。
 *
 * @author procurement
 */
@Data
public class PmsReserveOptionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 备用金人用户ID
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

}
