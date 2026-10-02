package org.dromara.procurement.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.procurement.domain.PmsReserveAccount;
import org.dromara.procurement.domain.bo.PmsReserveAccountBo;
import org.dromara.procurement.domain.vo.PmsReserveAccountVo;
import org.dromara.procurement.domain.vo.PmsReserveOptionVo;
import org.dromara.procurement.domain.vo.PmsReserveStatVo;
import org.dromara.procurement.domain.vo.PmsReserveSummaryVo;
import org.dromara.procurement.domain.vo.PmsUserOptionVo;
import org.dromara.procurement.mapper.PmsReserveAccountMapper;
import org.dromara.procurement.service.IPmsReserveAccountService;
import org.dromara.system.domain.SysUser;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 备用金额度账户Service业务层处理
 * <p>
 * 口径（docs/9.20/资金与报销体系设计-v4-确认版.md §2.2）：
 * 本表只存「额度」；占用/可用/回笼一律实时聚合采购申请得出，不落库。
 * 备用金侧任何操作都不影响项目账本（项目已用金额只由采购申请审批通过累加）。
 *
 * @author procurement
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PmsReserveAccountServiceImpl implements IPmsReserveAccountService {

    /**
     * 默认额度参数键（sys_config），取不到时用兜底值
     */
    private static final String DEFAULT_QUOTA_KEY = "procurement.reserve.default_quota";

    /**
     * 兜底默认额度（元）
     */
    private static final BigDecimal FALLBACK_QUOTA = new BigDecimal("10000");

    private final PmsReserveAccountMapper baseMapper;
    private final SysUserMapper userMapper;
    private final ISysConfigService configService;
    private final org.dromara.procurement.mapper.PmsFundFlowMapper fundFlowMapper;

    @Override
    public List<PmsReserveAccountVo> queryList() {
        List<PmsReserveAccountVo> accounts = baseMapper.selectVoList(
            Wrappers.<PmsReserveAccount>lambdaQuery().orderByAsc(PmsReserveAccount::getPersonId));
        fillStats(accounts);
        return accounts;
    }

    @Override
    public PmsReserveSummaryVo summary() {
        List<PmsReserveAccountVo> accounts = queryList();
        PmsReserveSummaryVo summary = new PmsReserveSummaryVo();
        BigDecimal totalQuota = BigDecimal.ZERO;
        BigDecimal occupied = BigDecimal.ZERO;
        BigDecimal recycled = BigDecimal.ZERO;
        for (PmsReserveAccountVo vo : accounts) {
            totalQuota = totalQuota.add(nvl(vo.getQuota()));
            occupied = occupied.add(nvl(vo.getOccupied()));
            recycled = recycled.add(nvl(vo.getRecycled()));
        }
        summary.setTotalQuota(totalQuota);
        summary.setOccupied(occupied);
        summary.setRecycled(recycled);
        summary.setAvailable(totalQuota.subtract(occupied));
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(PmsReserveAccountBo bo) {
        if (ObjectUtil.isNull(bo.getPersonId())) {
            throw new ServiceException("请选择人员");
        }
        PmsReserveAccount exist = selectByPersonId(bo.getPersonId());
        if (ObjectUtil.isNotNull(exist)) {
            throw new ServiceException("该用户已有备用金账户，请直接修改额度");
        }
        PmsReserveAccount add = MapstructUtils.convert(bo, PmsReserveAccount.class);
        if (ObjectUtil.isNull(add)) {
            throw new ServiceException("参数转换失败");
        }
        add.setId(null);
        if (StringUtils.isBlank(add.getPersonName())) {
            add.setPersonName(selectNickName(bo.getPersonId()));
        }
        if (ObjectUtil.isNull(add.getQuota())) {
            add.setQuota(defaultQuota());
        }
        return baseMapper.insert(add) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(PmsReserveAccountBo bo) {
        if (ObjectUtil.isNull(bo.getId())) {
            throw new ServiceException("账户ID不能为空");
        }
        PmsReserveAccount exist = baseMapper.selectById(bo.getId());
        if (ObjectUtil.isNull(exist)) {
            throw new ServiceException("备用金账户不存在");
        }
        // 只允许改额度与备注：归属人一旦确定不允许改，避免占用聚合口径错乱
        PmsReserveAccount update = new PmsReserveAccount();
        update.setId(exist.getId());
        update.setQuota(bo.getQuota());
        update.setRemark(bo.getRemark());
        return baseMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    public List<PmsReserveOptionVo> queryOptions() {
        return queryList().stream().map(a -> {
            PmsReserveOptionVo vo = new PmsReserveOptionVo();
            vo.setPersonId(a.getPersonId());
            vo.setPersonName(a.getPersonName());
            vo.setQuota(a.getQuota());
            vo.setOccupied(a.getOccupied());
            vo.setAvailable(a.getAvailable());
            return vo;
        }).toList();
    }

    /**
     * 可用额度 = 账户额度 − 流水占用；无账户视为 0（账户只能由管理员手动添加，不自动建档）
     */
    @Override
    public java.math.BigDecimal availableAmount(Long personId) {
        if (personId == null) {
            return BigDecimal.ZERO;
        }
        PmsReserveAccount account = selectByPersonId(personId);
        if (account == null || account.getQuota() == null) {
            return BigDecimal.ZERO;
        }
        return account.getQuota().subtract(nvl(fundFlowMapper.sumOccupiedByPerson(personId)));
    }

    @Override
    public List<PmsUserOptionVo> userOptions() {
        List<SysUser> users = userMapper.selectList(
            Wrappers.<SysUser>lambdaQuery()
                .select(SysUser::getUserId, SysUser::getNickName)
                .eq(SysUser::getStatus, "0")
                .orderByAsc(SysUser::getUserId));
        List<PmsUserOptionVo> options = new ArrayList<>(users.size());
        for (SysUser u : users) {
            PmsUserOptionVo vo = new PmsUserOptionVo();
            vo.setUserId(u.getUserId());
            vo.setNickName(u.getNickName());
            options.add(vo);
        }
        return options;
    }

    /**
     * 把按人聚合的占用/回笼/笔数填充到账户视图，并算出可用额度
     */
    private void fillStats(List<PmsReserveAccountVo> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return;
        }
        Map<Long, PmsReserveStatVo> statMap = baseMapper.selectPersonStats().stream()
            .filter(s -> s.getPersonId() != null)
            .collect(Collectors.toMap(PmsReserveStatVo::getPersonId, Function.identity(), (a, b) -> a));
        for (PmsReserveAccountVo vo : accounts) {
            PmsReserveStatVo stat = statMap.get(vo.getPersonId());
            BigDecimal quota = nvl(vo.getQuota());
            BigDecimal occupied = stat == null ? BigDecimal.ZERO : nvl(stat.getOccupied());
            vo.setOccupied(occupied);
            vo.setRecycled(stat == null ? BigDecimal.ZERO : nvl(stat.getRecycled()));
            vo.setUnreimbursedCount(stat == null || stat.getUnreimbursedCount() == null ? 0L : stat.getUnreimbursedCount());
            vo.setAvailable(quota.subtract(occupied));
        }
    }

    private PmsReserveAccount selectByPersonId(Long personId) {
        return baseMapper.selectOne(Wrappers.<PmsReserveAccount>lambdaQuery()
            .eq(PmsReserveAccount::getPersonId, personId)
            .last("limit 1"));
    }

    private String selectNickName(Long userId) {
        SysUser user = userMapper.selectById(userId);
        return user == null ? null : user.getNickName();
    }

    /**
     * 默认额度：优先取 sys_config，取不到或非法时用兜底值
     */
    private BigDecimal defaultQuota() {
        try {
            String value = configService.selectConfigByKey(DEFAULT_QUOTA_KEY);
            if (StringUtils.isNotBlank(value)) {
                return new BigDecimal(value.trim());
            }
        } catch (Exception e) {
            log.warn("读取备用金默认额度配置失败，使用兜底值 {}：{}", FALLBACK_QUOTA, e.getMessage());
        }
        return FALLBACK_QUOTA;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

}
