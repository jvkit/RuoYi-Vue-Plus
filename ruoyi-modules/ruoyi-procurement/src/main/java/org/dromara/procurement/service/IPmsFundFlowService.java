package org.dromara.procurement.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.procurement.domain.bo.PmsFundFlowBo;
import org.dromara.procurement.domain.bo.PmsFundStatusBo;
import org.dromara.procurement.domain.bo.PmsManualFundFlowBo;
import org.dromara.procurement.domain.vo.PmsFundFlowVo;
import org.dromara.procurement.domain.vo.PmsFundStatusBoardVo;
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

    /**
     * 资金状态看板：固定返回 4 行（已采购未报销/已报销未汇款/已报销已汇款/不适用），无数据填 0
     */
    List<PmsFundStatusBoardVo> statusBoard();

    /**
     * 批量推进采购申请资金状态（单向不可回溯）
     *
     * @param bo ids + action（reimburse=标记已报销 / paid=确认已汇款）
     * @return 处理结果提示，形如「成功 N 笔，跳过 M 笔（原因）」
     */
    String changeFundStatus(PmsFundStatusBo bo);

    /**
     * 人工登记资金流水（非采购订单的资金消耗）
     * <p>
     * 自购：按 payers 顺序拆账，每人一条流水，fund_status=已采购未报销；
     * 对公：一条流水，fund_status=null。项目 used_amount 按总额一次累加并同步父级。
     *
     * @param bo 登记参数（采购方式/项目/金额/备注/备用金出纳人）
     * @return 生成的流水列表
     */
    List<PmsFundFlowVo> createManualFlow(PmsManualFundFlowBo bo);

    /**
     * 人工流水资金状态推进（仅人工流水 request_id 为空 且 title_type=自购 可操作，单向、幂等）
     *
     * @param id         流水ID
     * @param fundStatus 目标状态（reimbursed_unpaid / reimbursed_paid）
     */
    void changeManualFundStatus(Long id, String fundStatus);
}
