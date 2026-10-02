package org.dromara.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.procurement.domain.PmsFundSource;
import org.dromara.procurement.domain.vo.PmsFundSourceVo;
import org.dromara.procurement.mapper.PmsFundSourceMapper;
import org.dromara.procurement.service.IPmsFundSourceService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 采购管理-项目归属(资金来源)Service业务层处理
 *
 * @author procurement
 */
@RequiredArgsConstructor
@Service
public class PmsFundSourceServiceImpl implements IPmsFundSourceService {

    private final PmsFundSourceMapper baseMapper;

    @Override
    public List<PmsFundSourceVo> queryTree() {
        List<PmsFundSourceVo> all = baseMapper.selectVoList(
            Wrappers.<PmsFundSource>lambdaQuery()
                .eq(PmsFundSource::getStatus, 1)
                .orderByAsc(PmsFundSource::getSort)
                .orderByAsc(PmsFundSource::getId));
        return buildTree(all, 0L);
    }

    /**
     * 根据 parentId 组装树形结构
     */
    private List<PmsFundSourceVo> buildTree(List<PmsFundSourceVo> all, Long parentId) {
        List<PmsFundSourceVo> tree = new ArrayList<>();
        for (PmsFundSourceVo vo : all) {
            Long pid = vo.getParentId() == null ? 0L : vo.getParentId();
            if (parentId.equals(pid)) {
                vo.setChildren(buildTree(all, vo.getId()));
                tree.add(vo);
            }
        }
        return tree;
    }

    /**
     * 批量查询归属名称（供项目列表填充）
     */
    public String selectNameById(Long id) {
        if (id == null) {
            return null;
        }
        LambdaQueryWrapper<PmsFundSource> lqw = Wrappers.lambdaQuery();
        lqw.select(PmsFundSource::getName).eq(PmsFundSource::getId, id);
        PmsFundSource one = baseMapper.selectOne(lqw);
        return one == null ? null : one.getName();
    }

}
