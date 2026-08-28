package org.dromara.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.procurement.domain.PmsBomNode;
import org.dromara.procurement.domain.bo.PmsBomNodeBo;
import org.dromara.procurement.domain.vo.PmsBomNodeVo;
import org.dromara.procurement.mapper.PmsBomNodeMapper;
import org.dromara.procurement.service.IPmsBomNodeService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-BOM节点Service业务层处理
 *
 * @author procurement
 */
@RequiredArgsConstructor
@Service
public class PmsBomNodeServiceImpl implements IPmsBomNodeService {

    private final PmsBomNodeMapper baseMapper;

    @Override
    public PmsBomNodeVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public List<PmsBomNodeVo> queryList(PmsBomNodeBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<PmsBomNode> buildQueryWrapper(PmsBomNodeBo bo) {
        LambdaQueryWrapper<PmsBomNode> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getBomTableId() != null, PmsBomNode::getBomTableId, bo.getBomTableId());
        lqw.eq(bo.getParentId() != null, PmsBomNode::getParentId, bo.getParentId());
        lqw.eq(StringUtils.isNotBlank(bo.getNodeType()), PmsBomNode::getNodeType, bo.getNodeType());
        lqw.like(StringUtils.isNotBlank(bo.getItemName()), PmsBomNode::getItemName, bo.getItemName());
        lqw.orderByAsc(PmsBomNode::getSortNo);
        lqw.orderByAsc(PmsBomNode::getId);
        return lqw;
    }

    @Override
    public Boolean insertByBo(PmsBomNodeBo bo) {
        PmsBomNode add = MapstructUtils.convert(bo, PmsBomNode.class);
        if (add.getParentId() == null) {
            add.setParentId(0L);
        }
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(PmsBomNodeBo bo) {
        PmsBomNode update = MapstructUtils.convert(bo, PmsBomNode.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        for (Long id : ids) {
            LambdaQueryWrapper<PmsBomNode> child = Wrappers.lambdaQuery();
            child.eq(PmsBomNode::getParentId, id);
            if (baseMapper.selectCount(child) > 0) {
                throw new ServiceException("存在子节点，不能删除该节点");
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<PmsBomNode> list) {
        return baseMapper.insertBatch(list);
    }
}
