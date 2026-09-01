package org.dromara.procurement.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.workflow.api.domain.StartProcessDTO;
import org.dromara.workflow.api.domain.StartProcessReturnDTO;
import org.dromara.workflow.api.event.ProcessDeleteEvent;
import org.dromara.workflow.api.event.ProcessEvent;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.api.WorkflowService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.procurement.domain.PmsProcurementRequest;
import org.dromara.procurement.domain.PmsProcurementRequestItem;
import org.dromara.procurement.domain.PmsProject;
import org.dromara.procurement.domain.PmsFundFlow;
import org.dromara.procurement.domain.bo.PmsProcurementRequestBo;
import org.dromara.procurement.domain.bo.PmsProcurementRequestItemBo;
import org.dromara.procurement.domain.vo.PmsProcurementRequestItemVo;
import org.dromara.procurement.domain.vo.PmsProcurementRequestVo;
import org.dromara.procurement.mapper.PmsProcurementRequestItemMapper;
import org.dromara.procurement.mapper.PmsProcurementRequestMapper;
import org.dromara.procurement.mapper.PmsProjectMapper;
import org.dromara.procurement.mapper.PmsFundFlowMapper;
import org.dromara.procurement.mapper.PmsFlowApproverMapper;
import org.dromara.procurement.service.IPmsProcurementRequestService;
import org.dromara.procurement.utils.PmsPlatformUtil;
import org.dromara.system.domain.SysUser;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysConfigService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.core.io.ClassPathResource;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 采购管理-采购申请Service业务层处理
 *
 * @author procurement
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PmsProcurementRequestServiceImpl implements IPmsProcurementRequestService {

    private final PmsProcurementRequestMapper baseMapper;
    private final PmsProcurementRequestItemMapper itemMapper;
    private final PmsProjectMapper projectMapper;
    private final PmsFlowApproverMapper flowApproverMapper;
    private final PmsFundFlowMapper fundFlowMapper;
    private final WorkflowService workflowService;
    private final ISysConfigService configService;
    private final SysUserMapper userMapper;

    /**
     * 已导出的文件名集合（按天），用于导出 Excel 同名时追加三位序数
     */
    private static final Map<String, Set<String>> EXPORTED_FILE_NAMES = new ConcurrentHashMap<>();

    /**
     * 批量填充当前审批人（从流程 waiting 任务动态查询，不硬编码）
     */
    private void fillCurrentApprover(List<PmsProcurementRequestVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (PmsProcurementRequestVo vo : list) {
            if (vo.getProcessInstanceId() != null) {
                List<String> names = flowApproverMapper.selectCurrentApproverNames(vo.getProcessInstanceId());
                if (CollUtil.isNotEmpty(names)) {
                    vo.setCurrentApprover(String.join("、", names));
                }
            }
        }
    }

    @Override
    public PmsProcurementRequestVo queryById(Long id) {
        PmsProcurementRequestVo vo = baseMapper.selectVoById(id);
        if (ObjectUtil.isNotNull(vo)) {
            List<PmsProcurementRequestItemVo> items = itemMapper.selectVoList(
                Wrappers.<PmsProcurementRequestItem>lambdaQuery()
                    .eq(PmsProcurementRequestItem::getRequestId, id)
                    .orderByAsc(PmsProcurementRequestItem::getSortNo));
            vo.setItems(items);
            if (vo.getProcessInstanceId() != null) {
                List<String> names = flowApproverMapper.selectCurrentApproverNames(vo.getProcessInstanceId());
                if (CollUtil.isNotEmpty(names)) {
                    vo.setCurrentApprover(String.join("、", names));
                }
            }
        }
        return vo;
    }

    @Override
    public PmsProcurementRequest getById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public PageResult<PmsProcurementRequestVo> queryPageList(PmsProcurementRequestBo bo, PageQuery pageQuery) {
        Page<PmsProcurementRequestVo> page = pageQuery.build();
        baseMapper.selectVoPageList(page, bo);
        fillCurrentApprover(page.getRecords());
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public List<PmsProcurementRequestVo> queryList(PmsProcurementRequestBo bo) {
        Page<PmsProcurementRequestVo> page = new Page<>(1, Integer.MAX_VALUE);
        baseMapper.selectVoPageList(page, bo);
        fillCurrentApprover(page.getRecords());
        return page.getRecords();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(PmsProcurementRequestBo bo) {
        if (StringUtils.isBlank(bo.getRequestCode())) {
            bo.setRequestCode(generateRequestCode());
        }
        if (StringUtils.isBlank(bo.getStatus())) {
            bo.setStatus(BusinessStatusEnum.DRAFT.getStatus());
        }
        bo.setTitle(buildTitle(bo));
        calcHeaderAmount(bo);
        PmsProcurementRequest add = MapstructUtils.convert(bo, PmsProcurementRequest.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
            saveItems(add.getId(), bo.getItems());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(PmsProcurementRequestBo bo) {
        if (ObjectUtil.isNull(bo.getId())) {
            throw new ServiceException("申请ID不能为空");
        }
        PmsProcurementRequest exist = baseMapper.selectById(bo.getId());
        if (ObjectUtil.isNull(exist)) {
            throw new ServiceException("采购申请不存在");
        }
        calcHeaderAmount(bo);
        bo.setTitle(buildTitle(bo));
        PmsProcurementRequest update = MapstructUtils.convert(bo, PmsProcurementRequest.class);
        boolean flag = baseMapper.updateById(update) > 0;
        if (flag) {
            itemMapper.delete(Wrappers.<PmsProcurementRequestItem>lambdaQuery()
                .eq(PmsProcurementRequestItem::getRequestId, bo.getId()));
            saveItems(bo.getId(), bo.getItems());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        // 删除前同步清理资金：已审批通过的申请需要扣减项目已用金额并删除资金流水
        List<PmsProcurementRequest> finishedRequests = baseMapper.selectList(
            Wrappers.<PmsProcurementRequest>lambdaQuery()
                .in(PmsProcurementRequest::getId, ids)
                .eq(PmsProcurementRequest::getStatus, BusinessStatusEnum.FINISH.getStatus())
                .eq(PmsProcurementRequest::getDelFlag, 0L));
        for (PmsProcurementRequest request : finishedRequests) {
            rollbackUsedAmount(request);
            deleteFundFlow(request.getId());
        }

        itemMapper.delete(Wrappers.<PmsProcurementRequestItem>lambdaQuery()
            .in(PmsProcurementRequestItem::getRequestId, ids));
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 扣减项目已用金额（采购申请删除时回滚）
     */
    private void rollbackUsedAmount(PmsProcurementRequest request) {
        if (ObjectUtil.isNull(request.getProjectId()) || ObjectUtil.isNull(request.getAmount())) {
            return;
        }
        PmsProject project = projectMapper.selectById(request.getProjectId());
        if (ObjectUtil.isNull(project)) {
            return;
        }
        BigDecimal used = ObjectUtil.isNull(project.getUsedAmount()) ? BigDecimal.ZERO : project.getUsedAmount();
        BigDecimal remain = used.subtract(request.getAmount());
        project.setUsedAmount(remain.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remain);
        projectMapper.updateById(project);
        log.info("项目已用金额回滚：projectId={}, rollbackAmount={}, newUsedAmount={}",
            request.getProjectId(), request.getAmount(), project.getUsedAmount());
    }

    /**
     * 删除采购申请对应的资金流水（幂等）
     */
    private void deleteFundFlow(Long requestId) {
        if (ObjectUtil.isNull(requestId)) {
            return;
        }
        fundFlowMapper.delete(Wrappers.<PmsFundFlow>lambdaQuery()
            .eq(PmsFundFlow::getRequestId, requestId)
            .eq(PmsFundFlow::getFlowType, "out"));
        log.info("资金流水已删除：requestId={}", requestId);
    }

    @Override
    public List<PmsProcurementRequestVo> queryApprovedList() {
        PmsProcurementRequestBo bo = new PmsProcurementRequestBo();
        bo.setStatus(BusinessStatusEnum.FINISH.getStatus());
        return queryList(bo);
    }

    /**
     * 可验收的采购申请：已审批通过（finish）且验收标志为 none 或 null（尚未创建验收单）
     */
    @Override
    public List<PmsProcurementRequestVo> queryAcceptableList() {
        List<PmsProcurementRequestVo> list = baseMapper.selectAcceptableList();
        fillCurrentApprover(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PmsProcurementRequestVo submitAndStartFlow(PmsProcurementRequestBo bo) {
        if (ObjectUtil.isNull(bo.getId())) {
            insertByBo(bo);
        } else {
            PmsProcurementRequest exist = baseMapper.selectById(bo.getId());
            if (ObjectUtil.isNull(exist)) {
                throw new ServiceException("采购申请不存在");
            }
            BusinessStatusEnum.checkStartStatus(exist.getStatus());
            updateByBo(bo);
        }

        PmsProcurementRequest request = baseMapper.selectById(bo.getId());
        // 提交时校验资金：总金额 ≤ 项目剩余资金，超出直接拒绝
        checkBudget(bo);
        PmsProject project = projectMapper.selectById(request.getProjectId());
        if (project == null || project.getLeaderId() == null) {
            throw new ServiceException("请先为项目配置负责人（用户）");
        }
        StartProcessDTO startProcess = new StartProcessDTO();
        startProcess.setBusinessId(request.getId().toString());
        startProcess.setFlowCode("pms_request");
        Map<String, Object> variables = new HashMap<>();
        variables.put("leaderId", project.getLeaderId().toString());
        // 申请总金额：流程条件分支 ceo -> end(<1000) / supreme_decision_maker(>=1000)
        BigDecimal amount = ObjectUtil.isNull(request.getAmount()) ? BigDecimal.ZERO : request.getAmount();
        variables.put("amount", amount);
        startProcess.setVariables(variables);
        // startCompleteTask = 启动流程 + 提交首个（申请人）节点，触发总体流程监听，
        // 由 processHandler 将实例推进到 leader 节点并把单据状态置为 waiting。
        boolean started = workflowService.startCompleteTask(startProcess);
        if (!started) {
            throw new ServiceException("流程发起失败");
        }
        request.setStatus(BusinessStatusEnum.WAITING.getStatus());
        baseMapper.updateById(request);
        return queryById(request.getId());
    }

    /**
     * 总体流程监听
     */
    @EventListener(condition = "#processEvent.flowCode.startsWith('pms_request')")
    public void processHandler(ProcessEvent processEvent) {
        log.info("采购申请流程执行了{}", processEvent);
        PmsProcurementRequest request = baseMapper.selectById(Convert.toLong(processEvent.getBusinessId()));
        if (ObjectUtil.isNull(request)) {
            return;
        }
        String oldStatus = request.getStatus();
        request.setStatus(processEvent.getStatus());
        request.setProcessInstanceId(processEvent.getInstanceId());
        if (Boolean.TRUE.equals(processEvent.getSubmit())) {
            if (StringUtils.isBlank(request.getRequestCode())) {
                String businessCode = MapUtil.getStr(processEvent.getParams(), "businessCode", "");
                request.setRequestCode(businessCode);
            }
            request.setStatus(BusinessStatusEnum.WAITING.getStatus());
            log.info("采购申请提交");
        }
        baseMapper.updateById(request);
        // 审批通过（进入完成态）时累加项目已用金额，防重复累加
        if (BusinessStatusEnum.FINISH.getStatus().equals(request.getStatus())
            && !BusinessStatusEnum.FINISH.getStatus().equals(oldStatus)) {
            accumulateUsedAmount(request);
            createFundFlow(request);
        }
    }

    /**
     * 审批通过后写入资金流水（幂等：同一申请只写一条 out 流水）
     */
    private void createFundFlow(PmsProcurementRequest request) {
        if (ObjectUtil.isNull(request.getAmount()) || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        long exist = fundFlowMapper.selectCount(Wrappers.<PmsFundFlow>lambdaQuery()
            .eq(PmsFundFlow::getRequestId, request.getId())
            .eq(PmsFundFlow::getFlowType, "out"));
        if (exist > 0) {
            return;
        }
        PmsFundFlow flow = new PmsFundFlow();
        flow.setFlowNo("FUND-" + java.time.LocalDate.now().toString().replace("-", "")
            + "-" + String.format("%03d", exist + 1));
        flow.setFlowType("out");
        flow.setRequestId(request.getId());
        flow.setRequestCode(request.getRequestCode());
        flow.setRequestTitle(request.getTitle());
        flow.setAmount(request.getAmount());
        flow.setOccurDate(java.time.LocalDate.now());
        flow.setProjectId(request.getProjectId());
        PmsProject project = projectMapper.selectById(request.getProjectId());
        if (ObjectUtil.isNotNull(project)) {
            flow.setProjectName(project.getProjectName());
            flow.setOperatorId(project.getLeaderId());
        }
        flow.setOperatorName(LoginHelper.getUsername());
        flow.setRemark("采购申请审批通过自动记录");
        fundFlowMapper.insert(flow);
        log.info("资金流水已记录：申请[{}] 金额[{}]", request.getId(), request.getAmount());
    }

    /**
     * 审批通过后累加项目已用金额
     */
    private void accumulateUsedAmount(PmsProcurementRequest request) {
        PmsProject project = projectMapper.selectById(request.getProjectId());
        if (ObjectUtil.isNull(project)) {
            return;
        }
        BigDecimal used = ObjectUtil.isNull(project.getUsedAmount()) ? BigDecimal.ZERO : project.getUsedAmount();
        BigDecimal amount = ObjectUtil.isNull(request.getAmount()) ? BigDecimal.ZERO : request.getAmount();
        project.setUsedAmount(used.add(amount));
        projectMapper.updateById(project);
    }

    /**
     * 监听删除流程事件
     */
    @EventListener(condition = "#processDeleteEvent.flowCode.startsWith('pms_request')")
    public void processDeleteHandler(ProcessDeleteEvent processDeleteEvent) {
        log.info("监听删除采购申请流程事件，当前任务执行了{}", processDeleteEvent);
        Long id = Convert.toLong(processDeleteEvent.getBusinessId());
        PmsProcurementRequest request = baseMapper.selectById(id);
        if (ObjectUtil.isNull(request)) {
            return;
        }
        itemMapper.delete(Wrappers.<PmsProcurementRequestItem>lambdaQuery()
            .eq(PmsProcurementRequestItem::getRequestId, id));
        baseMapper.deleteById(id);
    }

    /**
     * 资金校验：总金额 ≤ 项目剩余资金（预算 - 已用），超出直接拒绝
     */
    private void checkBudget(PmsProcurementRequestBo bo) {
        if (bo.getProjectId() == null) {
            throw new ServiceException("请选择项目");
        }
        PmsProject project = projectMapper.selectById(bo.getProjectId());
        if (ObjectUtil.isNull(project)) {
            throw new ServiceException("项目不存在");
        }
        BigDecimal budget = ObjectUtil.isNull(project.getBudget()) ? BigDecimal.ZERO : project.getBudget();
        BigDecimal used = ObjectUtil.isNull(project.getUsedAmount()) ? BigDecimal.ZERO : project.getUsedAmount();
        BigDecimal remaining = budget.subtract(used);
        BigDecimal amount = ObjectUtil.isNull(bo.getAmount()) ? BigDecimal.ZERO : bo.getAmount();
        if (amount.compareTo(remaining) > 0) {
            throw new ServiceException("采购总金额超出项目剩余资金（剩余 " + remaining.stripTrailingZeros().toPlainString() + " 元），申请被拒绝");
        }
    }

    /**
     * 保存申请明细
     */
    private void saveItems(Long requestId, List<PmsProcurementRequestItemBo> itemBos) {
        if (CollUtil.isEmpty(itemBos)) {
            return;
        }
        List<PmsProcurementRequestItem> items = new ArrayList<>();
        int sort = 1;
        for (PmsProcurementRequestItemBo itemBo : itemBos) {
            PmsProcurementRequestItem item = MapstructUtils.convert(itemBo, PmsProcurementRequestItem.class);
            // 先删后插重建明细：清空回传 id，重新生成主键，避免主键冲突
            item.setId(null);
            item.setRequestId(requestId);
            if (ObjectUtil.isNull(item.getSortNo())) {
                item.setSortNo(sort++);
            }
            // 有链接且未识别平台时自动识别（把平台视为供应商）
            if (StringUtils.isBlank(item.getPlatform()) && StringUtils.isNotBlank(item.getLink())) {
                item.setPlatform(PmsPlatformUtil.detectPlatform(item.getLink()));
            }
            calcItemAmount(item);
            items.add(item);
        }
        itemMapper.insertBatch(items);
    }

    /**
     * 根据明细计算表头总金额
     */
    private void calcHeaderAmount(PmsProcurementRequestBo bo) {
        BigDecimal total = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(bo.getItems())) {
            for (PmsProcurementRequestItemBo item : bo.getItems()) {
                total = total.add(calcItemAmount(item));
            }
        }
        bo.setAmount(total);
    }

    /**
     * 计算明细金额
     */
    private BigDecimal calcItemAmount(PmsProcurementRequestItemBo item) {
        BigDecimal qty = ObjectUtil.isNull(item.getQuantity()) ? BigDecimal.ZERO : item.getQuantity();
        BigDecimal price = ObjectUtil.isNull(item.getUnitPrice()) ? BigDecimal.ZERO : item.getUnitPrice();
        BigDecimal total = qty.multiply(price);
        item.setAmount(total);
        return total;
    }

    private void calcItemAmount(PmsProcurementRequestItem item) {
        BigDecimal qty = ObjectUtil.isNull(item.getQuantity()) ? BigDecimal.ZERO : item.getQuantity();
        BigDecimal price = ObjectUtil.isNull(item.getUnitPrice()) ? BigDecimal.ZERO : item.getUnitPrice();
        item.setAmount(qty.multiply(price));
    }

    /**
     * 生成申请编号 purq-yyyyMMdd-NNN
     */
    private String generateRequestCode() {
        String prefix = "purq-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        LambdaQueryWrapper<PmsProcurementRequest> lqw = Wrappers.lambdaQuery();
        lqw.likeRight(PmsProcurementRequest::getRequestCode, prefix);
        long count = baseMapper.selectCount(lqw);
        return prefix + String.format("%03d", count + 1);
    }

    /**
     * 拼接申请标题：【自购/对公】+项目名+月份月日期日+名称
     */
    private String buildTitle(PmsProcurementRequestBo bo) {
        boolean hasType = StringUtils.isNotBlank(bo.getTitleType());
        boolean hasName = StringUtils.isNotBlank(bo.getTitleName());
        if (!hasType && !hasName) {
            // 标题类型与名称均未填写时，保留前端传入的标题
            return bo.getTitle();
        }
        String type = hasType ? bo.getTitleType() : "自购";
        if (!type.startsWith("【")) {
            type = "【" + type + "】";
        }
        String projectName = "";
        if (ObjectUtil.isNotNull(bo.getProjectId())) {
            PmsProject project = projectMapper.selectById(bo.getProjectId());
            if (ObjectUtil.isNotNull(project)) {
                projectName = project.getProjectName();
            }
        }
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("MM月dd日"));
        String name = bo.getTitleName() == null ? "" : bo.getTitleName();
        return type + projectName + "_" + date + "_" + name;
    }

    /**
     * 导出采购申请表 Excel（按最终模板填充）
     */
    @Override
    public void exportFormExcel(Long id, HttpServletResponse response) {
        PmsProcurementRequestVo vo = queryById(id);
        if (ObjectUtil.isNull(vo)) {
            throw new ServiceException("采购申请不存在");
        }
        try {
            ClassPathResource tpl = new ClassPathResource("templates/最终模板.xlsx");
            try (Workbook wb = WorkbookFactory.create(tpl.getInputStream())) {
                Sheet sheet = wb.getSheetAt(0);

                // 申请人：取采购单创建人姓名，无昵称则取用户名
                SysUser applicantUser = userMapper.selectById(vo.getCreateBy());
                String applicantName = applicantUser == null ? "" :
                    StringUtils.isNotBlank(applicantUser.getNickName()) ? applicantUser.getNickName() : applicantUser.getUserName();

                // A1 项目归属：全画幅结构光超分辨显微镜系统及其子项目保持模板原文字，其他填"长三角物理研究中心"
                String projectBelong = "长三角物理研究中心";
                Long projectId = vo.getProjectId();
                if (projectId != null && isInSpecialProjectTree(projectId)) {
                    Row row1 = sheet.getRow(0);
                    if (row1 != null && row1.getCell(0) != null) {
                        projectBelong = row1.getCell(0).getStringCellValue();
                    }
                }
                setCell(sheet, 0, 0, projectBelong);
                // A1 字体强制为黑色：theme=1 在 WPS 某些主题下会渲染成红色，显式指定 RGB 避免
                Row a1Row = sheet.getRow(0);
                if (a1Row != null && a1Row.getCell(0) != null) {
                    org.apache.poi.ss.usermodel.Font blackFont = wb.createFont();
                    blackFont.setFontName("仿宋");
                    blackFont.setFontHeightInPoints((short) 18);
                    blackFont.setBold(true);
                    blackFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.BLACK.getIndex());
                    org.apache.poi.ss.usermodel.CellStyle a1Style = wb.createCellStyle();
                    a1Style.cloneStyleFrom(a1Row.getCell(0).getCellStyle());
                    a1Style.setFont(blackFont);
                    a1Row.getCell(0).setCellStyle(a1Style);
                }

                // C3 申请人（合并区域 C3:D3 左上角）
                setCell(sheet, 2, 2, applicantName);

                // 清空模板示例/旧数据行（第8行起，索引7）：逐单元格清空内容与超链接
                int start = 7;
                int lastRow = sheet.getLastRowNum();
                for (int r = start; r <= lastRow; r++) {
                    Row row = sheet.getRow(r);
                    if (row != null) {
                        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
                            org.apache.poi.ss.usermodel.Cell cell = row.getCell(c);
                            if (cell != null) {
                                cell.setCellValue("");
                                cell.setHyperlink(null);
                            }
                        }
                    }
                }

                // 写明细（从第8行开始，序号 1,2,3...）
                List<PmsProcurementRequestItemVo> items = vo.getItems();
                if (CollUtil.isNotEmpty(items)) {
                    int rowIdx = start;
                    int seq = 1;
                    for (PmsProcurementRequestItemVo item : items) {
                        BigDecimal qty = ObjectUtil.isNull(item.getQuantity()) ? BigDecimal.ZERO : item.getQuantity();
                        BigDecimal price = ObjectUtil.isNull(item.getUnitPrice()) ? BigDecimal.ZERO : item.getUnitPrice();
                        BigDecimal baseAmount = qty.multiply(price);
                        // 预估总价：<100 则 +10，>=100 则 *1.3
                        BigDecimal estimatedAmount;
                        if (baseAmount.compareTo(BigDecimal.valueOf(100)) < 0) {
                            estimatedAmount = baseAmount.add(BigDecimal.valueOf(10));
                        } else {
                            estimatedAmount = baseAmount.multiply(BigDecimal.valueOf(1.3));
                        }
                        estimatedAmount = estimatedAmount.setScale(2, java.math.RoundingMode.HALF_UP);

                        setCell(sheet, rowIdx, 0, seq++);                           // A 序号
                        setCell(sheet, rowIdx, 1, item.getPurchaseType());          // B 采购种类
                        setCell(sheet, rowIdx, 2, item.getCategory1());             // C 一级分类
                        setCell(sheet, rowIdx, 3, item.getCategory2());             // D 二级分类
                        setCell(sheet, rowIdx, 4, projectBelong);                   // E 项目归属
                        setCell(sheet, rowIdx, 5, item.getItemName());              // F 品名
                        setCell(sheet, rowIdx, 6, item.getUnit());                  // G 单位
                        setCell(sheet, rowIdx, 7, qty.doubleValue());               // H 数量
                        setCell(sheet, rowIdx, 8, item.getSpec());                  // I 规格型号/技术要求
                        setCell(sheet, rowIdx, 9, item.getBrand());                 // J 参考品牌
                        // K 实际单价、L 实际总价 不填
                        setCell(sheet, rowIdx, 12, estimatedAmount.doubleValue());  // M 预估总价
                        setCell(sheet, rowIdx, 13, applicantName);                  // N 保管人
                        setCell(sheet, rowIdx, 14, applicantName);                  // O 使用人
                        setCell(sheet, rowIdx, 15, item.getMaterialUsage());        // P 物料用途
                        // Q 采购理由：优先明细级采购原因，空则回退申请级申请理由
                        String rowReason = StringUtils.isNotBlank(item.getPurchaseReason())
                            ? item.getPurchaseReason() : vo.getApplyReason();
                        setCell(sheet, rowIdx, 16, rowReason);                      // Q 采购理由
                        setHyperLink(sheet, rowIdx, 17, item.getLink());            // R 采购链接
                        rowIdx++;
                    }
                }

                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                String title = StringUtils.isNotBlank(vo.getTitle()) ? vo.getTitle() : vo.getRequestCode();
                // 文件名 = 标题 + .xlsx，同名时追加三位序数避免重复
                String filename = dedupFileName(title + ".xlsx");
                String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
                response.setHeader("Content-Disposition", "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
                try (OutputStream os = response.getOutputStream()) {
                    wb.write(os);
                    os.flush();
                }
            }
        } catch (IOException e) {
            log.error("导出采购申请表失败", e);
            throw new ServiceException("导出采购申请表失败：" + e.getMessage());
        }
    }

    /**
     * 判断项目是否属于"全画幅结构光超分辨显微镜系统"及其子项目树
     */
    private boolean isInSpecialProjectTree(Long projectId) {
        if (projectId == null) {
            return false;
        }
        Long currentId = projectId;
        int depth = 0;
        while (currentId != null && depth < 50) {
            PmsProject project = projectMapper.selectById(currentId);
            if (project == null) {
                return false;
            }
            if ("全画幅结构光超分辨显微镜系统".equals(project.getProjectName())) {
                return true;
            }
            currentId = project.getParentId();
            depth++;
        }
        return false;
    }

    /**
     * 导出文件名防重：当天已导出过同名文件时，追加三位序数（_001、_002…）
     */
    private String dedupFileName(String baseFileName) {
        String day = LocalDate.now().toString();
        Set<String> names = EXPORTED_FILE_NAMES.computeIfAbsent(day, k -> ConcurrentHashMap.newKeySet());
        synchronized (names) {
            if (names.add(baseFileName)) {
                return baseFileName;
            }
            int seq = 1;
            String base = baseFileName.replaceFirst("\\.xlsx$", "");
            while (true) {
                String candidate = base + "_" + String.format("%03d", seq) + ".xlsx";
                if (names.add(candidate)) {
                    return candidate;
                }
                seq++;
            }
        }
    }

    /**
     * 写入单元格（行/列从0开始），自动补全空行/空单元格
     */
    private void setCell(Sheet sheet, int rowIdx, int colIdx, Object value) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }
        org.apache.poi.ss.usermodel.Cell cell = row.getCell(colIdx);
        if (cell == null) {
            cell = row.createCell(colIdx);
        }
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    /**
     * 设置单元格超链接（URL 为空时清空）
     */
    private void setHyperLink(Sheet sheet, int rowIdx, int colIdx, String url) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }
        org.apache.poi.ss.usermodel.Cell cell = row.getCell(colIdx);
        if (cell == null) {
            cell = row.createCell(colIdx);
        }
        if (StringUtils.isBlank(url)) {
            cell.setCellValue("");
            cell.setHyperlink(null);
            return;
        }
        String safeUrl = url.trim();
        if (!safeUrl.startsWith("http://") && !safeUrl.startsWith("https://") && !safeUrl.startsWith("file://")) {
            safeUrl = "https://" + safeUrl;
        }
        cell.setCellValue(safeUrl);
        org.apache.poi.ss.usermodel.Hyperlink link = sheet.getWorkbook().getCreationHelper().createHyperlink(org.apache.poi.common.usermodel.HyperlinkType.URL);
        link.setAddress(safeUrl);
        link.setLabel(safeUrl);
        cell.setHyperlink(link);
    }

}
