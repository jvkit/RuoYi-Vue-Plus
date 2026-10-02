package org.dromara.procurement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelBuilder;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.web.core.BaseController;
import org.dromara.procurement.domain.bo.PmsFundFlowBo;
import org.dromara.procurement.domain.bo.PmsFundStatusBo;
import org.dromara.procurement.domain.bo.PmsManualFundFlowBo;
import org.dromara.procurement.domain.bo.PmsManualFundStatusBo;
import org.dromara.procurement.domain.vo.PmsFundFlowVo;
import org.dromara.procurement.domain.vo.PmsFundStatusBoardVo;
import org.dromara.procurement.domain.vo.PmsFundSummaryVo;
import org.dromara.procurement.service.IPmsFundFlowService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 资金流水Controller
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/fund")
public class PmsFundFlowController extends BaseController {

    private final IPmsFundFlowService fundFlowService;

    /**
     * 查询资金流水分页列表
     */
    @SaCheckPermission("procurement:fund:list")
    @GetMapping("/list")
    public R<PageResult<PmsFundFlowVo>> list(PmsFundFlowBo bo, PageQuery pageQuery) {
        return R.ok(fundFlowService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取资金流水详细信息
     */
    @SaCheckPermission("procurement:fund:query")
    @GetMapping("/{id}")
    public R<PmsFundFlowVo> getInfo(@PathVariable Long id) {
        return R.ok(fundFlowService.queryById(id));
    }

    /**
     * 资金汇总（总预算/已用/剩余 + 本月流出 + 按项目维度）
     */
    @SaCheckPermission("procurement:fund:list")
    @GetMapping("/summary")
    public R<PmsFundSummaryVo> summary(Long projectId) {
        return R.ok(fundFlowService.summary(projectId));
    }

    /**
     * 导出资金流水
     */
    @SaCheckPermission("procurement:fund:export")
    @Log(title = "资金流水", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(PmsFundFlowBo bo, HttpServletResponse response) {
        List<PmsFundFlowVo> list = fundFlowService.queryExportList(bo);
        ExcelBuilder.of(list, PmsFundFlowVo.class).sheetName("资金流水").toResponse(response);
    }

    /**
     * 资金同步：根据所有 status='finish' 的采购申请重建项目已用金额和资金流水
     */
    @SaCheckPermission("procurement:fund:edit")
    @Log(title = "资金流水", businessType = BusinessType.UPDATE)
    @PostMapping("/sync")
    public R<Void> sync() {
        fundFlowService.syncFromRequests();
        return R.ok();
    }

    /**
     * 资金状态看板（按资金状态统计笔数与金额，催办用）
     */
    @SaCheckPermission("procurement:fund:list")
    @GetMapping("/status/board")
    public R<List<PmsFundStatusBoardVo>> statusBoard() {
        return R.ok(fundFlowService.statusBoard());
    }

    /**
     * 批量推进资金状态（标记已报销 / 确认已汇款）
     * <p>
     * 单向不可回溯：一旦推进不允许回退，操作人与时间会留痕在申请单上。
     */
    @SaCheckPermission("procurement:fund:status")
    @Log(title = "资金状态", businessType = BusinessType.UPDATE)
    @PutMapping("/status")
    public R<Void> changeStatus(@Validated @RequestBody PmsFundStatusBo bo) {
        return R.ok(fundFlowService.changeFundStatus(bo));
    }

    /**
     * 人工登记资金流水（非采购订单的资金消耗）
     * <p>
     * 自购：按 payers 顺序拆账，每人一条流水（fund_status=已采购未报销）；
     * 对公：一条流水（fund_status=null）。项目 used_amount 按总额一次累加。
     */
    @SaCheckPermission("procurement:fund:manual")
    @Log(title = "资金流水", businessType = BusinessType.INSERT)
    @PostMapping("/manual")
    public R<List<PmsFundFlowVo>> manual(@Validated @RequestBody PmsManualFundFlowBo bo) {
        return R.ok(fundFlowService.createManualFlow(bo));
    }

    /**
     * 人工流水资金状态推进（仅人工登记的备用金流水可操作，单向、幂等）
     */
    @SaCheckPermission("procurement:fund:manual")
    @Log(title = "资金流水", businessType = BusinessType.UPDATE)
    @PutMapping("/manual/{id}/fundStatus")
    public R<Void> changeManualStatus(@PathVariable Long id,
                                      @Validated @RequestBody PmsManualFundStatusBo bo) {
        fundFlowService.changeManualFundStatus(id, bo.getFundStatus());
        return R.ok();
    }
}
