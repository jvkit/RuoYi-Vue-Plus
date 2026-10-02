package org.dromara.procurement.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.procurement.domain.PmsFundFlow;
import org.dromara.procurement.domain.PmsProcurementRequest;
import org.dromara.procurement.domain.PmsProject;
import org.dromara.procurement.domain.bo.PmsFundFlowBo;
import org.dromara.procurement.domain.bo.PmsFundStatusBo;
import org.dromara.procurement.domain.bo.PmsManualFundFlowBo;
import org.dromara.procurement.domain.vo.PmsFundFlowVo;
import org.dromara.procurement.domain.vo.PmsFundStatusBoardVo;
import org.dromara.procurement.domain.vo.PmsFundSummaryVo;
import org.dromara.procurement.enums.PmsFundStatusEnum;
import org.dromara.procurement.mapper.PmsFundFlowMapper;
import org.dromara.procurement.mapper.PmsProcurementRequestMapper;
import org.dromara.procurement.mapper.PmsProjectMapper;
import org.dromara.procurement.service.IPmsFundFlowService;
import org.dromara.procurement.service.IPmsProjectService;
import org.dromara.procurement.service.IPmsReserveAccountService;
import org.dromara.procurement.utils.PmsFundSplitUtil;
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
    private final IPmsProjectService projectService;
    private final SysUserMapper userMapper;

    /**
     * 采购方式：自购（走备用金）
     */
    private static final String TITLE_TYPE_SELF = "自购";

    /**
     * 采购方式：对公（直支项目资金，与备用金无关）
     */
    private static final String TITLE_TYPE_PUBLIC = "对公";

    /**
     * 人工流水默认备注
     */
    private static final String DEFAULT_MANUAL_REMARK = "非采购订单资金消耗";

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
        // 流水编号：模糊筛选
        wrapper.like(org.dromara.common.core.utils.StringUtils.isNotBlank(bo.getFlowNo()), PmsFundFlow::getFlowNo, bo.getFlowNo());
        // 采购方式（自购/对公）：分账筛选的基础
        wrapper.eq(org.dromara.common.core.utils.StringUtils.isNotBlank(bo.getTitleType()), PmsFundFlow::getTitleType, bo.getTitleType());
        // 资金状态（仅人工备用金流水有值）
        wrapper.eq(org.dromara.common.core.utils.StringUtils.isNotBlank(bo.getFundStatus()), PmsFundFlow::getFundStatus, bo.getFundStatus());
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
     * 按项目汇总已用金额（数据源：全部 out 资金流水，含采购审批流水与人工登记流水）。
     * 备用金（自购）支出同样扣减项目资金，必须与项目 used_amount 记账口径一致。
     */
    private Map<Long, BigDecimal> calcUsedAmountByProject(Long projectId) {
        LambdaQueryWrapper<PmsFundFlow> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PmsFundFlow::getFlowType, "out");
        wrapper.eq(PmsFundFlow::getDelFlag, 0L);
        wrapper.isNotNull(PmsFundFlow::getProjectId);
        wrapper.eq(projectId != null, PmsFundFlow::getProjectId, projectId);
        List<PmsFundFlow> flows = baseMapper.selectList(wrapper);
        return flows.stream()
            .collect(Collectors.groupingBy(PmsFundFlow::getProjectId,
                Collectors.mapping(f -> nvl(f.getAmount()), Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
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
        // 5. 全量重算父级项目金额（预算/已用 = 直接子级之和）
        projectService.recomputeAllParentAmounts();
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
     * 人工登记资金流水（非采购订单的资金消耗）
     * <p>
     * 自购：按 payers 顺序拆账（同审批拆账规则），每人一条流水，fund_status=已采购未报销；
     * 对公：一条流水，fund_status=null。项目 used_amount 按总额一次累加并同步父级（事务内完成）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PmsFundFlowVo> createManualFlow(PmsManualFundFlowBo bo) {
        String titleType = bo.getTitleType() == null ? "" : bo.getTitleType().trim();
        boolean selfPurchase = TITLE_TYPE_SELF.equals(titleType);
        if (!selfPurchase && !TITLE_TYPE_PUBLIC.equals(titleType)) {
            throw new ServiceException("采购方式仅支持：" + TITLE_TYPE_SELF + " / " + TITLE_TYPE_PUBLIC);
        }
        BigDecimal amount = nvl(bo.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("金额必须大于 0");
        }
        PmsProject project = projectMapper.selectById(bo.getProjectId());
        if (ObjectUtil.isNull(project)) {
            throw new ServiceException("项目不存在");
        }
        String remark = org.dromara.common.core.utils.StringUtils.isNotBlank(bo.getRemark())
            ? bo.getRemark() : DEFAULT_MANUAL_REMARK;

        List<PmsFundFlow> flows = new ArrayList<>();
        if (selfPurchase) {
            // 备用金按人拆账（顺序即扣款顺序，靠后者兜尾差）
            List<PmsFundSplitUtil.Payer> payers = new ArrayList<>();
            if (CollUtil.isNotEmpty(bo.getPayers())) {
                for (PmsManualFundFlowBo.Payer p : bo.getPayers()) {
                    if (ObjectUtil.isNull(p.getPersonId())) {
                        continue;
                    }
                    String personName = org.dromara.common.core.utils.StringUtils.isNotBlank(p.getPersonName())
                        ? p.getPersonName() : resolveNickName(p.getPersonId());
                    payers.add(new PmsFundSplitUtil.Payer(p.getPersonId(), personName));
                }
            }
            List<PmsFundSplitUtil.Split> splits = PmsFundSplitUtil.split(payers, amount, reserveAccountService::availableAmount);
            for (PmsFundSplitUtil.Split split : splits) {
                PmsFundFlow flow = buildManualBaseFlow(project, remark);
                flow.setTitleType(TITLE_TYPE_SELF);
                flow.setApplicantId(split.getPersonId());
                flow.setApplicantName(split.getPersonName());
                flow.setAmount(split.getAmount());
                // 人工备用金流水：资金状态挂流水自身，初始=已采购未报销
                flow.setFundStatus(PmsFundStatusEnum.PURCHASED_UNREIMBURSED.getStatus());
                flows.add(flow);
            }
        } else {
            // 对公直支：一条流水，不扣备用金、无资金状态
            PmsFundFlow flow = buildManualBaseFlow(project, remark);
            flow.setTitleType(TITLE_TYPE_PUBLIC);
            flow.setAmount(amount);
            flows.add(flow);
        }

        // 项目账本：总金额一次累加（不随拆账重复累加），并同步父级
        project.setUsedAmount(nvl(project.getUsedAmount()).add(amount));
        projectMapper.updateById(project);
        projectService.syncAncestors(project.getId());

        List<PmsFundFlowVo> result = new ArrayList<>(flows.size());
        for (PmsFundFlow flow : flows) {
            flow.setFlowNo(generateFlowNo());
            baseMapper.insert(flow);
            result.add(MapstructUtils.convert(flow, PmsFundFlowVo.class));
        }
        log.info("人工资金流水已登记：项目[{}] 方式[{}] 金额[{}] 共[{}]条",
            project.getProjectName(), titleType, amount, flows.size());
        return result;
    }

    /**
     * 人工流水资金状态推进（仅 request_id 为空 且 title_type=自购 的人工流水可操作）
     * <p>
     * 单向不可回溯、幂等（已是目标状态直接成功）；操作人留痕在 operator_id/operator_name，备注追加流转记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeManualFundStatus(Long id, String fundStatus) {
        PmsFundStatusEnum target = PmsFundStatusEnum.getByStatus(fundStatus);
        if (target != PmsFundStatusEnum.REIMBURSED_UNPAID && target != PmsFundStatusEnum.REIMBURSED_PAID) {
            throw new ServiceException("目标状态仅支持：已报销未汇款 / 已报销已汇款");
        }
        PmsFundFlow flow = baseMapper.selectById(id);
        if (ObjectUtil.isNull(flow)) {
            throw new ServiceException("资金流水不存在");
        }
        if (ObjectUtil.isNotNull(flow.getRequestId())) {
            throw new ServiceException("仅人工登记的流水可手动设置资金状态（采购流水请在采购申请上操作）");
        }
        if (!TITLE_TYPE_SELF.equals(flow.getTitleType())) {
            throw new ServiceException("仅自购（备用金）流水可设置资金状态");
        }
        String current = flow.getFundStatus();
        if (target.getStatus().equals(current)) {
            // 幂等：已是目标状态直接成功
            return;
        }
        if (!PmsFundStatusEnum.canTransfer(current, target.getStatus())) {
            throw new ServiceException("当前状态[" + PmsFundStatusEnum.findByStatus(current)
                + "]不允许推进到[" + target.getDesc() + "]");
        }
        Long operatorId = LoginHelper.getUserId();
        String operatorName = resolveNickName(operatorId);
        LocalDateTime now = LocalDateTime.now();
        PmsFundFlow update = new PmsFundFlow();
        update.setId(id);
        update.setFundStatus(target.getStatus());
        update.setOperatorId(operatorId);
        update.setOperatorName(operatorName);
        String trace = "【" + now + " " + operatorName + " 置为" + target.getDesc() + "】";
        update.setRemark(org.dromara.common.core.utils.StringUtils.isBlank(flow.getRemark())
            ? trace : flow.getRemark() + " " + trace);
        baseMapper.updateById(update);
        log.info("人工流水资金状态推进：流水[{}] {} -> {}，操作人[{}]", flow.getFlowNo(), current, target.getStatus(), operatorName);
    }

    /**
     * 人工流水基础字段：无采购申请（request_id/code 留空）、审批人=无、发生日期=今天
     */
    private PmsFundFlow buildManualBaseFlow(PmsProject project, String remark) {
        PmsFundFlow flow = new PmsFundFlow();
        flow.setFlowType("out");
        flow.setProjectId(project.getId());
        flow.setProjectName(project.getProjectName());
        flow.setOccurDate(LocalDate.now());
        flow.setOperatorName("无");
        flow.setRemark(remark);
        return flow;
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
