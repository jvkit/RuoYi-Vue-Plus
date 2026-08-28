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
import org.dromara.procurement.domain.bo.PmsBomCatalogBo;
import org.dromara.procurement.domain.vo.PmsBomCatalogVo;
import org.dromara.procurement.service.IPmsBomCatalogService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 采购管理-BOM物料库Controller
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/catalog")
public class PmsBomCatalogController extends BaseController {

    private final IPmsBomCatalogService catalogService;

    /**
     * 查询物料库分页列表
     */
    @SaCheckPermission("procurement:catalog:list")
    @GetMapping("/list")
    public R<PageResult<PmsBomCatalogVo>> list(@Validated(QueryGroup.class) PmsBomCatalogBo bo, PageQuery pageQuery) {
        return R.ok(catalogService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出物料库列表
     */
    @SaCheckPermission("procurement:catalog:export")
    @Log(title = "BOM物料库", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PmsBomCatalogBo bo, HttpServletResponse response) {
        List<PmsBomCatalogVo> list = catalogService.queryList(bo);
        ExcelBuilder.of(list, PmsBomCatalogVo.class).sheetName("BOM物料库").toResponse(response);
    }

    /**
     * 获取物料库详细信息
     */
    @SaCheckPermission("procurement:catalog:query")
    @GetMapping("/{id}")
    public R<PmsBomCatalogVo> getInfo(@NotNull(message = "主键不能为空")
                                      @PathVariable("id") Long id) {
        return R.ok(catalogService.queryById(id));
    }

    /**
     * 新增物料库
     */
    @SaCheckPermission("procurement:catalog:add")
    @Log(title = "BOM物料库", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody PmsBomCatalogBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(catalogService.insertByBo(bo));
    }

    /**
     * 修改物料库
     */
    @SaCheckPermission("procurement:catalog:edit")
    @Log(title = "BOM物料库", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsBomCatalogBo bo) {
        return toAjax(catalogService.updateByBo(bo));
    }

    /**
     * 删除物料库
     */
    @SaCheckPermission("procurement:catalog:remove")
    @Log(title = "BOM物料库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(catalogService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
