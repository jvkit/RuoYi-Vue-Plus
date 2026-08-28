package org.dromara.workflow.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.dto.StartProcessDTO;
import org.dromara.common.core.domain.event.ProcessDeleteEvent;
import org.dromara.common.core.domain.event.ProcessEvent;
import org.dromara.common.core.domain.event.ProcessTaskEvent;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.WorkflowService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.domain.ProcurementRequest;
import org.dromara.workflow.domain.bo.ProcurementRequestBo;
import org.dromara.workflow.domain.vo.ProcurementRequestVo;
import org.dromara.workflow.mapper.ProcurementRequestMapper;
import org.dromara.workflow.service.IProcurementRequestService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 采购申请Service业务层处理
 *
 * @author Lion Li
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@Service
@Slf4j
public class ProcurementRequestServiceImpl implements IProcurementRequestService {

    private final ProcurementRequestMapper baseMapper;
    private final WorkflowService workflowService;

    /**
     * 查询采购申请
     */
    @Override
    public ProcurementRequestVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询采购申请列表
     */
    @Override
    public TableDataInfo<ProcurementRequestVo> queryPageList(ProcurementRequestBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ProcurementRequest> lqw = buildQueryWrapper(bo);
        Page<ProcurementRequestVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询采购申请列表
     */
    @Override
    public List<ProcurementRequestVo> queryList(ProcurementRequestBo bo) {
        LambdaQueryWrapper<ProcurementRequest> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<ProcurementRequest> buildQueryWrapper(ProcurementRequestBo bo) {
        LambdaQueryWrapper<ProcurementRequest> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getTitle()), ProcurementRequest::getTitle, bo.getTitle());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), ProcurementRequest::getStatus, bo.getStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getPurchaseType()), ProcurementRequest::getPurchaseType, bo.getPurchaseType());
        lqw.orderByDesc(BaseEntity::getCreateTime);
        return lqw;
    }

    /**
     * 新增采购申请
     */
    @Override
    public ProcurementRequestVo insertByBo(ProcurementRequestBo bo) {
        bo.setApplyCode(System.currentTimeMillis() + StrUtil.EMPTY);
        // 根据金额自动判断采购类型
        if (ObjectUtil.isNotNull(bo.getAmount())) {
            bo.setPurchaseType(bo.getAmount().compareTo(new java.math.BigDecimal("10000")) >= 0 ? "above" : "below");
        }
        ProcurementRequest add = MapstructUtils.convert(bo, ProcurementRequest.class);
        if (StringUtils.isBlank(add.getStatus())) {
            add.setStatus(BusinessStatusEnum.DRAFT.getStatus());
        }
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return MapstructUtils.convert(add, ProcurementRequestVo.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ProcurementRequestVo submitAndFlowStart(ProcurementRequestBo bo) {
        if (ObjectUtil.isNull(bo.getId())) {
            bo.setApplyCode(System.currentTimeMillis() + StrUtil.EMPTY);
        }
        // 根据金额自动判断采购类型
        if (ObjectUtil.isNotNull(bo.getAmount())) {
            bo.setPurchaseType(bo.getAmount().compareTo(new java.math.BigDecimal("10000")) >= 0 ? "above" : "below");
        }
        ProcurementRequest procurement = MapstructUtils.convert(bo, ProcurementRequest.class);
        boolean flag = baseMapper.insertOrUpdate(procurement);
        if (flag) {
            bo.setId(procurement.getId());
            bo.getParams().put("ignore", true);

            StartProcessDTO startProcess = new StartProcessDTO();
            startProcess.setBusinessId(procurement.getId().toString());
            startProcess.setFlowCode(StringUtils.isEmpty(bo.getFlowCode()) ? "procurement1" : bo.getFlowCode());
            startProcess.setVariables(bo.getParams());

            boolean flag1 = workflowService.startCompleteTask(startProcess);
            if (!flag1) {
                throw new ServiceException("流程发起异常");
            }
        }
        return MapstructUtils.convert(procurement, ProcurementRequestVo.class);
    }

    /**
     * 修改采购申请
     */
    @Override
    public ProcurementRequestVo updateByBo(ProcurementRequestBo bo) {
        ProcurementRequest update = MapstructUtils.convert(bo, ProcurementRequest.class);
        baseMapper.updateById(update);
        return MapstructUtils.convert(update, ProcurementRequestVo.class);
    }

    /**
     * 批量删除采购申请
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(List<Long> ids) {
        workflowService.deleteInstance(StreamUtils.toList(ids, Convert::toStr));
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 总体流程监听
     */
    @EventListener(condition = "#processEvent.flowCode.startsWith('procurement')")
    public void processHandler(ProcessEvent processEvent) {
        log.info("采购流程执行了{}", processEvent);
        ProcurementRequest procurement = baseMapper.selectById(Convert.toLong(processEvent.getBusinessId()));
        if (ObjectUtil.isNull(procurement)) {
            return;
        }
        procurement.setStatus(processEvent.getStatus());
        Map<String, Object> params = processEvent.getParams();
        if (processEvent.getSubmit()) {
            if (StringUtils.isBlank(procurement.getApplyCode())) {
                String businessCode = MapUtil.getStr(params, FlowConstant.BUSINESS_CODE, StrUtil.EMPTY);
                procurement.setApplyCode(businessCode);
            }
            procurement.setStatus(BusinessStatusEnum.WAITING.getStatus());
            log.info("采购申请人提交");
        }
        String status = BusinessStatusEnum.findByStatus(processEvent.getStatus());
        log.info("当前采购流程状态为{}", status);
        baseMapper.updateById(procurement);
    }

    /**
     * 执行任务创建监听
     */
    @EventListener(condition = "#processTaskEvent.flowCode.startsWith('procurement')")
    public void processTaskHandler(ProcessTaskEvent processTaskEvent) {
        log.info("采购流程任务创建了{}", processTaskEvent);
    }

    /**
     * 监听删除流程事件
     */
    @EventListener(condition = "#processDeleteEvent.flowCode.startsWith('procurement')")
    public void processDeleteHandler(ProcessDeleteEvent processDeleteEvent) {
        log.info("监听删除采购流程事件，当前任务执行了{}", processDeleteEvent);
        ProcurementRequest procurement = baseMapper.selectById(Convert.toLong(processDeleteEvent.getBusinessId()));
        if (ObjectUtil.isNull(procurement)) {
            return;
        }
        baseMapper.deleteById(procurement.getId());
    }
}
