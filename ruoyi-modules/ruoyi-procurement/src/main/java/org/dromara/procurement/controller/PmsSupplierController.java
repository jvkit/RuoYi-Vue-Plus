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
import org.dromara.procurement.domain.bo.PmsSupplierBo;
import org.dromara.procurement.domain.vo.PmsSupplierVo;
import org.dromara.procurement.service.IPmsSupplierService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 采购管理-供应商Controller
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/supplier")
public class PmsSupplierController extends BaseController {

    private final IPmsSupplierService supplierService;

    /**
     * 查询供应商分页列表
     */
    @SaCheckPermission("procurement:supplier:list")
    @GetMapping("/list")
    public R<PageResult<PmsSupplierVo>> list(@Validated(QueryGroup.class) PmsSupplierBo bo, PageQuery pageQuery) {
        return R.ok(supplierService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出供应商列表
     */
    @SaCheckPermission("procurement:supplier:export")
    @Log(title = "采购供应商", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PmsSupplierBo bo, HttpServletResponse response) {
        List<PmsSupplierVo> list = supplierService.queryList(bo);
        ExcelBuilder.of(list, PmsSupplierVo.class).sheetName("采购供应商").toResponse(response);
    }

    /**
     * 获取供应商详细信息
     */
    @SaCheckPermission("procurement:supplier:query")
    @GetMapping("/{id}")
    public R<PmsSupplierVo> getInfo(@NotNull(message = "主键不能为空")
                                    @PathVariable("id") Long id) {
        return R.ok(supplierService.queryById(id));
    }

    /**
     * 新增供应商
     */
    @SaCheckPermission("procurement:supplier:add")
    @Log(title = "采购供应商", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody PmsSupplierBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(supplierService.insertByBo(bo));
    }

    /**
     * 修改供应商
     */
    @SaCheckPermission("procurement:supplier:edit")
    @Log(title = "采购供应商", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsSupplierBo bo) {
        return toAjax(supplierService.updateByBo(bo));
    }

    /**
     * 删除供应商
     */
    @SaCheckPermission("procurement:supplier:remove")
    @Log(title = "采购供应商", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(supplierService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
