package org.dromara.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.procurement.domain.PmsAcceptance;
import org.dromara.procurement.domain.PmsAcceptanceItem;
import org.dromara.procurement.domain.PmsInvoiceInfo;
import org.dromara.procurement.domain.PmsProcurementRequest;
import org.dromara.procurement.domain.PmsProject;
import org.dromara.procurement.domain.bo.PmsInvoiceWorkbenchBo;
import org.dromara.procurement.domain.vo.PmsInvoiceWorkbenchVo;
import org.dromara.procurement.mapper.PmsAcceptanceItemMapper;
import org.dromara.procurement.mapper.PmsAcceptanceMapper;
import org.dromara.procurement.mapper.PmsInvoiceInfoMapper;
import org.dromara.procurement.mapper.PmsProcurementRequestMapper;
import org.dromara.procurement.mapper.PmsProjectMapper;
import org.dromara.procurement.service.IPmsInvoiceWorkbenchService;
import org.dromara.system.domain.SysUser;
import org.dromara.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 发票上传工作台Service实现
 *
 * <p>行 = 已验收的采购申请（数据源：验收单 status=finish，一个申请最多一个验收单）。
 * 聚合验收明细覆盖度 + 发票台账统计；数据量不大，先全量聚合再内存排序分页
 * （与 PmsInvoiceInfoController.list 的手动分页策略一致）。</p>
 *
 * @author procurement
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PmsInvoiceWorkbenchServiceImpl implements IPmsInvoiceWorkbenchService {

    private final PmsAcceptanceMapper acceptanceMapper;
    private final PmsAcceptanceItemMapper acceptanceItemMapper;
    private final PmsProcurementRequestMapper requestMapper;
    private final PmsProjectMapper projectMapper;
    private final PmsInvoiceInfoMapper invoiceInfoMapper;
    private final SysUserMapper userMapper;

    @Override
    public PageResult<PmsInvoiceWorkbenchVo> queryPageList(PmsInvoiceWorkbenchBo bo, PageQuery pageQuery) {
        // 1. 已完成的验收单（没有验收单的申请不进列表）
        LambdaQueryWrapper<PmsAcceptance> accWrapper = Wrappers.lambdaQuery();
        accWrapper.eq(PmsAcceptance::getStatus, "finish");
        if (bo != null && bo.getProjectId() != null) {
            accWrapper.eq(PmsAcceptance::getProjectId, bo.getProjectId());
        }
        accWrapper.orderByDesc(PmsAcceptance::getCreateTime);
        List<PmsAcceptance> acceptances = acceptanceMapper.selectList(accWrapper);
        if (acceptances.isEmpty()) {
            return PageResult.build();
        }

        // 2. 关联申请 + 关键字过滤（申请编号/标题模糊匹配）
        List<Long> requestIds = acceptances.stream()
            .map(PmsAcceptance::getRequestId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, PmsProcurementRequest> requestMap = requestIds.isEmpty() ? Map.of() :
            requestMapper.selectByIds(requestIds).stream()
                .collect(Collectors.toMap(PmsProcurementRequest::getId, Function.identity(), (a, b) -> a));
        String keyword = bo != null ? bo.getKeyword() : null;
        List<PmsAcceptance> filtered = acceptances.stream()
            .filter(acc -> {
                PmsProcurementRequest req = requestMap.get(acc.getRequestId());
                if (req == null) {
                    return false;
                }
                if (StringUtils.isBlank(keyword)) {
                    return true;
                }
                return StringUtils.contains(req.getRequestCode(), keyword) || StringUtils.contains(req.getTitle(), keyword);
            })
            .toList();
        if (filtered.isEmpty()) {
            return PageResult.build();
        }
        List<Long> filteredRequestIds = filtered.stream().map(PmsAcceptance::getRequestId).toList();

        // 3. 批量聚合：验收明细 / 发票台账 / 项目 / 用户昵称
        Map<Long, List<PmsAcceptanceItem>> itemsByAcceptance = acceptanceItemMapper
            .selectList(Wrappers.<PmsAcceptanceItem>lambdaQuery()
                .in(PmsAcceptanceItem::getAcceptanceId, filtered.stream().map(PmsAcceptance::getId).toList())
                .orderByAsc(PmsAcceptanceItem::getId))
            .stream()
            .collect(Collectors.groupingBy(PmsAcceptanceItem::getAcceptanceId));

        List<PmsInvoiceInfo> invoices = invoiceInfoMapper.selectList(Wrappers.<PmsInvoiceInfo>lambdaQuery()
            .in(PmsInvoiceInfo::getRequestId, filteredRequestIds));
        Map<Long, List<PmsInvoiceInfo>> invoicesByRequest = invoices.stream()
            .collect(Collectors.groupingBy(PmsInvoiceInfo::getRequestId));
        // 有效发票覆盖的明细ID集合（全局，明细ID唯一归属于某验收单）
        Set<Long> coveredItemIds = invoices.stream()
            .filter(inv -> Objects.equals(inv.getValidFlag(), 1) && inv.getAcceptanceItemId() != null)
            .map(PmsInvoiceInfo::getAcceptanceItemId)
            .collect(Collectors.toSet());

        List<Long> projectIds = filteredRequestIds.stream()
            .map(requestMap::get)
            .filter(Objects::nonNull)
            .map(PmsProcurementRequest::getProjectId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, String> projectNameMap = projectIds.isEmpty() ? Map.of() :
            projectMapper.selectByIds(projectIds).stream()
                .collect(Collectors.toMap(PmsProject::getId, p -> p.getProjectName() == null ? "" : p.getProjectName(), (a, b) -> a));

        Set<Long> userIds = new HashSet<>();
        filteredRequestIds.stream().map(requestMap::get).filter(Objects::nonNull).forEach(req -> {
            if (req.getCreateBy() != null) {
                userIds.add(req.getCreateBy());
            }
            if (req.getInvoiceDoneBy() != null) {
                userIds.add(req.getInvoiceDoneBy());
            }
        });
        Map<Long, String> nickNameMap = userIds.isEmpty() ? Map.of() :
            userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getUserId, u -> u.getNickName() == null ? "" : u.getNickName(), (a, b) -> a));

        // 4. 组装行
        List<PmsInvoiceWorkbenchVo> rows = new ArrayList<>(filtered.size());
        for (PmsAcceptance acc : filtered) {
            PmsProcurementRequest req = requestMap.get(acc.getRequestId());
            if (req == null) {
                continue;
            }
            List<PmsAcceptanceItem> items = itemsByAcceptance.getOrDefault(acc.getId(), List.of());
            List<PmsInvoiceInfo> reqInvoices = invoicesByRequest.getOrDefault(req.getId(), List.of());
            long validCount = reqInvoices.stream().filter(inv -> Objects.equals(inv.getValidFlag(), 1)).count();
            LocalDateTime lastInvoiceTime = reqInvoices.stream()
                .map(PmsInvoiceInfo::getCreateTime)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

            int itemTotal = items.size();
            int itemCovered = (int) items.stream()
                .map(PmsAcceptanceItem::getId)
                .filter(coveredItemIds::contains)
                .count();

            PmsInvoiceWorkbenchVo vo = new PmsInvoiceWorkbenchVo();
            vo.setRequestId(req.getId());
            vo.setRequestCode(req.getRequestCode());
            vo.setRequestTitle(req.getTitleName());
            vo.setProjectName(projectNameMap.get(req.getProjectId()));
            vo.setApplicantName(nickNameMap.get(req.getCreateBy()));
            vo.setAcceptanceId(acc.getId());
            vo.setAcceptanceCode(acc.getAcceptanceCode());
            vo.setAcceptanceDate(acc.getAcceptanceDate());
            vo.setItemTotal(itemTotal);
            vo.setItemCovered(itemCovered);
            vo.setInvoiceTotal(reqInvoices.size());
            vo.setInvoiceValid((int) validCount);
            boolean done = Objects.equals(req.getInvoiceDoneFlag(), 1);
            vo.setDoneFlag(done ? 1 : 0);
            vo.setStatus(done ? "done" : (reqInvoices.isEmpty() ? "none" : "processing"));
            vo.setDoneTime(req.getInvoiceDoneTime());
            vo.setDoneBy(nickNameMap.get(req.getInvoiceDoneBy()));
            vo.setLastInvoiceTime(lastInvoiceTime);
            rows.add(vo);
        }

        // 5. 状态筛选 + 排序（processing 优先 > lastInvoiceTime 降序）
        List<PmsInvoiceWorkbenchVo> filteredRows = rows.stream()
            .filter(vo -> matchStatus(vo.getStatus(), bo != null ? bo.getStatus() : null))
            .sorted(Comparator
                .comparingInt((PmsInvoiceWorkbenchVo vo) -> "processing".equals(vo.getStatus()) ? 0 : 1)
                .thenComparing(PmsInvoiceWorkbenchVo::getLastInvoiceTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(PmsInvoiceWorkbenchVo::getRequestId, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

        // 6. 手动分页（与 PmsInvoiceInfoController.list 一致）
        int total = filteredRows.size();
        int pageNum = pageQuery != null && pageQuery.getPageNum() != null ? pageQuery.getPageNum() : PageQuery.DEFAULT_PAGE_NUM;
        int pageSize = pageQuery != null && pageQuery.getPageSize() != null ? pageQuery.getPageSize() : PageQuery.DEFAULT_PAGE_SIZE;
        long safePageSize = pageSize <= 0 ? PageQuery.DEFAULT_PAGE_SIZE : pageSize;
        int from = (int) Math.min(((long) (pageNum - 1) * safePageSize), total);
        int to = (int) Math.min(from + safePageSize, total);
        List<PmsInvoiceWorkbenchVo> pageRows = from < total ? filteredRows.subList(from, to) : List.of();
        return PageResult.build(pageRows, total);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeDoneFlag(PmsInvoiceWorkbenchBo bo) {
        PmsProcurementRequest request = requestMapper.selectById(bo.getRequestId());
        if (request == null) {
            throw new ServiceException("采购申请不存在");
        }
        boolean alreadyDone = Objects.equals(request.getInvoiceDoneFlag(), 1);
        // 幂等：已是目标状态则直接返回（保留首次留痕）
        if (bo.getDone() && alreadyDone) {
            return;
        }
        if (!bo.getDone() && !alreadyDone) {
            return;
        }
        LambdaUpdateWrapper<PmsProcurementRequest> uw = Wrappers.lambdaUpdate();
        uw.eq(PmsProcurementRequest::getId, request.getId())
            .set(PmsProcurementRequest::getInvoiceDoneFlag, bo.getDone() ? 1 : 0);
        if (bo.getDone()) {
            uw.set(PmsProcurementRequest::getInvoiceDoneTime, LocalDateTime.now())
                .set(PmsProcurementRequest::getInvoiceDoneBy, LoginHelper.getUserId());
        } else {
            // 取消完成：时间/操作人一并清空
            uw.set(PmsProcurementRequest::getInvoiceDoneTime, null)
                .set(PmsProcurementRequest::getInvoiceDoneBy, null);
        }
        requestMapper.update(null, uw);
    }

    private boolean matchStatus(String rowStatus, String queryStatus) {
        if (StringUtils.isBlank(queryStatus)) {
            return true;
        }
        return switch (queryStatus) {
            case "unfinished" -> "none".equals(rowStatus) || "processing".equals(rowStatus);
            case "none", "processing", "done" -> queryStatus.equals(rowStatus);
            default -> true;
        };
    }

}
