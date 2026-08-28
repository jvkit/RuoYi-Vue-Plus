package org.dromara.procurement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.QueryGroup;
import org.dromara.common.excel.utils.ExcelBuilder;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.web.core.BaseController;
import org.dromara.procurement.domain.bo.PmsOperationLogBo;
import org.dromara.procurement.domain.vo.PmsOperationLogVo;
import org.dromara.procurement.service.IPmsOperationLogService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采购管理-流转记录Controller（只读：仅提供分页列表/查询，无新增修改删除接口）
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/log")
public class PmsOperationLogController extends BaseController {

    private final IPmsOperationLogService operationLogService;

    /**
     * 查询流转记录分页列表
     */
    @SaCheckPermission("procurement:log:list")
    @GetMapping("/list")
    public R<PageResult<PmsOperationLogVo>> list(@Validated(QueryGroup.class) PmsOperationLogBo bo, PageQuery pageQuery) {
        return R.ok(operationLogService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出流转记录列表
     */
    @SaCheckPermission("procurement:log:export")
    @Log(title = "流转记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated PmsOperationLogBo bo, HttpServletResponse response) {
        List<PmsOperationLogVo> list = operationLogService.queryList(bo);
        ExcelBuilder.of(list, PmsOperationLogVo.class).sheetName("流转记录").toResponse(response);
    }

    /**
     * 获取流转记录详细信息
     */
    @SaCheckPermission("procurement:log:query")
    @GetMapping("/{id}")
    public R<PmsOperationLogVo> getInfo(@NotNull(message = "主键不能为空")
                                        @PathVariable("id") Long id) {
        return R.ok(operationLogService.queryById(id));
    }
}
