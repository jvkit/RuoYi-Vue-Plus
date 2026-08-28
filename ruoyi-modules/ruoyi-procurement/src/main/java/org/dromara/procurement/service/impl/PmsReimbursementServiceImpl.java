package org.dromara.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.procurement.domain.PmsReimbursement;
import org.dromara.procurement.domain.bo.PmsReimbursementBo;
import org.dromara.procurement.domain.vo.PmsReimbursementVo;
import org.dromara.procurement.mapper.PmsReimbursementMapper;
import org.dromara.procurement.service.IPmsReimbursementService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 采购管理-报销Service业务层处理
 *
 * @author procurement
 */
@RequiredArgsConstructor
@Service
public class PmsReimbursementServiceImpl implements IPmsReimbursementService {

    private final PmsReimbursementMapper baseMapper;

    @Override
    public PmsReimbursementVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public PageResult<PmsReimbursementVo> queryPageList(PmsReimbursementBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsReimbursement> lqw = buildQueryWrapper(bo);
        Page<PmsReimbursementVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    @Override
    public List<PmsReimbursementVo> queryList(PmsReimbursementBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<PmsReimbursement> buildQueryWrapper(PmsReimbursementBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsReimbursement> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getReimbursementCode()), PmsReimbursement::getReimbursementCode, bo.getReimbursementCode());
        lqw.eq(bo.getRequestId() != null, PmsReimbursement::getRequestId, bo.getRequestId());
        lqw.eq(bo.getAcceptanceId() != null, PmsReimbursement::getAcceptanceId, bo.getAcceptanceId());
        lqw.eq(bo.getProjectId() != null, PmsReimbursement::getProjectId, bo.getProjectId());
        lqw.like(StringUtils.isNotBlank(bo.getApplicant()), PmsReimbursement::getApplicant, bo.getApplicant());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), PmsReimbursement::getStatus, bo.getStatus());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            PmsReimbursement::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        lqw.orderByDesc(PmsReimbursement::getId);
        return lqw;
    }

    @Override
    public Boolean insertByBo(PmsReimbursementBo bo) {
        if (StringUtils.isBlank(bo.getReimbursementCode())) {
            bo.setReimbursementCode(generateReimbursementCode());
        }
        PmsReimbursement add = MapstructUtils.convert(bo, PmsReimbursement.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 生成报销编号 reim-yyyyMMdd-NNN
     */
    private String generateReimbursementCode() {
        String prefix = "reim-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        LambdaQueryWrapper<PmsReimbursement> lqw = Wrappers.lambdaQuery();
        lqw.likeRight(PmsReimbursement::getReimbursementCode, prefix);
        long count = baseMapper.selectCount(lqw);
        return prefix + String.format("%03d", count + 1);
    }

    @Override
    public Boolean updateByBo(PmsReimbursementBo bo) {
        PmsReimbursement update = MapstructUtils.convert(bo, PmsReimbursement.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<PmsReimbursement> list) {
        return baseMapper.insertBatch(list);
    }
}
