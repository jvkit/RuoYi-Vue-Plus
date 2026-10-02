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
     * 口径与 PmsFundFlowMapper#sumOccupiedByPerson 完全一致（多人拆账口径）：
     * 按资金流水 applicant_id（=被扣款的备用金出纳人）聚合，数据源为自购流出流水
     * （title_type='自购'，含历史空值按自购口径回填的单子）；
     * 采购流水看关联申请的资金状态，人工流水（request_id 为空）看自身 fund_status；
     * 占用 = 状态 ∈ (已采购未报销, 已报销未汇款)，已回笼 = 状态 = 已报销已汇款。
     * 对公流水与备用金无关，永不计入。
     *
     * @return 每人的占用/回笼/未报销笔数
     */
    @Select("""
        SELECT f.applicant_id AS personId,
               COALESCE(SUM(CASE WHEN (f.request_id IS NOT NULL AND r.fund_status IN ('purchased_unreimbursed', 'reimbursed_unpaid'))
                                  OR (f.request_id IS NULL AND (f.fund_status IS NULL OR f.fund_status IN ('purchased_unreimbursed', 'reimbursed_unpaid')))
                                 THEN f.amount ELSE 0 END), 0) AS occupied,
               COALESCE(SUM(CASE WHEN (f.request_id IS NOT NULL AND r.fund_status = 'reimbursed_paid')
                                  OR (f.request_id IS NULL AND f.fund_status = 'reimbursed_paid')
                                 THEN f.amount ELSE 0 END), 0) AS recycled,
               COALESCE(SUM(CASE WHEN (f.request_id IS NOT NULL AND r.fund_status IN ('purchased_unreimbursed', 'reimbursed_unpaid'))
                                  OR (f.request_id IS NULL AND (f.fund_status IS NULL OR f.fund_status IN ('purchased_unreimbursed', 'reimbursed_unpaid')))
                                 THEN 1 ELSE 0 END), 0) AS unreimbursedCount
        FROM pms_fund_flow f
        LEFT JOIN pms_procurement_request r ON r.id = f.request_id
        WHERE f.del_flag = '0'
          AND f.flow_type = 'out'
          AND (f.title_type = '自购' OR f.title_type IS NULL OR f.title_type = '')
          AND f.applicant_id IS NOT NULL
        GROUP BY f.applicant_id
        """)
    List<PmsReserveStatVo> selectPersonStats();

}
