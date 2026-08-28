package org.dromara.procurement.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.procurement.domain.bo.PmsFundFlowBo;
import org.dromara.procurement.domain.vo.PmsFundFlowVo;
import org.dromara.procurement.domain.vo.PmsFundSummaryVo;

import java.util.List;

/**
 * 资金流水Service接口
 *
 * @author procurement
 */
public interface IPmsFundFlowService {

    /**
     * 查询资金流水详情
     */
    PmsFundFlowVo queryById(Long id);

    /**
     * 查询资金流水分页列表
     */
    PageResult<PmsFundFlowVo> queryPageList(PmsFundFlowBo bo, PageQuery pageQuery);

    /**
     * 查询资金流水列表
     */
    List<PmsFundFlowVo> queryList(PmsFundFlowBo bo);

    /**
     * 资金汇总（总预算/已用/剩余/本月流出 + 按项目维度）
     *
     * @param projectId 项目ID（可选，传空=全部）
     */
    PmsFundSummaryVo summary(Long projectId);

    /**
     * 导出资金流水
     */
    List<PmsFundFlowVo> queryExportList(PmsFundFlowBo bo);

    /**
     * 资金同步：根据所有 status='finish' 的采购申请重建项目已用金额和资金流水
     */
    void syncFromRequests();
}
