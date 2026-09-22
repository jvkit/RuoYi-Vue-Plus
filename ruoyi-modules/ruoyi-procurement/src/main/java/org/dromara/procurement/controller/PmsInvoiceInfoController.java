package org.dromara.procurement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.web.core.BaseController;
import org.dromara.procurement.domain.PmsInvoiceInfo;
import org.dromara.procurement.domain.vo.PmsInvoiceInfoViewVo;
import org.dromara.procurement.service.IPmsInvoiceInfoService;
import org.dromara.procurement.service.PmsAcceptanceInvoiceService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final PmsAcceptanceInvoiceService acceptanceInvoiceService;

    /**
     * 手动上传发票（不走 AI）：把 PDF 挂到某条验收明细并写入台账。
     * 用于发票台账页「上传发票」弹窗的逐明细手动挂载。
     */
    @SaCheckPermission("procurement:invoice:upload")
    @Log(title = "采购发票台账", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 5000)
    @PostMapping("/manual-upload")
    public R<cn.hutool.json.JSONObject> manualUpload(@RequestParam(required = false) Long acceptanceId,
                                                     @RequestParam(required = false) Long requestId,
                                                     @RequestParam(required = false) Long acceptanceItemId,
                                                     @RequestParam("files") List<MultipartFile> files) {
        return R.ok(acceptanceInvoiceService.manualUpload(acceptanceId, requestId, acceptanceItemId, files));
    }

    /**
     * 查询采购发票台账列表（补充项目名/申请标题/验收单号）
     */
    @SaCheckPermission("procurement:invoice:list")
    @GetMapping("/list")
    public R<PageResult<PmsInvoiceInfoViewVo>> list(PmsInvoiceInfo query, PageQuery pageQuery) {
        List<PmsInvoiceInfoViewVo> list = invoiceInfoService.listViewByCondition(query);
        // 简单分页：先全查再手动分页（数据量不大时可用）
        int total = list.size();
        int from = (pageQuery.getPageNum() - 1) * pageQuery.getPageSize();
        int to = Math.min(from + pageQuery.getPageSize(), total);
        List<PmsInvoiceInfoViewVo> rows = from < total ? list.subList(from, to) : List.of();
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
