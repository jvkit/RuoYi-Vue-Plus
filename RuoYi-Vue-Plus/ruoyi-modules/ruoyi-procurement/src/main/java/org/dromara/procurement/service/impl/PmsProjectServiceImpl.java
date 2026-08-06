package org.dromara.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.procurement.domain.PmsProject;
import org.dromara.procurement.domain.bo.PmsProjectBo;
import org.dromara.procurement.domain.vo.PmsProjectVo;
import org.dromara.procurement.mapper.PmsProjectMapper;
import org.dromara.procurement.service.IPmsProjectService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 采购管理-项目Service业务层处理
 *
 * @author procurement
 */
@RequiredArgsConstructor
@Service
public class PmsProjectServiceImpl implements IPmsProjectService {

    private final PmsProjectMapper baseMapper;

    @Override
    public PmsProjectVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<PmsProjectVo> queryPageList(PmsProjectBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsProject> lqw = buildQueryWrapper(bo);
        Page<PmsProjectVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<PmsProjectVo> queryList(PmsProjectBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<PmsProject> buildQueryWrapper(PmsProjectBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsProject> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getProjectCode()), PmsProject::getProjectCode, bo.getProjectCode());
        lqw.like(StringUtils.isNotBlank(bo.getProjectName()), PmsProject::getProjectName, bo.getProjectName());
        lqw.eq(bo.getDeptId() != null, PmsProject::getDeptId, bo.getDeptId());
        lqw.eq(StringUtils.isNotBlank(bo.getLeader()), PmsProject::getLeader, bo.getLeader());
        lqw.eq(bo.getStatus() != null, PmsProject::getStatus, bo.getStatus());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            PmsProject::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        lqw.orderByAsc(PmsProject::getProjectCode);
        return lqw;
    }

    @Override
    public Boolean insertByBo(PmsProjectBo bo) {
        PmsProject add = MapstructUtils.convert(bo, PmsProject.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(PmsProjectBo bo) {
        PmsProject update = MapstructUtils.convert(bo, PmsProject.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(PmsProject entity) {
        LambdaQueryWrapper<PmsProject> lqw = Wrappers.lambdaQuery();
        lqw.eq(PmsProject::getProjectCode, entity.getProjectCode());
        if (entity.getId() != null) {
            lqw.ne(PmsProject::getId, entity.getId());
        }
        if (baseMapper.selectCount(lqw) > 0) {
            throw new ServiceException("项目编码已存在");
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<PmsProject> list) {
        return baseMapper.insertBatch(list);
    }
}
