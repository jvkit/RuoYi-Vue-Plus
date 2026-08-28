package org.dromara.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.procurement.domain.PmsProject;
import org.dromara.procurement.domain.PmsWarehouseStock;
import org.dromara.procurement.domain.bo.PmsWarehouseStockBo;
import org.dromara.procurement.domain.vo.PmsWarehouseStockVo;
import org.dromara.procurement.mapper.PmsProjectMapper;
import org.dromara.procurement.mapper.PmsWarehouseStockMapper;
import org.dromara.procurement.service.IPmsWarehouseStockService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 采购管理-仓库库存Service业务层处理
 *
 * @author procurement
 */
@RequiredArgsConstructor
@Service
public class PmsWarehouseStockServiceImpl implements IPmsWarehouseStockService {

    private final PmsWarehouseStockMapper baseMapper;
    private final PmsProjectMapper projectMapper;

    /**
     * 批量填充项目名称
     */
    private void fillProjectName(List<PmsWarehouseStockVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> projectIds = list.stream()
            .map(PmsWarehouseStockVo::getProjectId)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());
        if (projectIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = projectMapper.selectByIds(projectIds)
            .stream()
            .collect(Collectors.toMap(PmsProject::getId, PmsProject::getProjectName, (a, b) -> a));
        for (PmsWarehouseStockVo vo : list) {
            if (vo.getProjectId() != null) {
                vo.setProjectName(nameMap.getOrDefault(vo.getProjectId(), ""));
            }
        }
    }

    @Override
    public PmsWarehouseStockVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public PageResult<PmsWarehouseStockVo> queryPageList(PmsWarehouseStockBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsWarehouseStock> lqw = buildQueryWrapper(bo);
        Page<PmsWarehouseStockVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        fillProjectName(result.getRecords());
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    @Override
    public List<PmsWarehouseStockVo> queryList(PmsWarehouseStockBo bo) {
        List<PmsWarehouseStockVo> list = baseMapper.selectVoList(buildQueryWrapper(bo));
        fillProjectName(list);
        return list;
    }

    private LambdaQueryWrapper<PmsWarehouseStock> buildQueryWrapper(PmsWarehouseStockBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsWarehouseStock> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getItemName()), PmsWarehouseStock::getItemName, bo.getItemName());
        lqw.like(StringUtils.isNotBlank(bo.getBrand()), PmsWarehouseStock::getBrand, bo.getBrand());
        lqw.eq(bo.getProjectId() != null, PmsWarehouseStock::getProjectId, bo.getProjectId());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            PmsWarehouseStock::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        lqw.orderByDesc(PmsWarehouseStock::getCreateTime);
        return lqw;
    }

    @Override
    public Boolean insertByBo(PmsWarehouseStockBo bo) {
        PmsWarehouseStock add = MapstructUtils.convert(bo, PmsWarehouseStock.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(PmsWarehouseStockBo bo) {
        PmsWarehouseStock update = MapstructUtils.convert(bo, PmsWarehouseStock.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<PmsWarehouseStock> list) {
        return baseMapper.insertBatch(list);
    }
}
