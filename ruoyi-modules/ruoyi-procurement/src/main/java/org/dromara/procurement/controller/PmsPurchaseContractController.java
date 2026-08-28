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
import org.dromara.procurement.domain.bo.PmsPurchaseContractBo;
import org.dromara.procurement.domain.vo.PmsPurchaseContractVo;
import org.dromara.procurement.service.IPmsPurchaseContractService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 采购管理-采购合同Controller
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/contract")
public class PmsPurchaseContractController extends BaseController {

    private final IPmsPurchaseContractService contractService;

    /**
     * 查询合同分页列表
     */
    @SaCheckPermission("procurement:contract:list")
    @GetMapping("/list")
    public R<PageResult<PmsPurchaseContractVo>> list(@Validated(QueryGroup.class) PmsPurchaseContractBo bo, PageQuery pageQuery) {
        return R.ok(contractService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出合同列表
     */
    @SaCheckPermission("procurement:contract:export")
    @Log(title = "采购合同", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PmsPurchaseContractBo bo, HttpServletResponse response) {
        List<PmsPurchaseContractVo> list = contractService.queryList(bo);
        ExcelBuilder.of(list, PmsPurchaseContractVo.class).sheetName("采购合同").toResponse(response);
    }

    /**
     * 获取合同详细信息
     */
    @SaCheckPermission("procurement:contract:query")
    @GetMapping("/{id}")
    public R<PmsPurchaseContractVo> getInfo(@NotNull(message = "主键不能为空")
                                            @PathVariable("id") Long id) {
        return R.ok(contractService.queryById(id));
    }

    /**
     * 新增合同
     */
    @SaCheckPermission("procurement:contract:add")
    @Log(title = "采购合同", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody PmsPurchaseContractBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(contractService.insertByBo(bo));
    }

    /**
     * 修改合同
     */
    @SaCheckPermission("procurement:contract:edit")
    @Log(title = "采购合同", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsPurchaseContractBo bo) {
        return toAjax(contractService.updateByBo(bo));
    }

    /**
     * 删除合同
     */
    @SaCheckPermission("procurement:contract:remove")
    @Log(title = "采购合同", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(contractService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
