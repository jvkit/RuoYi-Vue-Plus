package org.dromara.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.procurement.domain.PmsOperationLog;
import org.dromara.procurement.domain.bo.PmsOperationLogBo;
import org.dromara.procurement.domain.vo.PmsOperationLogVo;
import org.dromara.procurement.mapper.PmsOperationLogMapper;
import org.dromara.procurement.service.IPmsOperationLogService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 采购管理-流转记录Service业务层处理
 *
 * @author procurement
 */
@RequiredArgsConstructor
@Service
public class PmsOperationLogServiceImpl implements IPmsOperationLogService {

    private final PmsOperationLogMapper baseMapper;

    @Override
    public PmsOperationLogVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public PageResult<PmsOperationLogVo> queryPageList(PmsOperationLogBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsOperationLog> lqw = buildQueryWrapper(bo);
        Page<PmsOperationLogVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    @Override
    public List<PmsOperationLogVo> queryList(PmsOperationLogBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<PmsOperationLog> buildQueryWrapper(PmsOperationLogBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsOperationLog> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getBizType()), PmsOperationLog::getBizType, bo.getBizType());
        lqw.eq(bo.getBizId() != null, PmsOperationLog::getBizId, bo.getBizId());
        lqw.eq(StringUtils.isNotBlank(bo.getAction()), PmsOperationLog::getAction, bo.getAction());
        lqw.eq(bo.getOperator() != null, PmsOperationLog::getOperator, bo.getOperator());
        lqw.like(StringUtils.isNotBlank(bo.getOperatorName()), PmsOperationLog::getOperatorName, bo.getOperatorName());
        lqw.eq(StringUtils.isNotBlank(bo.getFromStatus()), PmsOperationLog::getFromStatus, bo.getFromStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getToStatus()), PmsOperationLog::getToStatus, bo.getToStatus());
        lqw.between(params.get("beginOperateTime") != null && params.get("endOperateTime") != null,
            PmsOperationLog::getOperateTime, params.get("beginOperateTime"), params.get("endOperateTime"));
        lqw.orderByDesc(PmsOperationLog::getOperateTime);
        return lqw;
    }
}
