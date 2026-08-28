package org.dromara.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.procurement.domain.PmsAttachment;
import org.dromara.procurement.domain.bo.PmsAttachmentBo;
import org.dromara.procurement.domain.vo.PmsAttachmentVo;
import org.dromara.procurement.mapper.PmsAttachmentMapper;
import org.dromara.procurement.service.IPmsAttachmentService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 采购管理-通用附件Service业务层处理
 *
 * @author procurement
 */
@RequiredArgsConstructor
@Service
public class PmsAttachmentServiceImpl implements IPmsAttachmentService {

    private final PmsAttachmentMapper baseMapper;

    @Override
    public PmsAttachmentVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public PageResult<PmsAttachmentVo> queryPageList(PmsAttachmentBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsAttachment> lqw = buildQueryWrapper(bo);
        Page<PmsAttachmentVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    @Override
    public List<PmsAttachmentVo> queryList(PmsAttachmentBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    public List<PmsAttachmentVo> listByBiz(String bizType, Long bizId) {
        return baseMapper.selectVoList(Wrappers.<PmsAttachment>lambdaQuery()
            .eq(StringUtils.isNotBlank(bizType), PmsAttachment::getBizType, bizType)
            .eq(bizId != null, PmsAttachment::getBizId, bizId)
            .orderByDesc(PmsAttachment::getId));
    }

    private LambdaQueryWrapper<PmsAttachment> buildQueryWrapper(PmsAttachmentBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsAttachment> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getBizType()), PmsAttachment::getBizType, bo.getBizType());
        lqw.eq(bo.getBizId() != null, PmsAttachment::getBizId, bo.getBizId());
        lqw.like(StringUtils.isNotBlank(bo.getFileName()), PmsAttachment::getFileName, bo.getFileName());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            PmsAttachment::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        lqw.orderByDesc(PmsAttachment::getId);
        return lqw;
    }

    @Override
    public Boolean insertByBo(PmsAttachmentBo bo) {
        PmsAttachment add = MapstructUtils.convert(bo, PmsAttachment.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(PmsAttachmentBo bo) {
        PmsAttachment update = MapstructUtils.convert(bo, PmsAttachment.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<PmsAttachment> list) {
        return baseMapper.insertBatch(list);
    }
}
