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
import org.dromara.procurement.domain.bo.PmsProjectBo;
import org.dromara.procurement.domain.vo.PmsProjectVo;
import org.dromara.procurement.service.IPmsProjectService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 采购管理-项目Controller
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/project")
public class PmsProjectController extends BaseController {

    private final IPmsProjectService projectService;

    /**
     * 查询项目分页列表
     */
    @SaCheckPermission("procurement:project:list")
    @GetMapping("/list")
    public R<PageResult<PmsProjectVo>> list(@Validated(QueryGroup.class) PmsProjectBo bo, PageQuery pageQuery) {
        return R.ok(projectService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出项目列表
     */
    @SaCheckPermission("procurement:project:export")
    @Log(title = "采购项目", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PmsProjectBo bo, HttpServletResponse response) {
        List<PmsProjectVo> list = projectService.queryList(bo);
        ExcelBuilder.of(list, PmsProjectVo.class).sheetName("采购项目").toResponse(response);
    }

    /**
     * 获取项目详细信息
     */
    @SaCheckPermission("procurement:project:query")
    @GetMapping("/{id}")
    public R<PmsProjectVo> getInfo(@NotNull(message = "主键不能为空")
                                   @PathVariable("id") Long id) {
        return R.ok(projectService.queryById(id));
    }

    /**
     * 新增项目
     */
    @SaCheckPermission("procurement:project:add")
    @Log(title = "采购项目", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody PmsProjectBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(projectService.insertByBo(bo));
    }

    /**
     * 修改项目
     */
    @SaCheckPermission("procurement:project:edit")
    @Log(title = "采购项目", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsProjectBo bo) {
        return toAjax(projectService.updateByBo(bo));
    }

    /**
     * 删除项目
     */
    @SaCheckPermission("procurement:project:remove")
    @Log(title = "采购项目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(projectService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
