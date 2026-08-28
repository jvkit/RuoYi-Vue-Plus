package org.dromara.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.procurement.domain.PmsBomCatalog;
import org.dromara.procurement.domain.bo.PmsBomCatalogBo;
import org.dromara.procurement.domain.vo.PmsBomCatalogVo;
import org.dromara.procurement.mapper.PmsBomCatalogMapper;
import org.dromara.procurement.service.IPmsBomCatalogService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-BOM物料库Service业务层处理
 *
 * @author procurement
 */
@RequiredArgsConstructor
@Service
public class PmsBomCatalogServiceImpl implements IPmsBomCatalogService {

    private final PmsBomCatalogMapper baseMapper;

    @Override
    public PmsBomCatalogVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public PageResult<PmsBomCatalogVo> queryPageList(PmsBomCatalogBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsBomCatalog> lqw = buildQueryWrapper(bo);
        Page<PmsBomCatalogVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    @Override
    public List<PmsBomCatalogVo> queryList(PmsBomCatalogBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<PmsBomCatalog> buildQueryWrapper(PmsBomCatalogBo bo) {
        LambdaQueryWrapper<PmsBomCatalog> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getItemName()), PmsBomCatalog::getItemName, bo.getItemName());
        lqw.eq(StringUtils.isNotBlank(bo.getCategory()), PmsBomCatalog::getCategory, bo.getCategory());
        lqw.eq(bo.getProjectId() != null, PmsBomCatalog::getProjectId, bo.getProjectId());
        lqw.eq(bo.getStockId() != null, PmsBomCatalog::getStockId, bo.getStockId());
        lqw.eq(bo.getStatus() != null, PmsBomCatalog::getStatus, bo.getStatus());
        lqw.orderByAsc(PmsBomCatalog::getItemName);
        return lqw;
    }

    @Override
    public Boolean insertByBo(PmsBomCatalogBo bo) {
        PmsBomCatalog add = MapstructUtils.convert(bo, PmsBomCatalog.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(PmsBomCatalogBo bo) {
        PmsBomCatalog update = MapstructUtils.convert(bo, PmsBomCatalog.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<PmsBomCatalog> list) {
        return baseMapper.insertBatch(list);
    }
}
