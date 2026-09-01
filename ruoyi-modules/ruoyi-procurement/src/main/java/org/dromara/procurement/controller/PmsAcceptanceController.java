package org.dromara.procurement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.procurement.domain.PmsAcceptance;
import org.dromara.procurement.domain.bo.PmsAcceptanceBo;
import org.dromara.procurement.domain.vo.PmsAcceptanceVo;
import org.dromara.procurement.service.IPmsAcceptanceService;
import org.dromara.procurement.service.PmsAcceptanceInvoiceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 采购管理-采购验收单Controller
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/acceptance")
public class PmsAcceptanceController extends BaseController {

    private final IPmsAcceptanceService acceptanceService;
    private final PmsAcceptanceInvoiceService acceptanceInvoiceService;

    /**
     * 查询采购验收单分页列表
     */
    @SaCheckPermission("procurement:acceptance:list")
    @GetMapping("/list")
    public R<PageResult<PmsAcceptanceVo>> list(@Validated(QueryGroup.class) PmsAcceptanceBo bo, PageQuery pageQuery) {
        return R.ok(acceptanceService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出采购验收单列表
     */
    @SaCheckPermission("procurement:acceptance:export")
    @Log(title = "采购验收单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PmsAcceptanceBo bo, HttpServletResponse response) {
        List<PmsAcceptanceVo> list = acceptanceService.queryList(bo);
        ExcelBuilder.of(list, PmsAcceptanceVo.class).sheetName("采购验收单").toResponse(response);
    }

    /**
     * 获取采购验收单详细信息
     */
    @SaCheckPermission("procurement:acceptance:query")
    @GetMapping("/{id}")
    public R<PmsAcceptanceVo> getInfo(@NotNull(message = "主键不能为空")
                                      @PathVariable("id") Long id) {
        return R.ok(acceptanceService.queryById(id));
    }

    /**
     * 新增采购验收单
     */
    @SaCheckPermission("procurement:acceptance:add")
    @Log(title = "采购验收单", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody PmsAcceptanceBo bo) {
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(acceptanceService.insertByBo(bo));
    }

    /**
     * 修改采购验收单
     */
    @SaCheckPermission("procurement:acceptance:edit")
    @Log(title = "采购验收单", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsAcceptanceBo bo) {
        PmsAcceptance entity = acceptanceService.getById(bo.getId());
        if (entity == null) {
            return R.fail("采购验收单不存在");
        }
        // 未启动流程前可编辑，一旦提交审批不可修改
        if (entity.getProcessInstanceId() != null) {
            return R.fail("已提交审批的验收单不可编辑");
        }
        // 只能编辑自己的单据（管理员除外）
        if (!LoginHelper.isSuperAdmin() && !LoginHelper.getUserId().equals(entity.getCreateBy())) {
            return R.fail("只能编辑自己的采购验收单");
        }
        return toAjax(acceptanceService.updateByBo(bo));
    }

    /**
     * 提交验收并启动流程：验收发起人→采购申请人→项目负责人→团队上级→结束
     */
    @SaCheckPermission("procurement:acceptance:submit")
    @Log(title = "采购验收单", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping("/submit")
    public R<PmsAcceptanceVo> submit(@Validated(AddGroup.class) @RequestBody PmsAcceptanceBo bo) {
        return R.ok(acceptanceService.submitAndStartFlow(bo));
    }

    /**
     * 删除采购验收单
     */
    @SaCheckPermission("procurement:acceptance:remove")
    @Log(title = "采购验收单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        for (Long id : ids) {
            PmsAcceptance entity = acceptanceService.getById(id);
            if (entity == null) {
                continue;
            }
            // 未启动流程前可删除，一旦提交审批不可删除
            if (entity.getProcessInstanceId() != null) {
                return R.fail("已提交审批的验收单不可删除");
            }
            // 只能删除自己的单据（管理员除外）
            if (!LoginHelper.isSuperAdmin() && !LoginHelper.getUserId().equals(entity.getCreateBy())) {
                return R.fail("只能删除自己的采购验收单");
            }
        }
        return toAjax(acceptanceService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

    /**
     * 发票批量识别 + 匹配 + 持久化（调用 agents 智能体服务）。
     *
     * @param acceptanceId 验收单 ID（编辑草稿时传入，新增草稿可为空）
     * @param items        验收明细 JSON 数组（含 itemName/spec/applyPrice/quantity/id）
     * @param files        发票 PDF 文件（可多个）
     * @return 匹配报告（含 ossId / invoiceId / invalidReason）
     */
    @SaCheckPermission("procurement:acceptance:add")
    @PostMapping("/ai-invoice-match")
    public R<JSONObject> aiInvoiceMatch(@RequestParam(value = "acceptanceId", required = false) Long acceptanceId,
                                        @RequestParam("items") String items,
                                        @RequestParam("files") List<MultipartFile> files) {
        List<Object> itemList = JSONUtil.parseArray(items);
        return R.ok(acceptanceInvoiceService.matchAndPersist(acceptanceId, itemList, files));
    }
}
