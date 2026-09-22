package org.dromara.procurement.mapper;

import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.procurement.domain.PmsReserveAccount;
import org.dromara.procurement.domain.vo.PmsReserveAccountVo;
import org.dromara.procurement.domain.vo.PmsReserveStatVo;

import java.util.List;

/**
 * 备用金额度账户Mapper接口
 *
 * @author procurement
 */
public interface PmsReserveAccountMapper extends BaseMapperPlus<PmsReserveAccount, PmsReserveAccountVo> {

    /**
     * 按人聚合备用金占用情况（一条 SQL 出全部人，避免 N+1）
     * <p>
     * 口径（docs/9.20/资金与报销体系设计-v4-确认版.md §2.2）：
     * 只统计自购（title_type='自购'，含历史空值按自购口径回填的单子）、
     * 已审批通过（status='finish'）的申请；对公申请与备用金无关，永不计入。
     * 占用 = fund_status ∈ (已采购未报销, 已报销未汇款)；已回笼 = fund_status = 已报销已汇款。
     *
     * @return 每人的占用/回笼/未报销笔数
     */
    @Select("""
        SELECT r.create_by AS personId,
               COALESCE(SUM(CASE WHEN r.fund_status IN ('purchased_unreimbursed', 'reimbursed_unpaid')
                                 THEN r.amount ELSE 0 END), 0) AS occupied,
               COALESCE(SUM(CASE WHEN r.fund_status = 'reimbursed_paid'
                                 THEN r.amount ELSE 0 END), 0) AS recycled,
               COALESCE(SUM(CASE WHEN r.fund_status IN ('purchased_unreimbursed', 'reimbursed_unpaid')
                                 THEN 1 ELSE 0 END), 0) AS unreimbursedCount
        FROM pms_procurement_request r
        WHERE r.del_flag = '0'
          AND r.status = 'finish'
          AND (r.title_type = '自购' OR r.title_type IS NULL OR r.title_type = '')
          AND r.create_by IS NOT NULL
        GROUP BY r.create_by
        """)
    List<PmsReserveStatVo> selectPersonStats();

}
