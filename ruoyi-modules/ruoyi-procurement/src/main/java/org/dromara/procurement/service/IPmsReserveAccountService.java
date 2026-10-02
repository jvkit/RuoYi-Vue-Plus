package org.dromara.procurement.service;

import org.dromara.procurement.domain.bo.PmsReserveAccountBo;
import org.dromara.procurement.domain.vo.PmsReserveAccountVo;
import org.dromara.procurement.domain.vo.PmsReserveOptionVo;
import org.dromara.procurement.domain.vo.PmsReserveSummaryVo;
import org.dromara.procurement.domain.vo.PmsUserOptionVo;

import java.util.List;

/**
 * 备用金额度账户Service接口
 *
 * @author procurement
 */
public interface IPmsReserveAccountService {

    /**
     * 备用金视图列表（额度 + 实时聚合的占用/可用/回笼/未报销笔数）
     */
    List<PmsReserveAccountVo> queryList();

    /**
     * 备用金汇总（全员合计，供资金管理页卡片使用）
     */
    PmsReserveSummaryVo summary();

    /**
     * 新增备用金账户（额度为空时取 sys_config 默认值；同一用户只允许一个账户）
     */
    Boolean insertByBo(PmsReserveAccountBo bo);

    /**
     * 修改备用金额度（只允许改 quota / remark，不允许改归属人）
     */
    Boolean updateByBo(PmsReserveAccountBo bo);

    /**
     * 删除备用金账户
     */
    Boolean deleteById(Long id);

    /**
     * 某人当前可用备用金额度（= 账户额度 - 按人占用聚合）
     * <p>
     * 占用以资金流水 applicant_id 聚合（多人拆账口径）：采购流水看关联申请的资金状态，
     * 人工流水看自身 fund_status；无账户视为 0（账户只能由管理员手动添加，不自动开户）。
     *
     * @param personId 备用金人（出纳人）ID
     * @return 可用额度（无账户返回 0）
     */
    java.math.BigDecimal availableAmount(Long personId);

    /**
     * 备用金平铺选项（登录即可读，采购申请表单备用金卡片用）
     * <p>
     * 与账户列表同口径实时聚合 occupied / available，只暴露表单需要的平铺字段。
     */
    List<PmsReserveOptionVo> queryOptions();

    /**
     * 选人下拉（未删除的正常用户）
     */
    List<PmsUserOptionVo> userOptions();

}
