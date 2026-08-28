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
import org.dromara.procurement.domain.vo.PmsFundFlowVo;
import org.dromara.procurement.domain.vo.PmsFundSummaryVo;
import org.dromara.procurement.service.IPmsFundFlowService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
}
