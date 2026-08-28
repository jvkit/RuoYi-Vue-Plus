package org.dromara.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.procurement.domain.PmsAcceptanceItem;
import org.dromara.procurement.domain.bo.PmsAcceptanceItemBo;
import org.dromara.procurement.domain.vo.PmsAcceptanceItemVo;
import org.dromara.procurement.mapper.PmsAcceptanceItemMapper;
import org.dromara.procurement.service.IPmsAcceptanceItemService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 采购管理-验收明细Service业务层处理
 *
 * @author procurement
 */
@RequiredArgsConstructor
@Service
public class PmsAcceptanceItemServiceImpl implements IPmsAcceptanceItemService {

    private final PmsAcceptanceItemMapper baseMapper;

    @Override
    public PmsAcceptanceItemVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public PageResult<PmsAcceptanceItemVo> queryPageList(PmsAcceptanceItemBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsAcceptanceItem> lqw = buildQueryWrapper(bo);
        Page<PmsAcceptanceItemVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    @Override
    public List<PmsAcceptanceItemVo> queryList(PmsAcceptanceItemBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<PmsAcceptanceItem> buildQueryWrapper(PmsAcceptanceItemBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsAcceptanceItem> lqw = Wrappers.lambdaQuery();
        // 按验收单ID过滤
        lqw.eq(bo.getAcceptanceId() != null, PmsAcceptanceItem::getAcceptanceId, bo.getAcceptanceId());
        lqw.like(StringUtils.isNotBlank(bo.getItemName()), PmsAcceptanceItem::getItemName, bo.getItemName());
        lqw.eq(StringUtils.isNotBlank(bo.getResult()), PmsAcceptanceItem::getResult, bo.getResult());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            PmsAcceptanceItem::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        lqw.orderByDesc(PmsAcceptanceItem::getCreateTime);
        return lqw;
    }

    @Override
    public Boolean insertByBo(PmsAcceptanceItemBo bo) {
        calcPriceCheck(bo);
        PmsAcceptanceItem add = MapstructUtils.convert(bo, PmsAcceptanceItem.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(PmsAcceptanceItemBo bo) {
        calcPriceCheck(bo);
        PmsAcceptanceItem update = MapstructUtils.convert(bo, PmsAcceptanceItem.class);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 金额核对：发票金额 ≤ 申请单价 → pass通过；发票金额 > 申请单价 → over冲红
     */
    private void calcPriceCheck(PmsAcceptanceItemBo bo) {
        BigDecimal invoice = bo.getInvoicePrice() == null ? BigDecimal.ZERO : bo.getInvoicePrice();
        BigDecimal apply = bo.getApplyPrice() == null ? BigDecimal.ZERO : bo.getApplyPrice();
        if (invoice.compareTo(apply) > 0) {
            bo.setPriceCheck("over");
            bo.setResult("over");
        } else {
            bo.setPriceCheck("pass");
            bo.setResult("pass");
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<PmsAcceptanceItem> list) {
        return baseMapper.insertBatch(list);
    }
}
