package org.dromara.procurement.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.procurement.domain.PmsAcceptance;
import org.dromara.procurement.domain.PmsAcceptanceItem;
import org.dromara.procurement.domain.PmsProcurementRequest;
import org.dromara.procurement.domain.PmsProcurementRequestItem;
import org.dromara.procurement.domain.PmsProject;
import org.dromara.procurement.domain.PmsStockMovement;
import org.dromara.procurement.domain.PmsWarehouseStock;
import org.dromara.procurement.domain.bo.PmsAcceptanceBo;
import org.dromara.procurement.domain.bo.PmsAcceptanceItemBo;
import org.dromara.procurement.domain.vo.PmsAcceptanceItemVo;
import org.dromara.procurement.domain.vo.PmsAcceptanceVo;
import org.dromara.procurement.mapper.PmsAcceptanceItemMapper;
import org.dromara.procurement.mapper.PmsAcceptanceMapper;
import org.dromara.procurement.mapper.PmsFlowApproverMapper;
import org.dromara.procurement.mapper.PmsProcurementRequestItemMapper;
import org.dromara.procurement.mapper.PmsProcurementRequestMapper;
import org.dromara.procurement.mapper.PmsProjectMapper;
import org.dromara.procurement.mapper.PmsStockMovementMapper;
import org.dromara.procurement.mapper.PmsWarehouseStockMapper;
import org.dromara.procurement.service.IPmsAcceptanceService;
import org.dromara.workflow.api.WorkflowService;
import org.dromara.workflow.api.domain.StartProcessDTO;
import org.dromara.workflow.api.event.ProcessEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购管理-采购验收单Service业务层处理
 *
 * @author procurement
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PmsAcceptanceServiceImpl implements IPmsAcceptanceService {

    private final PmsAcceptanceMapper baseMapper;
    private final PmsAcceptanceItemMapper itemMapper;
    private final PmsProcurementRequestItemMapper requestItemMapper;
    private final PmsProcurementRequestMapper requestMapper;
    private final PmsProjectMapper projectMapper;
    private final PmsWarehouseStockMapper warehouseStockMapper;
    private final PmsStockMovementMapper stockMovementMapper;
    private final PmsFlowApproverMapper flowApproverMapper;
    private final WorkflowService workflowService;

    /**
     * 填充当前审批人（从流程 waiting 任务动态查询）
     */
    private void fillCurrentApprover(List<PmsAcceptanceVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (PmsAcceptanceVo vo : list) {
            if (vo.getProcessInstanceId() != null) {
                List<String> names = flowApproverMapper.selectCurrentApproverNames(vo.getProcessInstanceId());
                if (CollUtil.isNotEmpty(names)) {
                    vo.setCurrentApprover(String.join("、", names));
                }
            }
        }
    }

    @Override
    public PmsAcceptanceVo queryById(Long id) {
        PmsAcceptanceVo vo = baseMapper.selectVoById(id);
        if (vo != null) {
            fillRequestProject(vo);
            vo.setItems(queryItems(id));
            if (vo.getProcessInstanceId() != null) {
                List<String> names = flowApproverMapper.selectCurrentApproverNames(vo.getProcessInstanceId());
                if (CollUtil.isNotEmpty(names)) {
                    vo.setCurrentApprover(String.join("、", names));
                }
            }
        }
        return vo;
    }

    /**
     * 带出关联采购申请的编号与项目名称
     */
    private void fillRequestProject(PmsAcceptanceVo vo) {
        if (vo.getRequestId() == null) {
            return;
        }
        PmsProcurementRequest request = requestMapper.selectById(vo.getRequestId());
        if (request != null) {
            vo.setRequestCode(request.getRequestCode());
            vo.setRequestTitle(request.getTitle());
            if (request.getProjectId() != null) {
                PmsProject project = projectMapper.selectById(request.getProjectId());
                if (project != null) {
                    vo.setProjectName(project.getProjectName());
                }
            }
        }
    }

    /**
     * 查询验收单明细（带出关联采购申请明细的 sourceItemId，供前端回显）
     */
    private List<PmsAcceptanceItemVo> queryItems(Long acceptanceId) {
        List<PmsAcceptanceItem> list = itemMapper.selectList(
            Wrappers.<PmsAcceptanceItem>lambdaQuery()
                .eq(PmsAcceptanceItem::getAcceptanceId, acceptanceId)
                .orderByAsc(PmsAcceptanceItem::getId));
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        List<PmsAcceptanceItemVo> vos = new ArrayList<>(list.size());
        for (PmsAcceptanceItem item : list) {
            PmsAcceptanceItemVo vo = MapstructUtils.convert(item, PmsAcceptanceItemVo.class);
            if (vo != null) {
                // 回显时前端用 sourceItemId 表示关联的采购申请明细ID
                vo.setSourceItemId(item.getRequestItemId());
                vos.add(vo);
            }
        }
        return vos;
    }

    @Override
    public PageResult<PmsAcceptanceVo> queryPageList(PmsAcceptanceBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsAcceptance> lqw = buildQueryWrapper(bo);
        Page<PmsAcceptanceVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        for (PmsAcceptanceVo vo : result.getRecords()) {
            fillRequestProject(vo);
        }
        fillCurrentApprover(result.getRecords());
        return PageResult.build(result.getRecords(), result.getTotal());
    }

    @Override
    public List<PmsAcceptanceVo> queryList(PmsAcceptanceBo bo) {
        List<PmsAcceptanceVo> list = baseMapper.selectVoList(buildQueryWrapper(bo));
        for (PmsAcceptanceVo vo : list) {
            fillRequestProject(vo);
        }
        fillCurrentApprover(list);
        return list;
    }

    private LambdaQueryWrapper<PmsAcceptance> buildQueryWrapper(PmsAcceptanceBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsAcceptance> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getAcceptanceCode()), PmsAcceptance::getAcceptanceCode, bo.getAcceptanceCode());
        // 按采购申请ID过滤
        lqw.eq(bo.getRequestId() != null, PmsAcceptance::getRequestId, bo.getRequestId());
        lqw.eq(bo.getProjectId() != null, PmsAcceptance::getProjectId, bo.getProjectId());
        lqw.like(StringUtils.isNotBlank(bo.getOperator()), PmsAcceptance::getOperator, bo.getOperator());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), PmsAcceptance::getStatus, bo.getStatus());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            PmsAcceptance::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        lqw.orderByDesc(PmsAcceptance::getCreateTime);
        return lqw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(PmsAcceptanceBo bo) {
        if (StringUtils.isBlank(bo.getAcceptanceCode())) {
            bo.setAcceptanceCode(generateAcceptanceCode());
        }
        if (StringUtils.isBlank(bo.getOperator())) {
            bo.setOperator(LoginHelper.getUsername());
        }
        if (StringUtils.isBlank(bo.getStatus())) {
            bo.setStatus("pending");
        }
        PmsAcceptance add = MapstructUtils.convert(bo, PmsAcceptance.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
            saveItems(add.getId(), bo.getItems());
            // 新建验收单即标记关联采购申请为「验收中」（防重复验收）
            updateAcceptanceFlag(add, "processing");
        }
        return flag;
    }

    /**
     * 生成验收编号 acpt-yyyyMMdd-NNN
     */
    private String generateAcceptanceCode() {
        String prefix = "acpt-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        LambdaQueryWrapper<PmsAcceptance> lqw = Wrappers.lambdaQuery();
        lqw.likeRight(PmsAcceptance::getAcceptanceCode, prefix);
        long count = baseMapper.selectCount(lqw);
        return prefix + String.format("%03d", count + 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(PmsAcceptanceBo bo) {
        PmsAcceptance update = MapstructUtils.convert(bo, PmsAcceptance.class);
        boolean flag = baseMapper.updateById(update) > 0;
        if (flag) {
            // 编辑时重建明细：先删后插，保证与前端提交一致
            itemMapper.delete(Wrappers.<PmsAcceptanceItem>lambdaQuery()
                .eq(PmsAcceptanceItem::getAcceptanceId, bo.getId()));
            saveItems(bo.getId(), bo.getItems());
        }
        return flag;
    }

    /**
     * 提交验收并启动流程：验收人→采购申请人→项目负责人→团队上级→结束
     *
     * @param bo 验收单
     * @return 验收单详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PmsAcceptanceVo submitAndStartFlow(PmsAcceptanceBo bo) {
        // 防重复验收：同一采购申请已存在其他验收单则拒绝
        if (bo.getRequestId() != null) {
            Long existCount = baseMapper.selectCount(Wrappers.<PmsAcceptance>lambdaQuery()
                .eq(PmsAcceptance::getRequestId, bo.getRequestId())
                .ne(bo.getId() != null, PmsAcceptance::getId, bo.getId()));
            if (existCount > 0) {
                throw new ServiceException("该采购申请已创建验收单，请勿重复验收");
            }
        }
        if (ObjectUtil.isNull(bo.getId())) {
            insertByBo(bo);
        } else {
            PmsAcceptance exist = baseMapper.selectById(bo.getId());
            if (ObjectUtil.isNull(exist)) {
                throw new ServiceException("验收单不存在");
            }
            if (StringUtils.isNotBlank(exist.getStatus()) && "finished".equals(exist.getStatus())) {
                throw new ServiceException("验收已完成，不能重复提交");
            }
            if (StringUtils.isNotBlank(exist.getStatus()) && "waiting".equals(exist.getStatus())) {
                throw new ServiceException("该验收单已在审批流程中，请勿重复提交");
            }
            updateByBo(bo);
        }

        PmsAcceptance acceptance = baseMapper.selectById(bo.getId());
        // 取关联采购申请：采购申请人 = 采购申请的 create_by（动态，非固定角色）
        if (acceptance.getRequestId() == null) {
            throw new ServiceException("请先选择关联采购申请");
        }
        PmsProcurementRequest request = requestMapper.selectById(acceptance.getRequestId());
        if (ObjectUtil.isNull(request)) {
            throw new ServiceException("关联采购申请不存在");
        }
        if (request.getCreateBy() == null) {
            throw new ServiceException("采购申请缺少申请人信息，无法启动流程");
        }
        // 项目负责人：验收单或关联申请的项目
        Long projectId = acceptance.getProjectId() != null ? acceptance.getProjectId() : request.getProjectId();
        PmsProject project = projectMapper.selectById(projectId);
        if (ObjectUtil.isNull(project) || project.getLeaderId() == null) {
            throw new ServiceException("请先为项目配置负责人（用户）");
        }

        StartProcessDTO startProcess = new StartProcessDTO();
        startProcess.setBusinessId(acceptance.getId().toString());
        startProcess.setFlowCode("pms_acceptance");
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicantId", request.getCreateBy().toString());
        variables.put("leaderId", project.getLeaderId().toString());
        startProcess.setVariables(variables);
        // 启动流程 + 提交首个（验收发起人）节点，由 processHandler 推进
        boolean started = workflowService.startCompleteTask(startProcess);
        if (!started) {
            throw new ServiceException("流程发起失败");
        }
        acceptance.setStatus(BusinessStatusEnum.WAITING.getStatus());
        baseMapper.updateById(acceptance);
        return queryById(acceptance.getId());
    }

    /**
     * 验收流程总体监听：同步状态；流程完成时自动入库
     */
    @EventListener(condition = "#processEvent.flowCode.startsWith('pms_acceptance')")
    public void processHandler(ProcessEvent processEvent) {
        log.info("采购验收流程执行了{}", processEvent);
        PmsAcceptance acceptance = baseMapper.selectById(Convert.toLong(processEvent.getBusinessId()));
        if (ObjectUtil.isNull(acceptance)) {
            return;
        }
        String oldStatus = acceptance.getStatus();
        acceptance.setStatus(processEvent.getStatus());
        acceptance.setProcessInstanceId(processEvent.getInstanceId());
        if (Boolean.TRUE.equals(processEvent.getSubmit())) {
            acceptance.setStatus(BusinessStatusEnum.WAITING.getStatus());
            log.info("采购验收提交");
        }
        baseMapper.updateById(acceptance);
        // 流程完成时：置验收标志 done + 自动入库（防重复累加）
        if (BusinessStatusEnum.FINISH.getStatus().equals(acceptance.getStatus())
            && !BusinessStatusEnum.FINISH.getStatus().equals(oldStatus)) {
            updateAcceptanceFlag(acceptance, "done");
            stockInOnFinish(acceptance);
        }
    }

    /**
     * 保存验收明细（sourceItemId → requestItemId 映射，并自动核对金额）
     */
    private void saveItems(Long acceptanceId, List<PmsAcceptanceItemBo> itemBos) {
        if (CollUtil.isEmpty(itemBos)) {
            return;
        }
        List<PmsAcceptanceItem> items = new ArrayList<>(itemBos.size());
        for (PmsAcceptanceItemBo itemBo : itemBos) {
            PmsAcceptanceItem item = MapstructUtils.convert(itemBo, PmsAcceptanceItem.class);
            if (item == null) {
                continue;
            }
            // 先删后插重建明细：清空回传 id，重新生成主键，避免主键冲突
            item.setId(null);
            item.setAcceptanceId(acceptanceId);
            // 前端带出用 sourceItemId 承载关联采购申请明细，保存时映射到 requestItemId
            if (item.getRequestItemId() == null) {
                item.setRequestItemId(itemBo.getSourceItemId());
            }
            // 金额核对：发票金额 ≤ 申请单价 → pass；超出 → over 冲红
            BigDecimal invoice = item.getInvoicePrice() == null ? BigDecimal.ZERO : item.getInvoicePrice();
            BigDecimal apply = item.getApplyPrice() == null ? BigDecimal.ZERO : item.getApplyPrice();
            String check = invoice.compareTo(apply) > 0 ? "over" : "pass";
            item.setPriceCheck(check);
            item.setResult(check);
            items.add(item);
        }
        if (CollUtil.isNotEmpty(items)) {
            itemMapper.insertBatch(items);
        }
    }

    /**
     * 验收完成自动入库：按品名+规格合并数量写入仓库，并记录入库流水
     */
    private void stockInOnFinish(PmsAcceptance acceptance) {
        List<PmsAcceptanceItem> items = itemMapper.selectList(
            Wrappers.<PmsAcceptanceItem>lambdaQuery().eq(PmsAcceptanceItem::getAcceptanceId, acceptance.getId()));
        for (PmsAcceptanceItem item : items) {
            BigDecimal qty = BigDecimal.ZERO;
            String brand = null;
            String unit = null;
            if (item.getRequestItemId() != null) {
                PmsProcurementRequestItem reqItem = requestItemMapper.selectById(item.getRequestItemId());
                if (reqItem != null) {
                    qty = reqItem.getQuantity() == null ? BigDecimal.ZERO : reqItem.getQuantity();
                    brand = reqItem.getBrand();
                    unit = reqItem.getUnit();
                }
            }
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            // 按品名+规格合并：找到则累加，找不到则新建
            PmsWarehouseStock stock = warehouseStockMapper.selectOne(
                Wrappers.<PmsWarehouseStock>lambdaQuery()
                    .eq(PmsWarehouseStock::getItemName, item.getItemName())
                    .eq(PmsWarehouseStock::getSpec, item.getSpec())
                    .last("limit 1"));
            if (stock == null) {
                stock = new PmsWarehouseStock();
                stock.setItemName(item.getItemName());
                stock.setSpec(item.getSpec());
                stock.setBrand(brand);
                stock.setUnit(unit);
                stock.setQtyAvailable(BigDecimal.ZERO);
                stock.setProjectId(acceptance.getProjectId());
                stock.setSourceItemId(item.getRequestItemId());
                stock.setInboundDate(new Date());
                warehouseStockMapper.insert(stock);
            }
            stock.setQtyAvailable(stock.getQtyAvailable().add(qty));
            warehouseStockMapper.updateById(stock);
            // 写入库流水
            PmsStockMovement movement = new PmsStockMovement();
            movement.setStockId(stock.getId());
            movement.setMovementType("in");
            movement.setQty(qty);
            movement.setRelateId(acceptance.getId());
            movement.setRelateType("acceptance");
            movement.setOperateTime(new Date());
            stockMovementMapper.insert(movement);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (CollUtil.isNotEmpty(ids)) {
            List<PmsAcceptance> list = baseMapper.selectByIds(ids);
            itemMapper.delete(Wrappers.<PmsAcceptanceItem>lambdaQuery()
                .in(PmsAcceptanceItem::getAcceptanceId, ids));
            boolean flag = baseMapper.deleteByIds(ids) > 0;
            // 删除验收单后：若该采购申请已无其他验收单，恢复验收标志为 none
            if (flag && CollUtil.isNotEmpty(list)) {
                for (PmsAcceptance acc : list) {
                    if (acc.getRequestId() == null) {
                        continue;
                    }
                    Long remain = baseMapper.selectCount(Wrappers.<PmsAcceptance>lambdaQuery()
                        .eq(PmsAcceptance::getRequestId, acc.getRequestId()));
                    if (remain == 0) {
                        updateAcceptanceFlag(acc, "none");
                    }
                }
            }
            return flag;
        }
        return false;
    }

    /**
     * 更新关联采购申请的验收标志
     */
    private void updateAcceptanceFlag(PmsAcceptance acceptance, String flag) {
        if (acceptance == null || acceptance.getRequestId() == null) {
            return;
        }
        PmsProcurementRequest request = requestMapper.selectById(acceptance.getRequestId());
        if (request != null) {
            PmsProcurementRequest update = new PmsProcurementRequest();
            update.setId(request.getId());
            update.setAcceptanceStatus(flag);
            requestMapper.updateById(update);
        }
    }

    @Override
    public Boolean saveBatch(List<PmsAcceptance> list) {
        return baseMapper.insertBatch(list);
    }
}
