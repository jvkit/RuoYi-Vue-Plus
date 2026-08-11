package org.dromara.procurement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.ValidatorUtils;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.core.validate.QueryGroup;
import org.dromara.common.excel.utils.ExcelBuilder;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.web.core.BaseController;
import org.dromara.procurement.domain.bo.PmsProcurementRequestBo;
import org.dromara.procurement.domain.vo.PmsProcurementRequestVo;
import org.dromara.procurement.service.IPmsProcurementRequestService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 采购管理-采购申请Controller
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/request")
public class PmsProcurementRequestController extends BaseController {

    private final IPmsProcurementRequestService requestService;

    /**
     * 查询采购申请分页列表
     */
    @SaCheckPermission("procurement:request:list")
    @GetMapping("/list")
    public R<PageResult<PmsProcurementRequestVo>> list(@Validated(QueryGroup.class) PmsProcurementRequestBo bo, PageQuery pageQuery) {
        return R.ok(requestService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出采购申请列表
     */
    @SaCheckPermission("procurement:request:export")
    @Log(title = "采购申请", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PmsProcurementRequestBo bo, HttpServletResponse response) {
        List<PmsProcurementRequestVo> list = requestService.queryList(bo);
        ExcelBuilder.of(list, PmsProcurementRequestVo.class).sheetName("采购申请").toResponse(response);
    }

    /**
     * 导出采购申请表 Excel（按模板填充）
     */
    @SaCheckPermission("procurement:request:export")
    @Log(title = "采购申请", businessType = BusinessType.EXPORT)
    @PostMapping("/exportForm/{id}")
    public void exportForm(@NotNull(message = "主键不能为空")
                           @PathVariable("id") Long id, HttpServletResponse response) {
        requestService.exportFormExcel(id, response);
    }

    /**
     * 获取采购申请详细信息
     */
    @SaCheckPermission("procurement:request:query")
    @GetMapping("/{id}")
    public R<PmsProcurementRequestVo> getInfo(@NotNull(message = "主键不能为空")
                                              @PathVariable("id") Long id) {
        return R.ok(requestService.queryById(id));
    }

    /**
     * 新增采购申请
     */
    @SaCheckPermission("procurement:request:add")
    @Log(title = "采购申请", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody PmsProcurementRequestBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(requestService.insertByBo(bo));
    }

    /**
     * 修改采购申请
     */
    @SaCheckPermission("procurement:request:edit")
    @Log(title = "采购申请", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsProcurementRequestBo bo) {
        return toAjax(requestService.updateByBo(bo));
    }

    /**
     * 删除采购申请
     */
    @SaCheckPermission("procurement:request:remove")
    @Log(title = "采购申请", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(requestService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

    /**
     * 提交采购申请并启动流程
     */
    @SaCheckPermission("procurement:request:submit")
    @Log(title = "采购申请", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping("/submit")
    public R<PmsProcurementRequestVo> submit(@Validated(AddGroup.class) @RequestBody PmsProcurementRequestBo bo) {
        return R.ok(requestService.submitAndStartFlow(bo));
    }

    /**
     * 查询已审批通过的采购申请
     */
    @SaCheckPermission("procurement:request:list")
    @GetMapping("/approvedList")
    public R<List<PmsProcurementRequestVo>> approvedList() {
        return R.ok(requestService.queryApprovedList());
    }

}
