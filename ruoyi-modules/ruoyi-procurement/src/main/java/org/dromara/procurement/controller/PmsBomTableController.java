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
import org.dromara.procurement.domain.bo.PmsBomTableBo;
import org.dromara.procurement.domain.vo.PmsBomTableVo;
import org.dromara.procurement.service.IPmsBomTableService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 采购管理-BOM表(产品)Controller
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/bomtable")
public class PmsBomTableController extends BaseController {

    private final IPmsBomTableService bomTableService;

    /**
     * 查询BOM表分页列表
     */
    @SaCheckPermission("procurement:bomtable:list")
    @GetMapping("/list")
    public R<PageResult<PmsBomTableVo>> list(@Validated(QueryGroup.class) PmsBomTableBo bo, PageQuery pageQuery) {
        return R.ok(bomTableService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出BOM表列表
     */
    @SaCheckPermission("procurement:bomtable:export")
    @Log(title = "BOM表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PmsBomTableBo bo, HttpServletResponse response) {
        List<PmsBomTableVo> list = bomTableService.queryList(bo);
        ExcelBuilder.of(list, PmsBomTableVo.class).sheetName("BOM表").toResponse(response);
    }

    /**
     * 获取BOM表详细信息
     */
    @SaCheckPermission("procurement:bomtable:query")
    @GetMapping("/{id}")
    public R<PmsBomTableVo> getInfo(@NotNull(message = "主键不能为空")
                                    @PathVariable("id") Long id) {
        return R.ok(bomTableService.queryById(id));
    }

    /**
     * 新增BOM表
     */
    @SaCheckPermission("procurement:bomtable:add")
    @Log(title = "BOM表", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody PmsBomTableBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(bomTableService.insertByBo(bo));
    }

    /**
     * 修改BOM表
     */
    @SaCheckPermission("procurement:bomtable:edit")
    @Log(title = "BOM表", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsBomTableBo bo) {
        return toAjax(bomTableService.updateByBo(bo));
    }

    /**
     * 删除BOM表
     */
    @SaCheckPermission("procurement:bomtable:remove")
    @Log(title = "BOM表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(bomTableService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
