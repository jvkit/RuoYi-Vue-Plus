package org.dromara.invoice.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.invoice.domain.InvoiceInfo;
import org.dromara.invoice.domain.bo.InvoiceInfoBo;
import org.dromara.invoice.domain.vo.InvoiceInfoVo;
import org.dromara.invoice.service.DifyInvoiceReviewService;
import org.dromara.invoice.service.IInvoiceInfoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发票信息
 *
 * @author Lion Li
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/invoice/info")
public class InvoiceInfoController extends BaseController {

    private final IInvoiceInfoService invoiceInfoService;
    private final DifyInvoiceReviewService difyInvoiceReviewService;

    /**
     * 查询发票信息列表
     */
    @SaCheckPermission("invoice:info:list")
    @GetMapping("/list")
    public TableDataInfo<InvoiceInfoVo> list(InvoiceInfoBo bo, PageQuery pageQuery) {
        return invoiceInfoService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出发票信息列表
     */
    @SaCheckPermission("invoice:info:export")
    @Log(title = "发票信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(InvoiceInfoBo bo, HttpServletResponse response) {
        List<InvoiceInfoVo> list = invoiceInfoService.queryList(bo);
        ExcelUtil.exportExcel(list, "发票信息", InvoiceInfoVo.class, response);
    }

    /**
     * 获取发票信息详细信息
     */
    @SaCheckPermission("invoice:info:query")
    @GetMapping("/{id}")
    public R<InvoiceInfoVo> getInfo(@NotNull(message = "主键不能为空")
                                    @PathVariable Long id) {
        return R.ok(invoiceInfoService.queryById(id));
    }

    /**
     * 新增发票信息
     */
    @SaCheckPermission("invoice:info:add")
    @Log(title = "发票信息", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<InvoiceInfoVo> add(@Validated(AddGroup.class) @RequestBody InvoiceInfoBo bo) {
        return R.ok(invoiceInfoService.insertByBo(bo));
    }

    /**
     * 修改发票信息
     */
    @SaCheckPermission("invoice:info:edit")
    @Log(title = "发票信息", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<InvoiceInfoVo> edit(@Validated(EditGroup.class) @RequestBody InvoiceInfoBo bo) {
        return R.ok(invoiceInfoService.updateByBo(bo));
    }

    /**
     * 删除发票信息
     */
    @SaCheckPermission("invoice:info:remove")
    @Log(title = "发票信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(invoiceInfoService.deleteWithValidByIds(List.of(ids)));
    }

    /**
     * AI审核发票（提交或重新提交）
     * 支持上传发票图片进行智能识别和审核
     */
    @PostMapping("/ai-review/{id}")
    public R<Map<String, Object>> aiReview(@NotNull(message = "主键不能为空")
                                            @PathVariable Long id,
                                            @RequestParam(required = false) MultipartFile imageFile) {
        InvoiceInfoVo invoice = invoiceInfoService.queryById(id);
        if (invoice == null) {
            return R.fail("发票不存在");
        }

        // 如果有上传图片，转为base64
        String imageBase64 = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                byte[] bytes = imageFile.getBytes();
                imageBase64 = Base64.getEncoder().encodeToString(bytes);
            } catch (Exception e) {
                log.error("Failed to read invoice image", e);
                return R.fail("图片读取失败");
            }
        }

        // 调用Dify进行审核
        DifyInvoiceReviewService.ReviewResult result = difyInvoiceReviewService.reviewInvoice(
                invoice.getInvoiceCode(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceType(),
                invoice.getAmount() != null ? invoice.getAmount().toString() : null,
                invoice.getTaxAmount() != null ? invoice.getTaxAmount().toString() : null,
                invoice.getTotalAmount() != null ? invoice.getTotalAmount().toString() : null,
                invoice.getInvoiceDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(invoice.getInvoiceDate()) : null,
                invoice.getSellerName(),
                invoice.getBuyerName(),
                imageBase64
        );

        // 更新发票状态和AI意见
        InvoiceInfoBo updateBo = new InvoiceInfoBo();
        updateBo.setId(id);
        updateBo.setAiOpinion(result.getOpinion());

        if (result.isPassed()) {
            updateBo.setStatus("submitted"); // 通过则标记为已提交
        } else {
            updateBo.setStatus("rejected"); // 不通过则驳回
        }

        invoiceInfoService.updateByBo(updateBo);

        Map<String, Object> data = new HashMap<>();
        data.put("passed", result.isPassed());
        data.put("opinion", result.getOpinion());
        return R.ok(data);
    }

    /**
     * 上传发票图片，AI识别字段并给出审核意见
     */
    @PostMapping("/extract")
    public R<DifyInvoiceReviewService.ExtractResult> extractInvoice(@RequestParam("imageFile") MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return R.fail("请上传发票图片");
        }
        DifyInvoiceReviewService.ExtractResult result = difyInvoiceReviewService.extractAndReviewFromImage(imageFile);
        if (!result.isSuccess()) {
            return R.fail(result.getErrorMsg());
        }
        return R.ok(result);
    }

    /**
     * 获取发票AI审核结果
     */
    @GetMapping("/ai-review/{id}")
    public R<Map<String, Object>> getAiReview(@NotNull(message = "主键不能为空")
                                               @PathVariable Long id) {
        InvoiceInfoVo invoice = invoiceInfoService.queryById(id);
        if (invoice == null) {
            return R.fail("发票不存在");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("aiOpinion", invoice.getAiOpinion());
        data.put("status", invoice.getStatus());
        return R.ok(data);
    }
}
