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
import org.dromara.system.api.DeptService;
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
    private final DeptService deptService;

    /**
     * 批量填充归属部门名称
     */
    private void fillDeptName(List<PmsProjectVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (PmsProjectVo vo : list) {
            if (vo.getDeptId() != null) {
                vo.setDeptName(deptService.selectDeptNameByIds(vo.getDeptId().toString()));
            }
        }
    }

    @Override
    public PmsProjectVo queryById(Long id) {
        PmsProjectVo vo = baseMapper.selectVoById(id);
        if (vo != null) {
            fillDeptName(List.of(vo));
        }
        return vo;
    }

    @Override
    public PageResult<PmsProjectVo> queryPageList(PmsProjectBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsProject> lqw = buildQueryWrapper(bo);
        Page<PmsProjectVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        fillDeptName(result.getRecords());
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    @Override
    public List<PmsProjectVo> queryList(PmsProjectBo bo) {
        List<PmsProjectVo> list = baseMapper.selectVoList(buildQueryWrapper(bo));
        fillDeptName(list);
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
        fillDeptName(all);
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
        // 子项目归属部门默认继承父项目
        if (bo.getParentId() != null && bo.getParentId() != 0 && bo.getDeptId() == null) {
            PmsProject parent = baseMapper.selectById(bo.getParentId());
            if (parent != null) {
                bo.setDeptId(parent.getDeptId());
            }
        }
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
        // 子项目预算不得超过父项目剩余预算
        if (entity.getParentId() != null && entity.getParentId() != 0) {
            PmsProject parent = baseMapper.selectById(entity.getParentId());
            if (parent == null) {
                throw new ServiceException("上级项目不存在");
            }
            BigDecimal budget = entity.getBudget() == null ? BigDecimal.ZERO : entity.getBudget();
            BigDecimal parentBudget = parent.getBudget() == null ? BigDecimal.ZERO : parent.getBudget();
            BigDecimal parentUsed = parent.getUsedAmount() == null ? BigDecimal.ZERO : parent.getUsedAmount();
            // 父项目剩余 = 父预算 - 父已用 - 其他子项目预算之和
            LambdaQueryWrapper<PmsProject> siblings = Wrappers.lambdaQuery();
            siblings.eq(PmsProject::getParentId, entity.getParentId());
            if (entity.getId() != null) {
                siblings.ne(PmsProject::getId, entity.getId());
            }
            BigDecimal siblingBudget = baseMapper.selectList(siblings).stream()
                .map(s -> s.getBudget() == null ? BigDecimal.ZERO : s.getBudget())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal parentRemaining = parentBudget.subtract(parentUsed).subtract(siblingBudget);
            if (budget.compareTo(parentRemaining) > 0) {
                throw new ServiceException("子项目预算不能超过父项目剩余预算（父剩余 "
                    + parentRemaining.stripTrailingZeros().toPlainString() + " 元）");
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
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<PmsProject> list) {
        return baseMapper.insertBatch(list);
    }
}
