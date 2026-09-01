package org.dromara.procurement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.web.core.BaseController;
import org.dromara.procurement.domain.PmsInvoiceInfo;
import org.dromara.procurement.service.IPmsInvoiceInfoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采购管理-发票台账Controller
 *
 * @author procurement
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/invoice")
public class PmsInvoiceInfoController extends BaseController {

    private final IPmsInvoiceInfoService invoiceInfoService;

    /**
     * 查询采购发票台账列表
     */
    @SaCheckPermission("procurement:invoice:list")
    @GetMapping("/list")
    public R<PageResult<PmsInvoiceInfo>> list(PmsInvoiceInfo query, PageQuery pageQuery) {
        List<PmsInvoiceInfo> list = invoiceInfoService.listByCondition(query);
        // 简单分页：先全查再手动分页（数据量不大时可用）
        int total = list.size();
        int from = (pageQuery.getPageNum() - 1) * pageQuery.getPageSize();
        int to = Math.min(from + pageQuery.getPageSize(), total);
        List<PmsInvoiceInfo> rows = from < total ? list.subList(from, to) : List.of();
        return R.ok(new PageResult<>(rows, (long) total));
    }

    /**
     * 获取发票详情
     */
    @SaCheckPermission("procurement:invoice:query")
    @GetMapping("/{id}")
    public R<PmsInvoiceInfo> getInfo(@PathVariable Long id) {
        return R.ok(invoiceInfoService.getById(id));
    }

    /**
     * 删除发票台账记录（仅管理员/有权限者）
     */
    @SaCheckPermission("procurement:invoice:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(invoiceInfoService.deleteByIds(List.of(ids)));
    }
}
