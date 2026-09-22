package org.dromara.procurement.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.procurement.domain.PmsProcurementRequest;
import org.dromara.procurement.domain.bo.PmsProcurementRequestBo;
import org.dromara.procurement.domain.vo.PmsFundStatusBoardVo;
import org.dromara.procurement.domain.vo.PmsProcurementRequestVo;

import java.util.List;

/**
 * 采购管理-采购申请Mapper接口
 *
 * @author procurement
 */
public interface PmsProcurementRequestMapper extends BaseMapperPlus<PmsProcurementRequest, PmsProcurementRequestVo> {

    /**
     * 查询采购申请分页列表（关联项目、供应商）
     */
    Page<PmsProcurementRequestVo> selectVoPageList(@Param("page") Page<PmsProcurementRequestVo> page, @Param("bo") PmsProcurementRequestBo bo);

    /**
     * 查询可验收的采购申请列表（关联项目）
     */
    List<PmsProcurementRequestVo> selectAcceptableList();

    /**
     * 查询已验收完成的采购申请列表（关联项目 + 申请人昵称，报销下拉用）
     */
    List<PmsProcurementRequestVo> selectReimbursableList();

    /**
     * 资金状态看板：按 fund_status 统计已审批通过申请的笔数与金额合计（催办用）
     * <p>
     * 只统计 status='finish' 的申请；未进入资金状态（fund_status 为空）的不出现在结果里，
     * 由 Service 补齐固定 4 行并填 0。
     */
    @Select("""
        SELECT r.fund_status AS status,
               COUNT(*) AS count,
               COALESCE(SUM(r.amount), 0) AS amount
        FROM pms_procurement_request r
        WHERE r.del_flag = '0'
          AND r.status = 'finish'
          AND r.fund_status IS NOT NULL
          AND r.fund_status <> ''
        GROUP BY r.fund_status
        """)
    List<PmsFundStatusBoardVo> selectFundStatusBoard();

}
