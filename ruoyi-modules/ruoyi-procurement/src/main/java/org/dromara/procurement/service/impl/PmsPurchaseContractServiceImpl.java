package org.dromara.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.procurement.domain.PmsPurchaseContract;
import org.dromara.procurement.domain.bo.PmsPurchaseContractBo;
import org.dromara.procurement.domain.vo.PmsPurchaseContractVo;
import org.dromara.procurement.mapper.PmsPurchaseContractMapper;
import org.dromara.procurement.service.IPmsPurchaseContractService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 采购管理-采购合同Service业务层处理
 *
 * @author procurement
 */
@RequiredArgsConstructor
@Service
public class PmsPurchaseContractServiceImpl implements IPmsPurchaseContractService {

    private final PmsPurchaseContractMapper baseMapper;

    @Override
    public PmsPurchaseContractVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public PageResult<PmsPurchaseContractVo> queryPageList(PmsPurchaseContractBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsPurchaseContract> lqw = buildQueryWrapper(bo);
        Page<PmsPurchaseContractVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    @Override
    public List<PmsPurchaseContractVo> queryList(PmsPurchaseContractBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<PmsPurchaseContract> buildQueryWrapper(PmsPurchaseContractBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsPurchaseContract> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getContractNo()), PmsPurchaseContract::getContractNo, bo.getContractNo());
        lqw.eq(bo.getRequestId() != null, PmsPurchaseContract::getRequestId, bo.getRequestId());
        lqw.like(StringUtils.isNotBlank(bo.getTitle()), PmsPurchaseContract::getTitle, bo.getTitle());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), PmsPurchaseContract::getStatus, bo.getStatus());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            PmsPurchaseContract::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        lqw.orderByDesc(PmsPurchaseContract::getId);
        return lqw;
    }

    @Override
    public Boolean insertByBo(PmsPurchaseContractBo bo) {
        if (StringUtils.isBlank(bo.getContractNo())) {
            bo.setContractNo(generateContractNo());
        }
        PmsPurchaseContract add = MapstructUtils.convert(bo, PmsPurchaseContract.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 生成合同编号 ctrt-yyyyMMdd-NNN
     */
    private String generateContractNo() {
        String prefix = "ctrt-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        LambdaQueryWrapper<PmsPurchaseContract> lqw = Wrappers.lambdaQuery();
        lqw.likeRight(PmsPurchaseContract::getContractNo, prefix);
        long count = baseMapper.selectCount(lqw);
        return prefix + String.format("%03d", count + 1);
    }

    @Override
    public Boolean updateByBo(PmsPurchaseContractBo bo) {
        PmsPurchaseContract update = MapstructUtils.convert(bo, PmsPurchaseContract.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<PmsPurchaseContract> list) {
        return baseMapper.insertBatch(list);
    }
}
