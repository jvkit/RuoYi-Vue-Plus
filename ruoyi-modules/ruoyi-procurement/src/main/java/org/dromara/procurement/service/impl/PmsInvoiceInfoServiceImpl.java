package org.dromara.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.procurement.domain.PmsInvoiceInfo;
import org.dromara.procurement.mapper.PmsInvoiceInfoMapper;
import org.dromara.procurement.service.IPmsInvoiceInfoService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-发票信息Service实现
 *
 * @author procurement
 */
@Service
@RequiredArgsConstructor
public class PmsInvoiceInfoServiceImpl implements IPmsInvoiceInfoService {

    private final PmsInvoiceInfoMapper baseMapper;

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
}
