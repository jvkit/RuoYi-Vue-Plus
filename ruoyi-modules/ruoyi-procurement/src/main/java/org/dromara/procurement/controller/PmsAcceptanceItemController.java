package org.dromara.procurement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.ValidatorUtils;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.core.validate.QueryGroup;
import org.dromara.common.excel.utils.ExcelBuilder;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.web.core.BaseController;
import org.dromara.procurement.domain.bo.PmsAcceptanceItemBo;
import org.dromara.procurement.domain.vo.PmsAcceptanceItemVo;
import org.dromara.procurement.service.IPmsAcceptanceItemService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 采购管理-验收明细Controller
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/acceptance/item")
public class PmsAcceptanceItemController extends BaseController {

    private final IPmsAcceptanceItemService acceptanceItemService;

    /**
     * 查询验收明细分页列表
     */
    @SaCheckPermission("procurement:acceptance:item:list")
    @GetMapping("/list")
    public R<PageResult<PmsAcceptanceItemVo>> list(@Validated(QueryGroup.class) PmsAcceptanceItemBo bo, PageQuery pageQuery) {
        return R.ok(acceptanceItemService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出验收明细列表
     */
    @SaCheckPermission("procurement:acceptance:item:export")
    @Log(title = "验收明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PmsAcceptanceItemBo bo, HttpServletResponse response) {
        List<PmsAcceptanceItemVo> list = acceptanceItemService.queryList(bo);
        ExcelBuilder.of(list, PmsAcceptanceItemVo.class).sheetName("验收明细").toResponse(response);
    }

    /**
     * 获取验收明细详细信息
     */
    @SaCheckPermission("procurement:acceptance:item:query")
    @GetMapping("/{id}")
    public R<PmsAcceptanceItemVo> getInfo(@NotNull(message = "主键不能为空")
                                          @PathVariable("id") Long id) {
        return R.ok(acceptanceItemService.queryById(id));
    }

    /**
     * 新增验收明细
     */
    @SaCheckPermission("procurement:acceptance:item:add")
    @Log(title = "验收明细", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody PmsAcceptanceItemBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(acceptanceItemService.insertByBo(bo));
    }

    /**
     * 修改验收明细
     */
    @SaCheckPermission("procurement:acceptance:item:edit")
    @Log(title = "验收明细", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsAcceptanceItemBo bo) {
        return toAjax(acceptanceItemService.updateByBo(bo));
    }

    /**
     * 删除验收明细
     */
    @SaCheckPermission("procurement:acceptance:item:remove")
    @Log(title = "验收明细", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(acceptanceItemService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
