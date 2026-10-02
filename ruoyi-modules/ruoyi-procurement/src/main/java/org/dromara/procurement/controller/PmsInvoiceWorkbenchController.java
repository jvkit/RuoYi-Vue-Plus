package org.dromara.procurement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.web.core.BaseController;
import org.dromara.procurement.domain.bo.PmsInvoiceWorkbenchBo;
import org.dromara.procurement.domain.vo.PmsInvoiceWorkbenchVo;
import org.dromara.procurement.service.IPmsInvoiceWorkbenchService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 发票上传工作台Controller
 *
 * <p>行 = 已验收的采购申请，聚合验收明细覆盖度与发票台账统计，
 * 供财务/采购专员逐单跟进发票上传进度并标记完成。</p>
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/invoiceWorkbench")
public class PmsInvoiceWorkbenchController extends BaseController {

    private final IPmsInvoiceWorkbenchService workbenchService;

    /**
     * 工作台分页列表（数据源：验收单 status=finish 的申请）
     */
    @SaCheckPermission("procurement:workbench:list")
    @GetMapping("/list")
    public R<PageResult<PmsInvoiceWorkbenchVo>> list(PmsInvoiceWorkbenchBo bo, PageQuery pageQuery) {
        return R.ok(workbenchService.queryPageList(bo, pageQuery));
    }

    /**
     * 标记/取消标记发票上传完成（幂等）
     */
    @SaCheckPermission("procurement:workbench:edit")
    @Log(title = "发票上传工作台", businessType = BusinessType.UPDATE)
    @PutMapping("/doneFlag")
    public R<Void> changeDoneFlag(@Validated @RequestBody PmsInvoiceWorkbenchBo bo) {
        workbenchService.changeDoneFlag(bo);
        return R.ok();
    }

}
