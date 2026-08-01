package org.dromara.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.domain.bo.ProcurementRequestBo;
import org.dromara.workflow.domain.vo.ProcurementRequestVo;
import org.dromara.workflow.service.IProcurementRequestService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采购申请
 *
 * @author Lion Li
 */
@ConditionalOnEnable
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/procurement")
public class ProcurementRequestController extends BaseController {

    private final IProcurementRequestService procurementRequestService;

    /**
     * 查询采购申请列表
     */
    @SaCheckPermission("workflow:procurement:list")
    @GetMapping("/list")
    public TableDataInfo<ProcurementRequestVo> list(ProcurementRequestBo bo, PageQuery pageQuery) {
        return procurementRequestService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出采购申请列表
     */
    @SaCheckPermission("workflow:procurement:export")
    @Log(title = "采购申请", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ProcurementRequestBo bo, HttpServletResponse response) {
        List<ProcurementRequestVo> list = procurementRequestService.queryList(bo);
        ExcelUtil.exportExcel(list, "采购申请", ProcurementRequestVo.class, response);
    }

    /**
     * 获取采购申请详细信息
     */
    @SaCheckPermission("workflow:procurement:query")
    @GetMapping("/{id}")
    public R<ProcurementRequestVo> getInfo(@NotNull(message = "主键不能为空")
                                           @PathVariable Long id) {
        return R.ok(procurementRequestService.queryById(id));
    }

    /**
     * 新增采购申请
     */
    @SaCheckPermission("workflow:procurement:add")
    @Log(title = "采购申请", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<ProcurementRequestVo> add(@Validated(AddGroup.class) @RequestBody ProcurementRequestBo bo) {
        return R.ok(procurementRequestService.insertByBo(bo));
    }

    /**
     * 提交采购申请并提交流程
     */
    @SaCheckPermission("workflow:procurement:add")
    @Log(title = "采购申请", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/submitAndFlowStart")
    public R<ProcurementRequestVo> submitAndFlowStart(@Validated(AddGroup.class) @RequestBody ProcurementRequestBo bo) {
        return R.ok(procurementRequestService.submitAndFlowStart(bo));
    }

    /**
     * 修改采购申请
     */
    @SaCheckPermission("workflow:procurement:edit")
    @Log(title = "采购申请", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<ProcurementRequestVo> edit(@Validated(EditGroup.class) @RequestBody ProcurementRequestBo bo) {
        return R.ok(procurementRequestService.updateByBo(bo));
    }

    /**
     * 删除采购申请
     */
    @SaCheckPermission("workflow:procurement:remove")
    @Log(title = "采购申请", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(procurementRequestService.deleteWithValidByIds(List.of(ids)));
    }
}
