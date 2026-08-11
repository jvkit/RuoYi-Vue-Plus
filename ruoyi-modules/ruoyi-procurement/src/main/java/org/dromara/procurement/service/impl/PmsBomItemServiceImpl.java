package org.dromara.procurement.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.procurement.domain.PmsBomItem;
import org.dromara.procurement.domain.bo.PmsBomItemBo;
import org.dromara.procurement.domain.vo.PmsBomItemVo;
import org.dromara.procurement.mapper.PmsBomItemMapper;
import org.dromara.procurement.service.IPmsBomItemService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * 采购管理-BOM/物料清单Service业务层处理
 *
 * @author procurement
 */
@RequiredArgsConstructor
@Service
public class PmsBomItemServiceImpl implements IPmsBomItemService {

    private final PmsBomItemMapper baseMapper;

    @Override
    public PmsBomItemVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public PageResult<PmsBomItemVo> queryPageList(PmsBomItemBo bo, PageQuery pageQuery) {
        Page<PmsBomItemVo> page = pageQuery.build();
        baseMapper.selectVoPageList(page, bo);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public List<PmsBomItemVo> queryList(PmsBomItemBo bo) {
        Page<PmsBomItemVo> page = new Page<>(1, Integer.MAX_VALUE);
        baseMapper.selectVoPageList(page, bo);
        return page.getRecords();
    }

    @Override
    public Boolean insertByBo(PmsBomItemBo bo) {
        PmsBomItem add = MapstructUtils.convert(bo, PmsBomItem.class);
        calcEstTotal(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(PmsBomItemBo bo) {
        PmsBomItem update = MapstructUtils.convert(bo, PmsBomItem.class);
        calcEstTotal(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 自动计算预估总价
     */
    private void calcEstTotal(PmsBomItem entity) {
        BigDecimal qty = entity.getQty();
        BigDecimal price = entity.getEstPrice();
        if (qty != null && price != null) {
            entity.setEstTotal(qty.multiply(price));
        } else if (StringUtils.isNotBlank(entity.getName())) {
            entity.setEstTotal(BigDecimal.ZERO);
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<PmsBomItem> list) {
        return baseMapper.insertBatch(list);
    }
}
