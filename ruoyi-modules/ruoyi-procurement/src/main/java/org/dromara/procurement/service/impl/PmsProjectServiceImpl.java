package org.dromara.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.procurement.domain.PmsProject;
import org.dromara.procurement.domain.bo.PmsProjectBo;
import org.dromara.procurement.domain.vo.PmsProjectVo;
import org.dromara.procurement.mapper.PmsProjectMapper;
import org.dromara.procurement.service.IPmsProjectService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private final PmsFundSourceServiceImpl fundSourceService;

    /**
     * 批量填充归属名称
     */
    private void fillOwnerName(List<PmsProjectVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (PmsProjectVo vo : list) {
            vo.setOwnerName(fundSourceService.selectNameById(vo.getOwnerId()));
        }
    }

    @Override
    public PmsProjectVo queryById(Long id) {
        PmsProjectVo vo = baseMapper.selectVoById(id);
        if (vo != null) {
            fillOwnerName(List.of(vo));
        }
        return vo;
    }

    @Override
    public PageResult<PmsProjectVo> queryPageList(PmsProjectBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsProject> lqw = buildQueryWrapper(bo);
        Page<PmsProjectVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        fillOwnerName(result.getRecords());
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    @Override
    public List<PmsProjectVo> queryList(PmsProjectBo bo) {
        List<PmsProjectVo> list = baseMapper.selectVoList(buildQueryWrapper(bo));
        fillOwnerName(list);
        return list;
    }

    private LambdaQueryWrapper<PmsProject> buildQueryWrapper(PmsProjectBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsProject> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getProjectCode()), PmsProject::getProjectCode, bo.getProjectCode());
        lqw.like(StringUtils.isNotBlank(bo.getProjectName()), PmsProject::getProjectName, bo.getProjectName());
        lqw.eq(StringUtils.isNotBlank(bo.getLeader()), PmsProject::getLeader, bo.getLeader());
        lqw.eq(bo.getStatus() != null, PmsProject::getStatus, bo.getStatus());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            PmsProject::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        lqw.orderByAsc(PmsProject::getProjectCode);
        return lqw;
    }

    @Override
    public List<PmsProjectVo> queryTreeList() {
        List<PmsProjectVo> all = baseMapper.selectVoList(
            Wrappers.<PmsProject>lambdaQuery()
                .orderByAsc(PmsProject::getProjectCode));
        fillOwnerName(all);
        return buildTree(all, 0L);
    }

    /**
     * 根据 parentId 组装树形结构
     */
    private List<PmsProjectVo> buildTree(List<PmsProjectVo> all, Long parentId) {
        List<PmsProjectVo> tree = new ArrayList<>();
        for (PmsProjectVo vo : all) {
            Long pid = vo.getParentId() == null ? 0L : vo.getParentId();
            if (parentId.equals(pid)) {
                vo.setChildren(buildTree(all, vo.getId()));
                tree.add(vo);
            }
        }
        return tree;
    }

    @Override
    public Boolean insertByBo(PmsProjectBo bo) {
        if (StringUtils.isBlank(bo.getProjectCode())) {
            bo.setProjectCode(generateProjectCode());
        }
        // 子项目归属默认继承父项目
        if (bo.getParentId() != null && bo.getParentId() != 0 && bo.getOwnerId() == null) {
            PmsProject parent = baseMapper.selectById(bo.getParentId());
            if (parent != null) {
                bo.setOwnerId(parent.getOwnerId());
            }
        }
        PmsProject add = MapstructUtils.convert(bo, PmsProject.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
            syncAncestors(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(PmsProjectBo bo) {
        PmsProject update = MapstructUtils.convert(bo, PmsProject.class);
        validEntityBeforeSave(update);
        boolean flag = baseMapper.updateById(update) > 0;
        if (flag) {
            syncAncestors(update.getId());
        }
        return flag;
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
        // 上级项目必须存在（父级金额由子级之和自动同步，不再做"子预算不得超父剩余"校验）
        if (entity.getParentId() != null && entity.getParentId() != 0) {
            PmsProject parent = baseMapper.selectById(entity.getParentId());
            if (parent == null) {
                throw new ServiceException("上级项目不存在");
            }
        }
    }

    /**
     * 生成项目编码 purp-yyyyMMdd-NNN（按当天已有数量递增）
     */
    private String generateProjectCode() {
        String prefix = "purp-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        LambdaQueryWrapper<PmsProject> lqw = Wrappers.lambdaQuery();
        lqw.likeRight(PmsProject::getProjectCode, prefix);
        long count = baseMapper.selectCount(lqw);
        return prefix + String.format("%03d", count + 1);
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        for (Long id : ids) {
            LambdaQueryWrapper<PmsProject> child = Wrappers.lambdaQuery();
            child.eq(PmsProject::getParentId, id);
            if (baseMapper.selectCount(child) > 0) {
                throw new ServiceException("存在二级项目，不能删除该主项目");
            }
        }
        // 先记录父级，删除后自底向上同步
        List<Long> parentIds = ids.stream()
            .map(id -> {
                PmsProject p = baseMapper.selectById(id);
                return p == null ? null : p.getParentId();
            })
            .filter(pid -> pid != null && pid != 0)
            .distinct()
            .collect(java.util.stream.Collectors.toList());
        boolean flag = baseMapper.deleteByIds(ids) > 0;
        if (flag) {
            parentIds.forEach(this::syncAncestors);
        }
        return flag;
    }

    @Override
    public void syncAncestors(Long nodeId) {
        if (nodeId == null) {
            return;
        }
        PmsProject node = baseMapper.selectById(nodeId);
        Long pid = node == null ? null : node.getParentId();
        while (pid != null && pid != 0) {
            PmsProject parent = baseMapper.selectById(pid);
            if (parent == null) {
                break;
            }
            List<PmsProject> children = baseMapper.selectList(
                Wrappers.<PmsProject>lambdaQuery().eq(PmsProject::getParentId, pid));
            BigDecimal budget = children.stream()
                .map(c -> c.getBudget() == null ? BigDecimal.ZERO : c.getBudget())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal used = children.stream()
                .map(c -> c.getUsedAmount() == null ? BigDecimal.ZERO : c.getUsedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            PmsProject upd = new PmsProject();
            upd.setId(pid);
            upd.setBudget(budget);
            upd.setUsedAmount(used);
            baseMapper.updateById(upd);
            pid = parent.getParentId();
        }
    }

    @Override
    public void recomputeAllParentAmounts() {
        List<PmsProject> all = baseMapper.selectList(Wrappers.lambdaQuery());
        if (all.isEmpty()) {
            return;
        }
        Map<Long, PmsProject> byId = new java.util.HashMap<>();
        all.forEach(p -> byId.put(p.getId(), p));
        // 单次遍历即可：childrenMap 持有 all 中同一对象引用，父级更新后祖父级读取到的即为新值
        all.stream()
            .filter(p -> p.getParentId() != null && p.getParentId() != 0 && byId.containsKey(p.getParentId()))
            .collect(java.util.stream.Collectors.groupingBy(PmsProject::getParentId))
            .forEach((parentId, children) -> {
                PmsProject parent = byId.get(parentId);
                BigDecimal budget = children.stream()
                    .map(c -> c.getBudget() == null ? BigDecimal.ZERO : c.getBudget())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal used = children.stream()
                    .map(c -> c.getUsedAmount() == null ? BigDecimal.ZERO : c.getUsedAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                parent.setBudget(budget);
                parent.setUsedAmount(used);
                baseMapper.updateById(parent);
            });
    }

    @Override
    public Boolean saveBatch(List<PmsProject> list) {
        return baseMapper.insertBatch(list);
    }
}
