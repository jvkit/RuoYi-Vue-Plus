package org.dromara.invoice.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.invoice.domain.bo.InvoiceUsageRecordBo;
import org.dromara.invoice.domain.vo.InvoiceUsageRecordVo;
import org.dromara.invoice.service.IInvoiceUsageRecordService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 发票使用记录
 *
 * @author Lion Li
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/invoice/usage")
public class InvoiceUsageRecordController extends BaseController {

    private final IInvoiceUsageRecordService invoiceUsageRecordService;

    /**
     * 查询发票使用记录列表
     */
    @SaCheckPermission("invoice:usage:list")
    @GetMapping("/list")
    public TableDataInfo<InvoiceUsageRecordVo> list(InvoiceUsageRecordBo bo, PageQuery pageQuery) {
        return invoiceUsageRecordService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出发票使用记录列表
     */
    @SaCheckPermission("invoice:usage:export")
    @Log(title = "发票使用记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(InvoiceUsageRecordBo bo, HttpServletResponse response) {
        List<InvoiceUsageRecordVo> list = invoiceUsageRecordService.queryList(bo);
        ExcelUtil.exportExcel(list, "发票使用记录", InvoiceUsageRecordVo.class, response);
    }

    /**
     * 获取发票使用记录详细信息
     */
    @SaCheckPermission("invoice:usage:query")
    @GetMapping("/{id}")
    public R<InvoiceUsageRecordVo> getInfo(@NotNull(message = "主键不能为空")
                                            @PathVariable Long id) {
        return R.ok(invoiceUsageRecordService.queryById(id));
    }

    /**
     * 新增发票使用记录
     */
    @SaCheckPermission("invoice:usage:add")
    @Log(title = "发票使用记录", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<InvoiceUsageRecordVo> add(@Validated(AddGroup.class) @RequestBody InvoiceUsageRecordBo bo) {
        return R.ok(invoiceUsageRecordService.insertByBo(bo));
    }

    /**
     * 删除发票使用记录
     */
    @SaCheckPermission("invoice:usage:remove")
    @Log(title = "发票使用记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(invoiceUsageRecordService.deleteWithValidByIds(List.of(ids)));
    }
}
