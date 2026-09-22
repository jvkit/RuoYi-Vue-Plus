package org.dromara.procurement.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 备用金按人聚合统计（Mapper 聚合查询的承载对象，非持久化）
 * <p>
 * 一条 SQL 出全部人的占用/回笼/笔数，避免逐人查询造成 N+1。
 *
 * @author procurement
 */
@Data
public class PmsReserveStatVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 申请人用户ID（pms_procurement_request.create_by）
     */
    private Long personId;

    /**
     * 已占用金额
     */
    private BigDecimal occupied;

    /**
     * 已回笼金额
     */
    private BigDecimal recycled;

    /**
     * 未报销笔数
     */
    private Long unreimbursedCount;

}
