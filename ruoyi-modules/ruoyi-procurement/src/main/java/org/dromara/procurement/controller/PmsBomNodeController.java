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
import org.dromara.common.excel.utils.ExcelBuilder;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.procurement.domain.bo.PmsBomNodeBo;
import org.dromara.procurement.domain.vo.PmsBomNodeVo;
import org.dromara.procurement.service.IPmsBomNodeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 采购管理-BOM节点Controller
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/bomtable/node")
public class PmsBomNodeController extends BaseController {

    private final IPmsBomNodeService bomNodeService;

    /**
     * 查询BOM节点平铺列表（按 bomTableId 过滤，前端自行组装树）
     */
    @SaCheckPermission("procurement:bomtable:list")
    @GetMapping("/list")
    public R<List<PmsBomNodeVo>> list(PmsBomNodeBo bo) {
        return R.ok(bomNodeService.queryList(bo));
    }

    /**
     * 导出BOM节点列表
     */
    @SaCheckPermission("procurement:bomtable:export")
    @Log(title = "BOM节点", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PmsBomNodeBo bo, HttpServletResponse response) {
        List<PmsBomNodeVo> list = bomNodeService.queryList(bo);
        ExcelBuilder.of(list, PmsBomNodeVo.class).sheetName("BOM节点").toResponse(response);
    }

    /**
     * 获取BOM节点详细信息
     */
    @SaCheckPermission("procurement:bomtable:query")
    @GetMapping("/{id}")
    public R<PmsBomNodeVo> getInfo(@NotNull(message = "主键不能为空")
                                   @PathVariable("id") Long id) {
        return R.ok(bomNodeService.queryById(id));
    }

    /**
     * 新增BOM节点
     */
    @SaCheckPermission("procurement:bomtable:add")
    @Log(title = "BOM节点", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody PmsBomNodeBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(bomNodeService.insertByBo(bo));
    }

    /**
     * 修改BOM节点
     */
    @SaCheckPermission("procurement:bomtable:edit")
    @Log(title = "BOM节点", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsBomNodeBo bo) {
        return toAjax(bomNodeService.updateByBo(bo));
    }

    /**
     * 删除BOM节点
     */
    @SaCheckPermission("procurement:bomtable:remove")
    @Log(title = "BOM节点", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(bomNodeService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
