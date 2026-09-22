package org.dromara.procurement.service;

import org.dromara.procurement.domain.bo.PmsReserveAccountBo;
import org.dromara.procurement.domain.vo.PmsReserveAccountVo;
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
     * 确保某用户拥有备用金账户（懒创建，供采购申请提交时调用）
     * <p>
     * 已存在则直接返回，不覆盖管理员配置过的额度。
     *
     * @param personId 用户ID
     * @return 账户（新建或已有）
     */
    PmsReserveAccountVo ensureAccount(Long personId);

    /**
     * 选人下拉（未删除的正常用户）
     */
    List<PmsUserOptionVo> userOptions();

}
