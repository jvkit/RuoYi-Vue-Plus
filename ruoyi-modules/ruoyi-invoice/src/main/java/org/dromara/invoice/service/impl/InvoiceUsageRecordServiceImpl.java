package org.dromara.invoice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.invoice.domain.InvoiceUsageRecord;
import org.dromara.invoice.domain.bo.InvoiceUsageRecordBo;
import org.dromara.invoice.domain.vo.InvoiceUsageRecordVo;
import org.dromara.invoice.mapper.InvoiceUsageRecordMapper;
import org.dromara.invoice.service.IInvoiceUsageRecordService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 发票使用记录Service业务层处理
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class InvoiceUsageRecordServiceImpl implements IInvoiceUsageRecordService {

    private final InvoiceUsageRecordMapper baseMapper;

    /**
     * 查询发票使用记录
     */
    @Override
    public InvoiceUsageRecordVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询发票使用记录列表
     */
    @Override
    public PageResult<InvoiceUsageRecordVo> queryPageList(InvoiceUsageRecordBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<InvoiceUsageRecord> lqw = buildQueryWrapper(bo);
        Page<InvoiceUsageRecordVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    /**
     * 查询发票使用记录列表
     */
    @Override
    public List<InvoiceUsageRecordVo> queryList(InvoiceUsageRecordBo bo) {
        LambdaQueryWrapper<InvoiceUsageRecord> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<InvoiceUsageRecord> buildQueryWrapper(InvoiceUsageRecordBo bo) {
        LambdaQueryWrapper<InvoiceUsageRecord> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getInvoiceId() != null, InvoiceUsageRecord::getInvoiceId, bo.getInvoiceId());
        lqw.eq(StringUtils.isNotBlank(bo.getBizType()), InvoiceUsageRecord::getBizType, bo.getBizType());
        lqw.like(StringUtils.isNotBlank(bo.getBizNo()), InvoiceUsageRecord::getBizNo, bo.getBizNo());
        lqw.eq(bo.getUsedBy() != null, InvoiceUsageRecord::getUsedBy, bo.getUsedBy());
        lqw.orderByDesc(BaseEntity::getCreateTime);
        return lqw;
    }

    /**
     * 新增发票使用记录
     */
    @Override
    public InvoiceUsageRecordVo insertByBo(InvoiceUsageRecordBo bo) {
        bo.setUsedBy(LoginHelper.getUserId());
        bo.setUsedTime(new Date());
        InvoiceUsageRecord add = MapstructUtils.convert(bo, InvoiceUsageRecord.class);
        baseMapper.insert(add);
        bo.setId(add.getId());
        return MapstructUtils.convert(add, InvoiceUsageRecordVo.class);
    }

    /**
     * 批量删除发票使用记录
     */
    @Override
    public Boolean deleteWithValidByIds(List<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }
}
