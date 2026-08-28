package org.dromara.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.procurement.domain.PmsBomTable;
import org.dromara.procurement.domain.bo.PmsBomTableBo;
import org.dromara.procurement.domain.vo.PmsBomTableVo;
import org.dromara.procurement.mapper.PmsBomTableMapper;
import org.dromara.procurement.service.IPmsBomTableService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-BOM表(产品)Service业务层处理
 *
 * @author procurement
 */
@RequiredArgsConstructor
@Service
public class PmsBomTableServiceImpl implements IPmsBomTableService {

    private final PmsBomTableMapper baseMapper;

    @Override
    public PmsBomTableVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public PageResult<PmsBomTableVo> queryPageList(PmsBomTableBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsBomTable> lqw = buildQueryWrapper(bo);
        Page<PmsBomTableVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    @Override
    public List<PmsBomTableVo> queryList(PmsBomTableBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<PmsBomTable> buildQueryWrapper(PmsBomTableBo bo) {
        LambdaQueryWrapper<PmsBomTable> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getName()), PmsBomTable::getName, bo.getName());
        lqw.like(StringUtils.isNotBlank(bo.getSpec()), PmsBomTable::getSpec, bo.getSpec());
        lqw.eq(bo.getProjectId() != null, PmsBomTable::getProjectId, bo.getProjectId());
        lqw.eq(bo.getStatus() != null, PmsBomTable::getStatus, bo.getStatus());
        lqw.orderByAsc(PmsBomTable::getName);
        return lqw;
    }

    @Override
    public Boolean insertByBo(PmsBomTableBo bo) {
        PmsBomTable add = MapstructUtils.convert(bo, PmsBomTable.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(PmsBomTableBo bo) {
        PmsBomTable update = MapstructUtils.convert(bo, PmsBomTable.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<PmsBomTable> list) {
        return baseMapper.insertBatch(list);
    }
}
