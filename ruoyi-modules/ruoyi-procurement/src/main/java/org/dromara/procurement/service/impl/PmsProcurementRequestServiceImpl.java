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
import org.dromara.procurement.domain.bo.PmsProcurementRequestBo;
import org.dromara.procurement.domain.bo.PmsProcurementRequestItemBo;
import org.dromara.procurement.domain.vo.PmsProcurementRequestItemVo;
import org.dromara.procurement.domain.vo.PmsProcurementRequestVo;
import org.dromara.procurement.mapper.PmsProcurementRequestItemMapper;
import org.dromara.procurement.mapper.PmsProcurementRequestMapper;
import org.dromara.procurement.service.IPmsProcurementRequestService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final WorkflowService workflowService;

    @Override
    public PmsProcurementRequestVo queryById(Long id) {
        PmsProcurementRequestVo vo = baseMapper.selectVoById(id);
        if (ObjectUtil.isNotNull(vo)) {
            List<PmsProcurementRequestItemVo> items = itemMapper.selectVoList(
                Wrappers.<PmsProcurementRequestItem>lambdaQuery()
                    .eq(PmsProcurementRequestItem::getRequestId, id)
                    .orderByAsc(PmsProcurementRequestItem::getSortNo));
            vo.setItems(items);
        }
        return vo;
    }

    @Override
    public PageResult<PmsProcurementRequestVo> queryPageList(PmsProcurementRequestBo bo, PageQuery pageQuery) {
        Page<PmsProcurementRequestVo> page = pageQuery.build();
        baseMapper.selectVoPageList(page, bo);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public List<PmsProcurementRequestVo> queryList(PmsProcurementRequestBo bo) {
        Page<PmsProcurementRequestVo> page = new Page<>(1, Integer.MAX_VALUE);
        baseMapper.selectVoPageList(page, bo);
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
        itemMapper.delete(Wrappers.<PmsProcurementRequestItem>lambdaQuery()
            .in(PmsProcurementRequestItem::getRequestId, ids));
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public List<PmsProcurementRequestVo> queryApprovedList() {
        PmsProcurementRequestBo bo = new PmsProcurementRequestBo();
        bo.setStatus(BusinessStatusEnum.FINISH.getStatus());
        return queryList(bo);
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
        StartProcessDTO startProcess = new StartProcessDTO();
        startProcess.setBusinessId(request.getId().toString());
        startProcess.setFlowCode("pms_request");
        Map<String, Object> variables = new HashMap<>();
        variables.put("ignore", true);
        startProcess.setVariables(variables);
        boolean ok = workflowService.startCompleteTask(startProcess);
        if (!ok) {
            throw new ServiceException("流程发起失败");
        }
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
            item.setRequestId(requestId);
            if (ObjectUtil.isNull(item.getSortNo())) {
                item.setSortNo(sort++);
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
     * 生成申请编号 PR-yyyyMMdd-NNN
     */
    private String generateRequestCode() {
        String prefix = "PR-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        LambdaQueryWrapper<PmsProcurementRequest> lqw = Wrappers.lambdaQuery();
        lqw.likeRight(PmsProcurementRequest::getRequestCode, prefix);
        long count = baseMapper.selectCount(lqw);
        return prefix + String.format("%03d", count + 1);
    }

    /**
     * 导出采购申请表 Excel（按模板填充）
     */
    @Override
    public void exportFormExcel(Long id, HttpServletResponse response) {
        PmsProcurementRequestVo vo = queryById(id);
        if (ObjectUtil.isNull(vo)) {
            throw new ServiceException("采购申请不存在");
        }
        try {
            ClassPathResource tpl = new ClassPathResource("templates/采购申请模板.xlsx");
            try (Workbook wb = WorkbookFactory.create(tpl.getInputStream())) {
                Sheet sheet = wb.getSheetAt(0);
                // 申请部门 B3、申请人 D3
                String deptName = ObjectUtil.isNull(LoginHelper.getLoginUser())
                    ? "" : LoginHelper.getLoginUser().getDeptName();
                if (deptName == null) {
                    deptName = "";
                }
                setCell(sheet, 2, 1, deptName);
                setCell(sheet, 2, 3, LoginHelper.getUsername());
                // 明细从第6行（索引5）开始
                List<PmsProcurementRequestItemVo> items = vo.getItems();
                if (CollUtil.isNotEmpty(items)) {
                    int rowIdx = 5;
                    int seq = 1;
                    for (PmsProcurementRequestItemVo item : items) {
                        BigDecimal qty = ObjectUtil.isNull(item.getQuantity()) ? BigDecimal.ZERO : item.getQuantity();
                        BigDecimal price = ObjectUtil.isNull(item.getUnitPrice()) ? BigDecimal.ZERO : item.getUnitPrice();
                        BigDecimal actual = ObjectUtil.isNull(item.getAmount()) ? qty.multiply(price) : item.getAmount();
                        BigDecimal prepay = actual.multiply(new BigDecimal("1.03")).setScale(2, java.math.RoundingMode.HALF_UP);
                        setCell(sheet, rowIdx, 0, seq++);                          // A 序号
                        setCell(sheet, rowIdx, 1, deptName);                       // B 工作室/部门
                        setCell(sheet, rowIdx, 2, item.getItemName());             // C 品名
                        setCell(sheet, rowIdx, 3, item.getSpec());                 // D 规格型号
                        setCell(sheet, rowIdx, 4, item.getBrand());                // E 品牌
                        setCell(sheet, rowIdx, 5, qty.doubleValue());              // F 数量
                        setCell(sheet, rowIdx, 6, item.getUnit());                 // G 单位
                        setCell(sheet, rowIdx, 7, price.doubleValue());            // H 单价
                        setCell(sheet, rowIdx, 8, actual.doubleValue());           // I 实际总价
                        setCell(sheet, rowIdx, 9, prepay.doubleValue());           // J 预付总价
                        rowIdx++;
                    }
                }
                response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                String filename = "采购申请表_" + vo.getRequestCode() + ".xlsx";
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

}
