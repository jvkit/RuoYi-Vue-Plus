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
import org.dromara.procurement.domain.bo.PmsPurchaseOrderBo;
import org.dromara.procurement.domain.vo.PmsPurchaseOrderVo;
import org.dromara.procurement.service.IPmsPurchaseOrderService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 采购管理-采购订单Controller
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/order")
public class PmsPurchaseOrderController extends BaseController {

    private final IPmsPurchaseOrderService orderService;

    /**
     * 查询采购订单分页列表
     */
    @SaCheckPermission("procurement:order:list")
    @GetMapping("/list")
    public R<PageResult<PmsPurchaseOrderVo>> list(@Validated(QueryGroup.class) PmsPurchaseOrderBo bo, PageQuery pageQuery) {
        return R.ok(orderService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出采购订单列表
     */
    @SaCheckPermission("procurement:order:export")
    @Log(title = "采购订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PmsPurchaseOrderBo bo, HttpServletResponse response) {
        List<PmsPurchaseOrderVo> list = orderService.queryList(bo);
        ExcelBuilder.of(list, PmsPurchaseOrderVo.class).sheetName("采购订单").toResponse(response);
    }

    /**
     * 获取采购订单详细信息
     */
    @SaCheckPermission("procurement:order:query")
    @GetMapping("/{id}")
    public R<PmsPurchaseOrderVo> getInfo(@NotNull(message = "主键不能为空")
                                         @PathVariable("id") Long id) {
        return R.ok(orderService.queryById(id));
    }

    /**
     * 新增采购订单
     */
    @SaCheckPermission("procurement:order:add")
    @Log(title = "采购订单", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody PmsPurchaseOrderBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(orderService.insertByBo(bo));
    }

    /**
     * 修改采购订单
     */
    @SaCheckPermission("procurement:order:edit")
    @Log(title = "采购订单", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsPurchaseOrderBo bo) {
        return toAjax(orderService.updateByBo(bo));
    }

    /**
     * 删除采购订单
     */
    @SaCheckPermission("procurement:order:remove")
    @Log(title = "采购订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(orderService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

}
