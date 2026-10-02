package org.dromara.procurement.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.procurement.domain.PmsFundFlow;
import org.dromara.procurement.domain.vo.PmsFundFlowVo;

import java.math.BigDecimal;

/**
 * 资金流水Mapper接口
 *
 * @author procurement
 */
public interface PmsFundFlowMapper extends BaseMapperPlus<PmsFundFlow, PmsFundFlowVo> {

    /**
     * 某人的备用金占用金额（按流水 applicant_id 聚合，多人流水口径）：
     * 自购（含历史空值按自购口径）流出流水，且
     * 采购流水关联申请的资金状态 ∈ (已采购未报销, 已报销未汇款)，
     * 人工流水（request_id 为空）自身 fund_status ∈ (空/已采购未报销/已报销未汇款) 视为占用，
     * 已报销已汇款（request 或人工流水自身状态）即释放，不再计入。
     *
     * @param personId 备用金人（出纳人）ID
     * @return 占用金额（无数据返回 0）
     */
    @Select("""
        SELECT COALESCE(SUM(f.amount), 0)
        FROM pms_fund_flow f
        LEFT JOIN pms_procurement_request r ON r.id = f.request_id
        WHERE f.del_flag = '0'
          AND f.flow_type = 'out'
          AND (f.title_type = '自购' OR f.title_type IS NULL OR f.title_type = '')
          AND f.applicant_id = #{personId}
          AND (
            (f.request_id IS NOT NULL AND r.fund_status IN ('purchased_unreimbursed', 'reimbursed_unpaid'))
            OR (f.request_id IS NULL AND (f.fund_status IS NULL OR f.fund_status IN ('purchased_unreimbursed', 'reimbursed_unpaid')))
          )
        """)
    BigDecimal sumOccupiedByPerson(@Param("personId") Long personId);

}
