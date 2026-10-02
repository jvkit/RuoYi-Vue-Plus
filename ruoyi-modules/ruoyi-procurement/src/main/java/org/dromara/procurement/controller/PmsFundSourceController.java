package org.dromara.procurement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.procurement.domain.vo.PmsFundSourceVo;
import org.dromara.procurement.service.IPmsFundSourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 采购管理-项目归属(资金来源)Controller
 *
 * @author procurement
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/fundSource")
public class PmsFundSourceController extends BaseController {

    private final IPmsFundSourceService fundSourceService;

    /**
     * 查询归属树（项目管理表单的归属选择器数据源）
     */
    @SaCheckPermission("procurement:project:list")
    @GetMapping("/tree")
    public R<List<PmsFundSourceVo>> tree() {
        return R.ok(fundSourceService.queryTree());
    }

}
