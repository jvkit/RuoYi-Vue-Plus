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
import org.dromara.procurement.domain.bo.PmsReimbursementBo;
import org.dromara.procurement.domain.vo.PmsReimbursementVo;
import org.dromara.procurement.service.IPmsReimbursementService;
import org.dromara.procurement.service.impl.PmsReimbursementPackService;
import org.dromara.system.service.ISysOssService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 采购管理-报销Controller
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/reimbursement")
public class PmsReimbursementController extends BaseController {

    private final IPmsReimbursementService reimbursementService;
    private final PmsReimbursementPackService packService;
    private final ISysOssService sysOssService;

    /**
     * 查询报销分页列表
     */
    @SaCheckPermission("procurement:reimbursement:list")
    @GetMapping("/list")
    public R<PageResult<PmsReimbursementVo>> list(@Validated(QueryGroup.class) PmsReimbursementBo bo, PageQuery pageQuery) {
        return R.ok(reimbursementService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出报销列表
     */
    @SaCheckPermission("procurement:reimbursement:export")
    @Log(title = "报销单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PmsReimbursementBo bo, HttpServletResponse response) {
        List<PmsReimbursementVo> list = reimbursementService.queryList(bo);
        ExcelBuilder.of(list, PmsReimbursementVo.class).sheetName("报销单").toResponse(response);
    }

    /**
     * 获取报销详细信息
     */
    @SaCheckPermission("procurement:reimbursement:query")
    @GetMapping("/{id}")
    public R<PmsReimbursementVo> getInfo(@NotNull(message = "主键不能为空")
                                         @PathVariable("id") Long id) {
        return R.ok(reimbursementService.queryById(id));
    }

    /**
     * 新增报销
     */
    @SaCheckPermission("procurement:reimbursement:add")
    @Log(title = "报销单", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Long> add(@RequestBody PmsReimbursementBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        reimbursementService.insertByBo(bo);
        return R.ok(bo.getId());
    }

    /**
     * 生成报销包（Excel + 验收图片 + 发票pdf 打包 ZIP 上传，回写 file_url）
     */
    @SaCheckPermission("procurement:reimbursement:edit")
    @Log(title = "报销单", businessType = BusinessType.UPDATE)
    @RepeatSubmit(interval = 5, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping("/generate/{id}")
    public R<Void> generate(@NotNull(message = "主键不能为空") @PathVariable("id") Long id) {
        packService.pack(id);
        return R.ok("报销包已生成");
    }

    /**
     * 下载报销包 ZIP（按打包时上传的 MinIO 文件流式回传）
     */
    @SaCheckPermission("procurement:reimbursement:export")
    @Log(title = "报销单", businessType = BusinessType.EXPORT)
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@NotNull(message = "主键不能为空") @PathVariable("id") Long id) {
        PmsReimbursementVo vo = reimbursementService.queryById(id);
        if (vo == null || org.dromara.common.core.utils.StringUtils.isBlank(vo.getFileUrl())) {
            throw new org.dromara.common.core.exception.ServiceException("该报销记录尚未生成报销包");
        }
        Long ossId = packService.resolveOssIdFromUrl(vo.getFileUrl());
        ResponseEntity<byte[]> resp = sysOssService.download(ossId);
        String zipName = URLEncoder.encode(
            (vo.getReimbursementCode() == null ? "报销包" : vo.getReimbursementCode()) + ".zip",
            StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.status(resp.getStatusCode())
            .header("Content-Disposition", "attachment; filename*=utf-8''" + zipName)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resp.getBody());
    }

    /**
     * 修改报销
     */
    @SaCheckPermission("procurement:reimbursement:edit")
    @Log(title = "报销单", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsReimbursementBo bo) {
        return toAjax(reimbursementService.updateByBo(bo));
    }

    /**
     * 删除报销
     */
    @SaCheckPermission("procurement:reimbursement:remove")
    @Log(title = "报销单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(reimbursementService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
