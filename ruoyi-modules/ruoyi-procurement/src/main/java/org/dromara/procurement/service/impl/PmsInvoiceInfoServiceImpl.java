package org.dromara.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.procurement.domain.PmsAcceptance;
import org.dromara.procurement.domain.PmsAcceptanceItem;
import org.dromara.procurement.domain.PmsInvoiceInfo;
import org.dromara.procurement.domain.PmsProcurementRequest;
import org.dromara.procurement.domain.PmsProject;
import org.dromara.procurement.domain.vo.PmsInvoiceInfoViewVo;
import org.dromara.procurement.mapper.PmsAcceptanceItemMapper;
import org.dromara.procurement.mapper.PmsAcceptanceMapper;
import org.dromara.procurement.mapper.PmsInvoiceInfoMapper;
import org.dromara.procurement.mapper.PmsProcurementRequestMapper;
import org.dromara.procurement.mapper.PmsProjectMapper;
import org.dromara.procurement.service.IPmsInvoiceInfoService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 采购管理-发票信息Service实现
 *
 * @author procurement
 */
@Service
@RequiredArgsConstructor
public class PmsInvoiceInfoServiceImpl implements IPmsInvoiceInfoService {

    private final PmsInvoiceInfoMapper baseMapper;
    private final PmsProjectMapper projectMapper;
    private final PmsProcurementRequestMapper requestMapper;
    private final PmsAcceptanceMapper acceptanceMapper;
    private final PmsAcceptanceItemMapper acceptanceItemMapper;

    @Override
    public PmsInvoiceInfo getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public PmsInvoiceInfo findValidByCodeAndNumber(String invoiceCode, String invoiceNumber) {
        if (StringUtils.isBlank(invoiceCode) && StringUtils.isBlank(invoiceNumber)) {
            return null;
        }
        LambdaQueryWrapper<PmsInvoiceInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PmsInvoiceInfo::getValidFlag, 1)
            .eq(StringUtils.isNotBlank(invoiceCode), PmsInvoiceInfo::getInvoiceCode, invoiceCode)
            .eq(StringUtils.isNotBlank(invoiceNumber), PmsInvoiceInfo::getInvoiceNumber, invoiceNumber)
            .orderByDesc(PmsInvoiceInfo::getCreateTime)
            .last("LIMIT 1");
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 人工改挂发票到指定验收明细（拖拽修正，即时生效）；
     * acceptanceItemId 为 null 表示取消挂载，发票回到未匹配池并记为无效
     */
    @Override
    public void assignItem(Long id, Long acceptanceItemId) {
        PmsInvoiceInfo invoice = baseMapper.selectById(id);
        if (invoice == null) {
            throw new ServiceException("发票记录不存在");
        }
        String matchedName = null;
        if (acceptanceItemId != null) {
            PmsAcceptanceItem item = acceptanceItemMapper.selectById(acceptanceItemId);
            if (item == null) {
                throw new ServiceException("验收明细不存在");
            }
            matchedName = StringUtils.isBlank(item.getSpec())
                ? item.getItemName() : item.getItemName() + "(" + item.getSpec() + ")";
        }
        // 用 UpdateWrapper 显式置 null（updateById 默认忽略 null 字段，清不掉旧值）
        LambdaUpdateWrapper<PmsInvoiceInfo> uw = Wrappers.lambdaUpdate();
        uw.eq(PmsInvoiceInfo::getId, id)
            .set(PmsInvoiceInfo::getAcceptanceItemId, acceptanceItemId)
            .set(PmsInvoiceInfo::getValidFlag, acceptanceItemId != null ? 1 : 0)
            .set(PmsInvoiceInfo::getInvalidReason, acceptanceItemId != null ? null : "未匹配到本订单商品")
            .set(PmsInvoiceInfo::getMatchedItems, matchedName);
        baseMapper.update(null, uw);
    }

    @Override
    public boolean saveOrUpdateInvoice(PmsInvoiceInfo invoice) {
        if (invoice.getId() != null) {
            return baseMapper.updateById(invoice) > 0;
        }
        return baseMapper.insert(invoice) > 0;
    }

    @Override
    public boolean deleteByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public List<PmsInvoiceInfo> listByCondition(PmsInvoiceInfo query) {
        LambdaQueryWrapper<PmsInvoiceInfo> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(query.getAcceptanceId() != null, PmsInvoiceInfo::getAcceptanceId, query.getAcceptanceId())
            .eq(query.getRequestId() != null, PmsInvoiceInfo::getRequestId, query.getRequestId())
            .eq(query.getProjectId() != null, PmsInvoiceInfo::getProjectId, query.getProjectId())
            .eq(query.getValidFlag() != null, PmsInvoiceInfo::getValidFlag, query.getValidFlag())
            .eq(query.getRedFlag() != null, PmsInvoiceInfo::getRedFlag, query.getRedFlag())
            .like(StringUtils.isNotBlank(query.getInvoiceNumber()), PmsInvoiceInfo::getInvoiceNumber, query.getInvoiceNumber())
            .like(StringUtils.isNotBlank(query.getSellerName()), PmsInvoiceInfo::getSellerName, query.getSellerName())
            .orderByDesc(PmsInvoiceInfo::getCreateTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<PmsInvoiceInfoViewVo> listViewByCondition(PmsInvoiceInfo query) {
        List<PmsInvoiceInfo> list = listByCondition(query);
        if (list.isEmpty()) {
            return List.of();
        }
        // 批量查关联名称，避免逐行查询
        Map<Long, String> projectNameMap = nameMap(list.stream()
            .map(PmsInvoiceInfo::getProjectId).filter(java.util.Objects::nonNull).distinct()
            .collect(Collectors.toList()), projectMapper::selectByIds, PmsProject::getId, PmsProject::getProjectName);
        Map<Long, String> requestTitleMap = nameMap(list.stream()
            .map(PmsInvoiceInfo::getRequestId).filter(java.util.Objects::nonNull).distinct()
            .collect(Collectors.toList()), requestMapper::selectByIds, PmsProcurementRequest::getId, PmsProcurementRequest::getTitle);
        Map<Long, String> acceptanceCodeMap = nameMap(list.stream()
            .map(PmsInvoiceInfo::getAcceptanceId).filter(java.util.Objects::nonNull).distinct()
            .collect(Collectors.toList()), acceptanceMapper::selectByIds, PmsAcceptance::getId, PmsAcceptance::getAcceptanceCode);

        return list.stream().map(info -> {
            PmsInvoiceInfoViewVo vo = new PmsInvoiceInfoViewVo();
            org.springframework.beans.BeanUtils.copyProperties(info, vo);
            if (info.getProjectId() != null) {
                vo.setProjectName(projectNameMap.get(info.getProjectId()));
            }
            if (info.getRequestId() != null) {
                vo.setRequestTitle(requestTitleMap.get(info.getRequestId()));
            }
            if (info.getAcceptanceId() != null) {
                vo.setAcceptanceCode(acceptanceCodeMap.get(info.getAcceptanceId()));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private <T> Map<Long, String> nameMap(Collection<Long> ids,
                                          Function<Collection<Long>, List<T>> loader,
                                          Function<T, Long> idFn,
                                          Function<T, String> nameFn) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return loader.apply(ids).stream()
            .filter(e -> idFn.apply(e) != null)
            .collect(Collectors.toMap(idFn, e -> nameFn.apply(e) == null ? "" : nameFn.apply(e), (a, b) -> a));
    }
}
