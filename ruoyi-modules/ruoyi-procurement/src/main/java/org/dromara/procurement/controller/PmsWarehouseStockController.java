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
import org.dromara.procurement.domain.bo.PmsWarehouseStockBo;
import org.dromara.procurement.domain.vo.PmsWarehouseStockVo;
import org.dromara.procurement.service.IPmsWarehouseStockService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 采购管理-仓库库存Controller
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/warehouse")
public class PmsWarehouseStockController extends BaseController {

    private final IPmsWarehouseStockService warehouseStockService;

    /**
     * 查询仓库库存分页列表
     */
    @SaCheckPermission("procurement:warehouse:list")
    @GetMapping("/list")
    public R<PageResult<PmsWarehouseStockVo>> list(@Validated(QueryGroup.class) PmsWarehouseStockBo bo, PageQuery pageQuery) {
        return R.ok(warehouseStockService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出仓库库存列表
     */
    @SaCheckPermission("procurement:warehouse:export")
    @Log(title = "仓库库存", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PmsWarehouseStockBo bo, HttpServletResponse response) {
        List<PmsWarehouseStockVo> list = warehouseStockService.queryList(bo);
        ExcelBuilder.of(list, PmsWarehouseStockVo.class).sheetName("仓库库存").toResponse(response);
    }

    /**
     * 获取仓库库存详细信息
     */
    @SaCheckPermission("procurement:warehouse:query")
    @GetMapping("/{id}")
    public R<PmsWarehouseStockVo> getInfo(@NotNull(message = "主键不能为空")
                                          @PathVariable("id") Long id) {
        return R.ok(warehouseStockService.queryById(id));
    }

    /**
     * 新增仓库库存
     */
    @SaCheckPermission("procurement:warehouse:add")
    @Log(title = "仓库库存", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody PmsWarehouseStockBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(warehouseStockService.insertByBo(bo));
    }

    /**
     * 修改仓库库存
     */
    @SaCheckPermission("procurement:warehouse:edit")
    @Log(title = "仓库库存", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsWarehouseStockBo bo) {
        return toAjax(warehouseStockService.updateByBo(bo));
    }

    /**
     * 删除仓库库存
     */
    @SaCheckPermission("procurement:warehouse:remove")
    @Log(title = "仓库库存", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(warehouseStockService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
