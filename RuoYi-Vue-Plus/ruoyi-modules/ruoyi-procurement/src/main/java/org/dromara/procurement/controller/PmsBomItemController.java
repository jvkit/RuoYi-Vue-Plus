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
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.procurement.domain.bo.PmsBomItemBo;
import org.dromara.procurement.domain.vo.PmsBomItemVo;
import org.dromara.procurement.service.IPmsBomItemService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 采购管理-BOM/物料清单Controller
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/bom")
public class PmsBomItemController extends BaseController {

    private final IPmsBomItemService bomItemService;

    /**
     * 查询BOM分页列表
     */
    @SaCheckPermission("procurement:bom:list")
    @GetMapping("/list")
    public TableDataInfo<PmsBomItemVo> list(@Validated(QueryGroup.class) PmsBomItemBo bo, PageQuery pageQuery) {
        return bomItemService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出BOM列表
     */
    @SaCheckPermission("procurement:bom:export")
    @Log(title = "采购BOM", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PmsBomItemBo bo, HttpServletResponse response) {
        List<PmsBomItemVo> list = bomItemService.queryList(bo);
        ExcelUtil.exportExcel(list, "采购BOM", PmsBomItemVo.class, response);
    }

    /**
     * 获取BOM详细信息
     */
    @SaCheckPermission("procurement:bom:query")
    @GetMapping("/{id}")
    public R<PmsBomItemVo> getInfo(@NotNull(message = "主键不能为空")
                                   @PathVariable("id") Long id) {
        return R.ok(bomItemService.queryById(id));
    }

    /**
     * 新增BOM
     */
    @SaCheckPermission("procurement:bom:add")
    @Log(title = "采购BOM", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody PmsBomItemBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(bomItemService.insertByBo(bo));
    }

    /**
     * 修改BOM
     */
    @SaCheckPermission("procurement:bom:edit")
    @Log(title = "采购BOM", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsBomItemBo bo) {
        return toAjax(bomItemService.updateByBo(bo));
    }

    /**
     * 删除BOM
     */
    @SaCheckPermission("procurement:bom:remove")
    @Log(title = "采购BOM", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(bomItemService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
