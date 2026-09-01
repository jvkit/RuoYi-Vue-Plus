package org.dromara.procurement.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.procurement.domain.PmsIssueRequest;
import org.dromara.procurement.domain.PmsStockMovement;
import org.dromara.procurement.domain.PmsWarehouseStock;
import org.dromara.procurement.domain.bo.PmsIssueRequestBo;
import org.dromara.procurement.domain.vo.PmsIssueRequestVo;
import org.dromara.procurement.mapper.PmsFlowApproverMapper;
import org.dromara.procurement.mapper.PmsIssueRequestMapper;
import org.dromara.procurement.mapper.PmsProjectMapper;
import org.dromara.procurement.mapper.PmsStockMovementMapper;
import org.dromara.procurement.mapper.PmsWarehouseStockMapper;
import org.dromara.procurement.service.IPmsIssueRequestService;
import org.dromara.workflow.api.WorkflowService;
import org.dromara.workflow.api.domain.StartProcessDTO;
import org.dromara.workflow.api.event.ProcessEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购管理-领用申请Service业务层处理
 *
 * @author procurement
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PmsIssueRequestServiceImpl implements IPmsIssueRequestService {

    private final PmsIssueRequestMapper baseMapper;
    private final PmsWarehouseStockMapper warehouseStockMapper;
    private final PmsStockMovementMapper stockMovementMapper;
    private final PmsProjectMapper projectMapper;
    private final PmsFlowApproverMapper flowApproverMapper;
    private final WorkflowService workflowService;

    @Override
    public PmsIssueRequestVo queryById(Long id) {
        PmsIssueRequestVo vo = baseMapper.selectVoById(id);
        if (vo != null && vo.getProcessInstanceId() != null) {
            List<String> names = flowApproverMapper.selectCurrentApproverNames(vo.getProcessInstanceId());
            if (CollUtil.isNotEmpty(names)) {
                vo.setCurrentApprover(String.join("、", names));
            }
        }
        return vo;
    }

    @Override
    public PmsIssueRequest getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public PageResult<PmsIssueRequestVo> queryPageList(PmsIssueRequestBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsIssueRequest> lqw = buildQueryWrapper(bo);
        Page<PmsIssueRequestVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        fillCurrentApprover(result.getRecords());
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    @Override
    public List<PmsIssueRequestVo> queryList(PmsIssueRequestBo bo) {
        List<PmsIssueRequestVo> list = baseMapper.selectVoList(buildQueryWrapper(bo));
        fillCurrentApprover(list);
        return list;
    }

    /**
     * 填充当前审批人（从流程 waiting 任务动态查询）
     */
    private void fillCurrentApprover(List<PmsIssueRequestVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (PmsIssueRequestVo vo : list) {
            if (vo.getProcessInstanceId() != null) {
                List<String> names = flowApproverMapper.selectCurrentApproverNames(vo.getProcessInstanceId());
                if (CollUtil.isNotEmpty(names)) {
                    vo.setCurrentApprover(String.join("、", names));
                }
            }
        }
    }

    private LambdaQueryWrapper<PmsIssueRequest> buildQueryWrapper(PmsIssueRequestBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsIssueRequest> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getIssueCode()), PmsIssueRequest::getIssueCode, bo.getIssueCode());
        lqw.like(StringUtils.isNotBlank(bo.getItemName()), PmsIssueRequest::getItemName, bo.getItemName());
        lqw.like(StringUtils.isNotBlank(bo.getApplicant()), PmsIssueRequest::getApplicant, bo.getApplicant());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), PmsIssueRequest::getStatus, bo.getStatus());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            PmsIssueRequest::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        lqw.orderByDesc(PmsIssueRequest::getCreateTime);
        return lqw;
    }

    @Override
    public Boolean insertByBo(PmsIssueRequestBo bo) {
        if (StringUtils.isBlank(bo.getIssueCode())) {
            bo.setIssueCode(generateIssueCode());
        }
        if (StringUtils.isBlank(bo.getApplicant())) {
            bo.setApplicant(LoginHelper.getUsername());
        }
        PmsIssueRequest add = MapstructUtils.convert(bo, PmsIssueRequest.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 生成领用编号 issue-yyyyMMdd-NNN
     */
    private String generateIssueCode() {
        String prefix = "issue-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        LambdaQueryWrapper<PmsIssueRequest> lqw = Wrappers.lambdaQuery();
        lqw.likeRight(PmsIssueRequest::getIssueCode, prefix);
        long count = baseMapper.selectCount(lqw);
        return prefix + String.format("%03d", count + 1);
    }

    @Override
    public Boolean updateByBo(PmsIssueRequestBo bo) {
        PmsIssueRequest update = MapstructUtils.convert(bo, PmsIssueRequest.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean approve(Long id, String status) {
        PmsIssueRequest entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("领用申请不存在");
        }
        if (!"pending".equals(entity.getStatus())) {
            throw new ServiceException("当前状态不是待审批，不能审批");
        }
        PmsIssueRequest update = new PmsIssueRequest();
        update.setId(id);
        update.setStatus(status);
        update.setApproveTime(new Date());
        boolean flag = baseMapper.updateById(update) > 0;
        // 审批通过：扣减库存 + 写出库流水
        if (flag && "approved".equals(status)) {
            PmsWarehouseStock stock = entity.getStockId() == null ? null : warehouseStockMapper.selectById(entity.getStockId());
            if (stock == null) {
                throw new ServiceException("关联的仓库物料不存在，无法出库");
            }
            BigDecimal qty = entity.getQtyRequested() == null ? BigDecimal.ZERO : entity.getQtyRequested();
            BigDecimal available = stock.getQtyAvailable() == null ? BigDecimal.ZERO : stock.getQtyAvailable();
            if (qty.compareTo(available) > 0) {
                throw new ServiceException("领用数量超过库存（当前 " + available.stripTrailingZeros().toPlainString() + "）");
            }
            stock.setQtyAvailable(available.subtract(qty));
            warehouseStockMapper.updateById(stock);
            PmsStockMovement movement = new PmsStockMovement();
            movement.setStockId(stock.getId());
            movement.setMovementType("out");
            movement.setQty(qty);
            movement.setRelateId(entity.getId());
            movement.setRelateType("issue");
            movement.setOperateTime(new Date());
            stockMovementMapper.insert(movement);
        }
        return flag;
    }

    /**
     * 提交领用申请并启动流程：发起人 → 仓库管理员 → 结束
     *
     * @param bo 领用申请
     * @return 领用申请详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PmsIssueRequestVo submitAndStartFlow(PmsIssueRequestBo bo) {
        if (ObjectUtil.isNull(bo.getId())) {
            insertByBo(bo);
        } else {
            PmsIssueRequest exist = baseMapper.selectById(bo.getId());
            if (ObjectUtil.isNull(exist)) {
                throw new ServiceException("领用申请不存在");
            }
            if (StringUtils.isNotBlank(exist.getStatus()) && "approved".equals(exist.getStatus())) {
                throw new ServiceException("领用申请已通过，不能重复提交");
            }
            if (StringUtils.isNotBlank(exist.getStatus()) && "waiting".equals(exist.getStatus())) {
                throw new ServiceException("该领用申请已在审批流程中，请勿重复提交");
            }
            updateByBo(bo);
        }

        PmsIssueRequest entity = baseMapper.selectById(bo.getId());

        // 根据库存物料关联的项目，取项目负责人作为审批人
        Long leaderId = null;
        if (entity.getStockId() != null) {
            PmsWarehouseStock stock = warehouseStockMapper.selectById(entity.getStockId());
            if (stock != null && stock.getProjectId() != null) {
                org.dromara.procurement.domain.PmsProject project = projectMapper.selectById(stock.getProjectId());
                if (project != null) {
                    leaderId = project.getLeaderId();
                }
            }
        }
        if (leaderId == null) {
            throw new ServiceException("无法确定项目负责人，请确认库存物料已关联项目");
        }

        StartProcessDTO startProcess = new StartProcessDTO();
        startProcess.setBusinessId(entity.getId().toString());
        startProcess.setFlowCode("pms_issue_request");
        startProcess.setVariables(new HashMap<>());
        startProcess.getVariables().put("leaderId", leaderId.toString());
        boolean started = workflowService.startCompleteTask(startProcess);
        if (!started) {
            throw new ServiceException("流程发起失败");
        }
        entity.setStatus(BusinessStatusEnum.WAITING.getStatus());
        baseMapper.updateById(entity);
        return queryById(entity.getId());
    }

    /**
     * 领用申请流程总体监听：同步状态；流程完成时自动扣减库存
     */
    @EventListener(condition = "#processEvent.flowCode.startsWith('pms_issue_request')")
    public void processHandler(ProcessEvent processEvent) {
        log.info("领用申请流程执行了{}", processEvent);
        PmsIssueRequest entity = baseMapper.selectById(Convert.toLong(processEvent.getBusinessId()));
        if (ObjectUtil.isNull(entity)) {
            return;
        }
        String oldStatus = entity.getStatus();
        entity.setStatus(processEvent.getStatus());
        entity.setProcessInstanceId(processEvent.getInstanceId());
        if (Boolean.TRUE.equals(processEvent.getSubmit())) {
            entity.setStatus(BusinessStatusEnum.WAITING.getStatus());
            log.info("领用申请提交");
        }
        baseMapper.updateById(entity);
        // 流程完成时自动扣减库存（防重复累加）
        if (BusinessStatusEnum.FINISH.getStatus().equals(entity.getStatus())
            && !BusinessStatusEnum.FINISH.getStatus().equals(oldStatus)) {
            stockOutOnFinish(entity);
        }
    }

    /**
     * 流程完成自动出库：扣减库存 + 写出库流水
     */
    private void stockOutOnFinish(PmsIssueRequest entity) {
        PmsWarehouseStock stock = entity.getStockId() == null ? null : warehouseStockMapper.selectById(entity.getStockId());
        if (stock == null) {
            throw new ServiceException("关联的仓库物料不存在，无法出库");
        }
        BigDecimal qty = entity.getQtyRequested() == null ? BigDecimal.ZERO : entity.getQtyRequested();
        BigDecimal available = stock.getQtyAvailable() == null ? BigDecimal.ZERO : stock.getQtyAvailable();
        if (qty.compareTo(available) > 0) {
            throw new ServiceException("领用数量超过库存（当前 " + available.stripTrailingZeros().toPlainString() + "）");
        }
        stock.setQtyAvailable(available.subtract(qty));
        warehouseStockMapper.updateById(stock);
        PmsStockMovement movement = new PmsStockMovement();
        movement.setStockId(stock.getId());
        movement.setMovementType("out");
        movement.setQty(qty);
        movement.setRelateId(entity.getId());
        movement.setRelateType("issue");
        movement.setOperateTime(new Date());
        stockMovementMapper.insert(movement);
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public Boolean saveBatch(List<PmsIssueRequest> list) {
        return baseMapper.insertBatch(list);
    }
}
