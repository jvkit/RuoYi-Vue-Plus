package org.dromara.procurement.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.procurement.domain.bo.PmsReserveAccountBo;
import org.dromara.procurement.domain.vo.PmsReserveAccountVo;
import org.dromara.procurement.domain.vo.PmsReserveOptionVo;
import org.dromara.procurement.domain.vo.PmsReserveSummaryVo;
import org.dromara.procurement.domain.vo.PmsUserOptionVo;
import org.dromara.procurement.service.IPmsReserveAccountService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 备用金额度账户Controller
 * <p>
 * 权限沿用资金管理：查看用 procurement:fund:list，配置额度用 procurement:fund:quota；
 * /options 与 /userOptions 不加权限（登录即可），供普通用户填采购申请时读取。
 *
 * @author procurement
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/procurement/reserve")
public class PmsReserveAccountController extends BaseController {

    private final IPmsReserveAccountService reserveAccountService;

    /**
     * 备用金视图列表（额度 + 实时聚合的占用/可用/回笼）
     */
    @SaCheckPermission("procurement:fund:list")
    @GetMapping("/list")
    public R<List<PmsReserveAccountVo>> list() {
        return R.ok(reserveAccountService.queryList());
    }

    /**
     * 备用金汇总（全员合计，卡片用）
     */
    @SaCheckPermission("procurement:fund:list")
    @GetMapping("/summary")
    public R<PmsReserveSummaryVo> summary() {
        return R.ok(reserveAccountService.summary());
    }

    /**
     * 备用金平铺选项（登录即可，采购申请表单备用金卡片用）
     * <p>
     * 不加权限注解：普通用户（common_user 角色）填采购申请时需要看到各备用金人的
     * 额度/占用/可用，与拆账口径一致（按流水 applicant_id 实时聚合）。
     */
    @GetMapping("/options")
    public R<List<PmsReserveOptionVo>> options() {
        return R.ok(reserveAccountService.queryOptions());
    }

    /**
     * 选人下拉（新增备用金账户 / 采购申请表单「使用人」下拉共用，登录即可）
     */
    @GetMapping("/userOptions")
    public R<List<PmsUserOptionVo>> userOptions() {
        return R.ok(reserveAccountService.userOptions());
    }

    /**
     * 新增备用金账户
     */
    @SaCheckPermission("procurement:fund:quota")
    @Log(title = "备用金额度", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody PmsReserveAccountBo bo) {
        return toAjax(reserveAccountService.insertByBo(bo));
    }

    /**
     * 修改备用金额度
     */
    @SaCheckPermission("procurement:fund:quota")
    @Log(title = "备用金额度", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsReserveAccountBo bo) {
        return toAjax(reserveAccountService.updateByBo(bo));
    }

    /**
     * 删除备用金账户
     */
    @SaCheckPermission("procurement:fund:quota")
    @Log(title = "备用金额度", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(reserveAccountService.deleteById(id));
    }

}
