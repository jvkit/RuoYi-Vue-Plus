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
import org.dromara.procurement.domain.bo.PmsAttachmentBo;
import org.dromara.procurement.domain.vo.PmsAttachmentVo;
import org.dromara.procurement.service.IPmsAttachmentService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 采购管理-通用附件Controller
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/attachment")
public class PmsAttachmentController extends BaseController {

    private final IPmsAttachmentService attachmentService;

    /**
     * 查询附件分页列表
     */
    @SaCheckPermission("procurement:attachment:list")
    @GetMapping("/list")
    public R<PageResult<PmsAttachmentVo>> list(@Validated(QueryGroup.class) PmsAttachmentBo bo, PageQuery pageQuery) {
        return R.ok(attachmentService.queryPageList(bo, pageQuery));
    }

    /**
     * 按业务类型+业务ID查询附件列表（不要求分页，用于业务详情页附件展示）
     */
    @SaCheckPermission("procurement:attachment:list")
    @GetMapping("/listByBiz")
    public R<List<PmsAttachmentVo>> listByBiz(@RequestParam("bizType") String bizType,
                                              @RequestParam("bizId") Long bizId) {
        return R.ok(attachmentService.listByBiz(bizType, bizId));
    }

    /**
     * 导出附件列表
     */
    @SaCheckPermission("procurement:attachment:export")
    @Log(title = "采购附件", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PmsAttachmentBo bo, HttpServletResponse response) {
        List<PmsAttachmentVo> list = attachmentService.queryList(bo);
        ExcelBuilder.of(list, PmsAttachmentVo.class).sheetName("采购附件").toResponse(response);
    }

    /**
     * 获取附件详细信息
     */
    @SaCheckPermission("procurement:attachment:query")
    @GetMapping("/{id}")
    public R<PmsAttachmentVo> getInfo(@NotNull(message = "主键不能为空")
                                      @PathVariable("id") Long id) {
        return R.ok(attachmentService.queryById(id));
    }

    /**
     * 新增附件
     */
    @SaCheckPermission("procurement:attachment:add")
    @Log(title = "采购附件", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody PmsAttachmentBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(attachmentService.insertByBo(bo));
    }

    /**
     * 修改附件
     */
    @SaCheckPermission("procurement:attachment:edit")
    @Log(title = "采购附件", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsAttachmentBo bo) {
        return toAjax(attachmentService.updateByBo(bo));
    }

    /**
     * 删除附件
     */
    @SaCheckPermission("procurement:attachment:remove")
    @Log(title = "采购附件", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(attachmentService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
