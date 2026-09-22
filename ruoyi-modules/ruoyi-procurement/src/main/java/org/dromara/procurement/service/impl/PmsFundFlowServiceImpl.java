package org.dromara.procurement.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.procurement.domain.PmsFundFlow;
import org.dromara.procurement.domain.PmsProcurementRequest;
import org.dromara.procurement.domain.PmsProject;
import org.dromara.procurement.domain.bo.PmsFundFlowBo;
import org.dromara.procurement.domain.bo.PmsFundStatusBo;
import org.dromara.procurement.domain.vo.PmsFundFlowVo;
import org.dromara.procurement.domain.vo.PmsFundStatusBoardVo;
import org.dromara.procurement.domain.vo.PmsFundSummaryVo;
import org.dromara.procurement.enums.PmsFundStatusEnum;
import org.dromara.procurement.mapper.PmsFundFlowMapper;
import org.dromara.procurement.mapper.PmsProcurementRequestMapper;
import org.dromara.procurement.mapper.PmsProjectMapper;
import org.dromara.procurement.service.IPmsFundFlowService;
import org.dromara.procurement.service.IPmsReserveAccountService;
import org.dromara.system.domain.SysUser;
import org.dromara.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资金流水Service业务层处理
 *
 * @author procurement
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PmsFundFlowServiceImpl implements IPmsFundFlowService {

    private final PmsFundFlowMapper baseMapper;
    private final PmsProjectMapper projectMapper;
    private final PmsProcurementRequestMapper requestMapper;
    private final IPmsReserveAccountService reserveAccountService;
    private final SysUserMapper userMapper;

    @Override
    public PmsFundFlowVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public PageResult<PmsFundFlowVo> queryPageList(PmsFundFlowBo bo, PageQuery pageQuery) {
        Page<PmsFundFlowVo> page = baseMapper.selectVoPage(pageQuery.build(), buildWrapper(bo));
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public List<PmsFundFlowVo> queryList(PmsFundFlowBo bo) {
        return baseMapper.selectVoList(buildWrapper(bo));
    }

    @Override
    public List<PmsFundFlowVo> queryExportList(PmsFundFlowBo bo) {
        LambdaQueryWrapper<PmsFundFlow> wrapper = buildWrapper(bo);
        wrapper.orderByDesc(PmsFundFlow::getOccurDate);
        wrapper.last("limit 5000");
        return baseMapper.selectVoList(wrapper);
    }

    /**
     * 构造查询条件
     */
    private LambdaQueryWrapper<PmsFundFlow> buildWrapper(PmsFundFlowBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<PmsFundFlow> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(bo.getProjectId() != null, PmsFundFlow::getProjectId, bo.getProjectId());
        wrapper.eq(org.dromara.common.core.utils.StringUtils.isNotBlank(bo.getFlowType()), PmsFundFlow::getFlowType, bo.getFlowType());
        // 采购方式（自购/对公）：分账筛选的基础
        wrapper.eq(org.dromara.common.core.utils.StringUtils.isNotBlank(bo.getTitleType()), PmsFundFlow::getTitleType, bo.getTitleType());
        // 申请人：ID 精确 / 姓名模糊
        wrapper.eq(bo.getApplicantId() != null, PmsFundFlow::getApplicantId, bo.getApplicantId());
        wrapper.like(org.dromara.common.core.utils.StringUtils.isNotBlank(bo.getApplicantName()),
            PmsFundFlow::getApplicantName, bo.getApplicantName());
        // 关键字：申请标题/编号模糊
        if (org.dromara.common.core.utils.StringUtils.isNotBlank(bo.getRequestTitle())) {
            wrapper.and(w -> w
                .like(PmsFundFlow::getRequestTitle, bo.getRequestTitle())
                .or().like(PmsFundFlow::getRequestCode, bo.getRequestTitle()));
        }
        // 日期范围
        if (params.get("beginDate") != null && params.get("endDate") != null) {
            wrapper.between(PmsFundFlow::getOccurDate, params.get("beginDate"), params.get("endDate"));
        }
        wrapper.orderByDesc(PmsFundFlow::getOccurDate).orderByDesc(PmsFundFlow::getId);
        return wrapper;
    }

    /**
     * 资金汇总：总预算/已用/剩余 + 本月流出 + 按项目维度
     * <p>
     * 已用金额以 pms_procurement_request 中 status='finish' 的采购申请汇总为准，
     * 不再直接依赖 pms_project.used_amount，避免数据不同源。
     */
    @Override
    public PmsFundSummaryVo summary(Long projectId) {
        PmsFundSummaryVo summary = new PmsFundSummaryVo();

        // 项目维度
        List<PmsProject> projects = projectMapper.selectList(
            Wrappers.<PmsProject>lambdaQuery()
                .eq(projectId != null, PmsProject::getId, projectId)
                .orderByAsc(PmsProject::getId));

        // 动态计算各项目已用金额（数据源：已审批通过的采购申请）
        Map<Long, BigDecimal> usedAmountMap = calcUsedAmountByProject(projectId);

        List<PmsFundSummaryVo.PmsFundProjectSummaryVo> projectSummaries = new ArrayList<>();
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalUsed = BigDecimal.ZERO;
        for (PmsProject p : projects) {
            PmsFundSummaryVo.PmsFundProjectSummaryVo ps = new PmsFundSummaryVo.PmsFundProjectSummaryVo();
            ps.setProjectId(p.getId());
            ps.setProjectName(p.getProjectName());
            BigDecimal budget = nvl(p.getBudget());
            BigDecimal used = nvl(usedAmountMap.get(p.getId()));
            ps.setBudget(budget);
            ps.setUsed(used);
            ps.setRemaining(budget.subtract(used));
            projectSummaries.add(ps);
            totalBudget = totalBudget.add(budget);
            totalUsed = totalUsed.add(used);
        }

        // 本月流出（来自流水，按项目归集）
        YearMonth ym = YearMonth.now();
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);
        List<PmsFundFlow> monthFlows = baseMapper.selectList(
            Wrappers.<PmsFundFlow>lambdaQuery()
                .eq(projectId != null, PmsFundFlow::getProjectId, projectId)
                .eq(PmsFundFlow::getFlowType, "out")
                .between(PmsFundFlow::getCreateTime, monthStart, monthEnd));

        BigDecimal monthOut = BigDecimal.ZERO;
        Map<Long, List<PmsFundFlow>> flowGroup = monthFlows.stream()
            .collect(Collectors.groupingBy(PmsFundFlow::getProjectId));
        for (PmsFundFlow f : monthFlows) {
            monthOut = monthOut.add(nvl(f.getAmount()));
        }
        summary.setMonthOut(monthOut);
        summary.setMonthOutCount((long) monthFlows.size());

        // 按项目回填本月流出
        for (PmsFundSummaryVo.PmsFundProjectSummaryVo ps : projectSummaries) {
            List<PmsFundFlow> flows = flowGroup.get(ps.getProjectId());
            BigDecimal po = BigDecimal.ZERO;
            if (CollUtil.isNotEmpty(flows)) {
                for (PmsFundFlow f : flows) {
                    po = po.add(nvl(f.getAmount()));
                }
            }
            ps.setMonthOut(po);
            ps.setMonthOutCount(flows == null ? 0L : (long) flows.size());
        }

        summary.setTotalBudget(totalBudget);
        summary.setTotalUsed(totalUsed);
        summary.setTotalRemaining(totalBudget.subtract(totalUsed));
        summary.setProjects(projectSummaries);
        // 第二本账：备用金（额度/占用/可用/回笼），与项目账本互不影响
        summary.setReserve(reserveAccountService.summary());
        return summary;
    }

    /**
     * 根据已审批通过的采购申请，按项目汇总已用金额
     */
    private Map<Long, BigDecimal> calcUsedAmountByProject(Long projectId) {
        LambdaQueryWrapper<PmsProcurementRequest> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PmsProcurementRequest::getStatus, "finish");
        wrapper.eq(PmsProcurementRequest::getDelFlag, 0L);
        wrapper.isNotNull(PmsProcurementRequest::getProjectId);
        wrapper.isNotNull(PmsProcurementRequest::getAmount);
        wrapper.eq(projectId != null, PmsProcurementRequest::getProjectId, projectId);
        List<PmsProcurementRequest> requests = requestMapper.selectList(wrapper);
        return requests.stream()
            .collect(Collectors.groupingBy(PmsProcurementRequest::getProjectId,
                Collectors.mapping(r -> nvl(r.getAmount()), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    }

    /**
     * 资金同步：根据当前所有 status='finish' 的采购申请，重建项目已用金额和资金流水。
     * 用于修复历史不一致数据或测试数据清理后的兜底重算。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromRequests() {
        log.info("开始资金同步：根据采购申请表重建 used_amount 和 fund_flow");

        // 1. 清空现有资金流水（只清 out 类型）
        baseMapper.delete(Wrappers.<PmsFundFlow>lambdaQuery().eq(PmsFundFlow::getFlowType, "out"));

        // 2. 重置所有项目已用金额为 0
        PmsProject resetProject = new PmsProject();
        resetProject.setUsedAmount(BigDecimal.ZERO);
        projectMapper.update(resetProject, Wrappers.<PmsProject>lambdaQuery().isNotNull(PmsProject::getId));

        // 3. 查询所有 finish 且未删除的采购申请
        List<PmsProcurementRequest> requests = requestMapper.selectList(
            Wrappers.<PmsProcurementRequest>lambdaQuery()
                .eq(PmsProcurementRequest::getStatus, "finish")
                .eq(PmsProcurementRequest::getDelFlag, 0L)
                .isNotNull(PmsProcurementRequest::getProjectId)
                .isNotNull(PmsProcurementRequest::getAmount)
                .orderByAsc(PmsProcurementRequest::getId));

        // 4. 按项目累计已用金额并生成流水
        Map<Long, PmsProject> projectCache = new java.util.HashMap<>();
        int count = 0;
        for (PmsProcurementRequest request : requests) {
            PmsProject project = projectCache.computeIfAbsent(request.getProjectId(), projectMapper::selectById);
            if (project == null) {
                continue;
            }
            // 累计已用金额
            BigDecimal used = nvl(project.getUsedAmount());
            project.setUsedAmount(used.add(nvl(request.getAmount())));
            projectMapper.updateById(project);

            // 生成资金流水
            PmsFundFlow flow = new PmsFundFlow();
            flow.setFlowNo(generateFlowNo());
            flow.setFlowType("out");
            flow.setProjectId(project.getId());
            flow.setProjectName(project.getProjectName());
            flow.setRequestId(request.getId());
            flow.setRequestCode(request.getRequestCode());
            flow.setRequestTitle(request.getTitle());
            flow.setAmount(nvl(request.getAmount()));
            // 以采购申请创建日期作为流水发生日期（不存在则取当天）
            flow.setOccurDate(request.getCreateTime() == null ? LocalDate.now()
                : request.getCreateTime().toLocalDate());
            flow.setOperatorId(project.getLeaderId());
            flow.setOperatorName("系统同步");
            flow.setRemark("根据采购申请表 status=finish 自动同步");
            baseMapper.insert(flow);
            count++;
        }
        log.info("资金同步完成：共处理 {} 条采购申请", count);
    }

    /**
     * 资金状态看板：固定 4 行（含 0 值行），label 取枚举中文名，顺序即状态机推进顺序
     */
    @Override
    public List<PmsFundStatusBoardVo> statusBoard() {
        Map<String, PmsFundStatusBoardVo> statMap = new LinkedHashMap<>();
        for (PmsFundStatusBoardVo row : requestMapper.selectFundStatusBoard()) {
            if (row.getStatus() != null) {
                statMap.put(row.getStatus(), row);
            }
        }
        List<PmsFundStatusBoardVo> board = new ArrayList<>();
        for (PmsFundStatusEnum statusEnum : PmsFundStatusEnum.values()) {
            PmsFundStatusBoardVo row = statMap.get(statusEnum.getStatus());
            if (row == null) {
                row = new PmsFundStatusBoardVo();
                row.setStatus(statusEnum.getStatus());
                row.setCount(0L);
                row.setAmount(BigDecimal.ZERO);
            }
            row.setLabel(statusEnum.getDesc());
            board.add(row);
        }
        return board;
    }

    /**
     * 批量推进资金状态（单向不可回溯）
     * <p>
     * 业务要求：一旦标记报销就不能回退，因此这里只做「向前推进」校验，
     * 不合法的申请跳过并在提示里说明原因（不整批失败，避免一笔脏数据卡住整批操作）。
     * 每次变更都写操作人 + 时间留痕（不可回溯 ⇒ 必须能查谁改的）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String changeFundStatus(PmsFundStatusBo bo) {
        PmsFundStatusEnum target;
        if ("reimburse".equals(bo.getAction())) {
            target = PmsFundStatusEnum.REIMBURSED_UNPAID;
        } else if ("paid".equals(bo.getAction())) {
            target = PmsFundStatusEnum.REIMBURSED_PAID;
        } else {
            throw new ServiceException("不支持的操作类型：" + bo.getAction());
        }
        List<PmsProcurementRequest> requests = requestMapper.selectList(
            Wrappers.<PmsProcurementRequest>lambdaQuery().in(PmsProcurementRequest::getId, bo.getIds()));
        if (CollUtil.isEmpty(requests)) {
            throw new ServiceException("采购申请不存在");
        }

        Long operatorId = LoginHelper.getUserId();
        String operatorName = resolveNickName(operatorId);
        LocalDateTime now = LocalDateTime.now();

        int success = 0;
        Map<String, Integer> skipReasons = new LinkedHashMap<>();
        for (PmsProcurementRequest request : requests) {
            String current = request.getFundStatus();
            if (!PmsFundStatusEnum.canTransfer(current, target.getStatus())) {
                skipReasons.merge(skipReason(current, target), 1, Integer::sum);
                continue;
            }
            PmsProcurementRequest update = new PmsProcurementRequest();
            update.setId(request.getId());
            update.setFundStatus(target.getStatus());
            if (target == PmsFundStatusEnum.REIMBURSED_UNPAID) {
                update.setReimburseDate(now);
                update.setReimburseBy(operatorId);
                update.setReimburseByName(operatorName);
            } else {
                update.setPaidDate(now);
                update.setPaidBy(operatorId);
                update.setPaidByName(operatorName);
                // 允许跳级：从「已采购未报销」直接确认汇款时，报销留痕一并补齐
                if (PmsFundStatusEnum.PURCHASED_UNREIMBURSED.getStatus().equals(current)) {
                    update.setReimburseDate(now);
                    update.setReimburseBy(operatorId);
                    update.setReimburseByName(operatorName);
                }
            }
            requestMapper.updateById(update);
            success++;
            log.info("资金状态推进：申请[{}] {} -> {}，操作人[{}]",
                request.getRequestCode(), current, target.getStatus(), operatorName);
        }

        StringBuilder msg = new StringBuilder("成功 ").append(success).append(" 笔");
        if (!skipReasons.isEmpty()) {
            int skipTotal = skipReasons.values().stream().mapToInt(Integer::intValue).sum();
            msg.append("，跳过 ").append(skipTotal).append(" 笔（");
            msg.append(skipReasons.entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue() + " 笔")
                .collect(Collectors.joining("；")));
            msg.append("）");
        }
        if (success == 0) {
            throw new ServiceException(msg.toString());
        }
        return msg.toString();
    }

    /**
     * 跳过原因（给用户看的中文提示）
     */
    private String skipReason(String current, PmsFundStatusEnum target) {
        if (org.dromara.common.core.utils.StringUtils.isBlank(current)) {
            return "未进入资金状态（申请未审批通过）";
        }
        if (PmsFundStatusEnum.NOT_APPLICABLE.getStatus().equals(current)) {
            return "对公申请不适用报销";
        }
        if (PmsFundStatusEnum.REIMBURSED_PAID.getStatus().equals(current)) {
            return "已汇款完成（终态）";
        }
        if (target.getStatus().equals(current)) {
            return "已是" + target.getDesc();
        }
        return "状态不允许该操作";
    }

    /**
     * 取操作人昵称快照（取不到时退回登录名）
     */
    private String resolveNickName(Long userId) {
        if (userId == null) {
            return LoginHelper.getUsername();
        }
        SysUser user = userMapper.selectById(userId);
        return user != null && org.dromara.common.core.utils.StringUtils.isNotBlank(user.getNickName())
            ? user.getNickName() : LoginHelper.getUsername();
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 生成流水编号 FUND-yyyyMMdd-NNN（按天计数）
     */
    public String generateFlowNo() {
        String prefix = "FUND-" + LocalDate.now().toString().replace("-", "") + "-";
        LambdaQueryWrapper<PmsFundFlow> lqw = Wrappers.lambdaQuery();
        lqw.likeRight(PmsFundFlow::getFlowNo, prefix);
        long count = baseMapper.selectCount(lqw);
        return prefix + String.format("%03d", count + 1);
    }
}
