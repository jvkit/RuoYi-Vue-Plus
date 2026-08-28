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
import org.dromara.procurement.domain.bo.PmsIssueRequestBo;
import org.dromara.procurement.domain.vo.PmsIssueRequestVo;
import org.dromara.procurement.service.IPmsIssueRequestService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 采购管理-领用申请Controller
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/issue")
public class PmsIssueRequestController extends BaseController {

    private final IPmsIssueRequestService issueRequestService;

    /**
     * 查询领用申请分页列表
     */
    @SaCheckPermission("procurement:issue:list")
    @GetMapping("/list")
    public R<PageResult<PmsIssueRequestVo>> list(@Validated(QueryGroup.class) PmsIssueRequestBo bo, PageQuery pageQuery) {
        return R.ok(issueRequestService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出领用申请列表
     */
    @SaCheckPermission("procurement:issue:export")
    @Log(title = "领用申请", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PmsIssueRequestBo bo, HttpServletResponse response) {
        List<PmsIssueRequestVo> list = issueRequestService.queryList(bo);
        ExcelBuilder.of(list, PmsIssueRequestVo.class).sheetName("领用申请").toResponse(response);
    }

    /**
     * 获取领用申请详细信息
     */
    @SaCheckPermission("procurement:issue:query")
    @GetMapping("/{id}")
    public R<PmsIssueRequestVo> getInfo(@NotNull(message = "主键不能为空")
                                        @PathVariable("id") Long id) {
        return R.ok(issueRequestService.queryById(id));
    }

    /**
     * 新增领用申请
     */
    @SaCheckPermission("procurement:issue:add")
    @Log(title = "领用申请", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody PmsIssueRequestBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(issueRequestService.insertByBo(bo));
    }

    /**
     * 修改领用申请
     */
    @SaCheckPermission("procurement:issue:edit")
    @Log(title = "领用申请", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsIssueRequestBo bo) {
        return toAjax(issueRequestService.updateByBo(bo));
    }

    /**
     * 审批领用申请（pending -> approved/rejected）
     */
    @SaCheckPermission("procurement:issue:approve")
    @Log(title = "领用申请审批", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/approve")
    public R<Void> approve(@RequestBody PmsIssueRequestBo bo) {
        return toAjax(issueRequestService.approve(bo.getId(), bo.getStatus()));
    }

    /**
     * 提交领用申请并启动流程：发起人 → 仓库管理员 → 结束
     */
    @SaCheckPermission("procurement:issue:submit")
    @Log(title = "领用申请", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping("/submit")
    public R<PmsIssueRequestVo> submit(@Validated(AddGroup.class) @RequestBody PmsIssueRequestBo bo) {
        return R.ok(issueRequestService.submitAndStartFlow(bo));
    }

    /**
     * 删除领用申请
     */
    @SaCheckPermission("procurement:issue:remove")
    @Log(title = "领用申请", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(issueRequestService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
