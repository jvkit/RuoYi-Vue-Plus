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
import org.dromara.procurement.domain.PmsSupplier;
import org.dromara.procurement.domain.bo.PmsSupplierBo;
import org.dromara.procurement.domain.vo.PmsSupplierVo;
import org.dromara.procurement.mapper.PmsSupplierMapper;
import org.dromara.procurement.service.IPmsSupplierService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 采购管理-供应商Service业务层处理
 *
 * @author procurement
 */
@RequiredArgsConstructor
@Service
public class PmsSupplierServiceImpl implements IPmsSupplierService {

    private final PmsSupplierMapper baseMapper;

    @Override
    public PmsSupplierVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public PageResult<PmsSupplierVo> queryPageList(PmsSupplierBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsSupplier> lqw = buildQueryWrapper(bo);
        Page<PmsSupplierVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    @Override
    public List<PmsSupplierVo> queryList(PmsSupplierBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<PmsSupplier> buildQueryWrapper(PmsSupplierBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsSupplier> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getSupplierCode()), PmsSupplier::getSupplierCode, bo.getSupplierCode());
        lqw.like(StringUtils.isNotBlank(bo.getSupplierName()), PmsSupplier::getSupplierName, bo.getSupplierName());
        lqw.like(StringUtils.isNotBlank(bo.getContactName()), PmsSupplier::getContactName, bo.getContactName());
        lqw.eq(bo.getStatus() != null, PmsSupplier::getStatus, bo.getStatus());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            PmsSupplier::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        lqw.orderByAsc(PmsSupplier::getSupplierCode);
        return lqw;
    }

    @Override
    public Boolean insertByBo(PmsSupplierBo bo) {
        PmsSupplier add = MapstructUtils.convert(bo, PmsSupplier.class);
        // 编码可空，为空时自动生成
        if (StringUtils.isBlank(add.getSupplierCode())) {
            add.setSupplierCode("SUP-" + System.currentTimeMillis());
        }
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(PmsSupplierBo bo) {
        PmsSupplier update = MapstructUtils.convert(bo, PmsSupplier.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(PmsSupplier entity) {
        // 校验供应商编码唯一性
        LambdaQueryWrapper<PmsSupplier> lqw = Wrappers.lambdaQuery();
        lqw.eq(PmsSupplier::getSupplierCode, entity.getSupplierCode());
        if (entity.getId() != null) {
            lqw.ne(PmsSupplier::getId, entity.getId());
        }
        if (baseMapper.selectCount(lqw) > 0) {
            throw new ServiceException("供应商编码已存在");
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<PmsSupplier> list) {
        return baseMapper.insertBatch(list);
    }
}
