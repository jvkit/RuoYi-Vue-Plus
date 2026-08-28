package org.dromara.procurement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.procurement.domain.vo.PmsProcurementCategoryNode;
import org.dromara.procurement.service.IPmsProcurementCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 采购管理-采购分类Controller
 * <p>
 * 采购申请明细中的分类树（一级分类 100材料/600其他/700危险化学品/800设备/900费用 → 二级分类）
 *
 * @author procurement
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/category")
public class PmsProcurementCategoryController extends BaseController {

    private final IPmsProcurementCategoryService categoryService;

    /**
     * 查询采购分类树
     */
    @SaCheckPermission("procurement:request:list")
    @GetMapping("/tree")
    public R<List<PmsProcurementCategoryNode>> tree() {
        return R.ok(categoryService.queryCategoryTree());
    }

}
