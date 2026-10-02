package org.dromara.procurement.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.procurement.domain.PmsAcceptance;
import org.dromara.procurement.domain.PmsAcceptanceItem;
import org.dromara.procurement.domain.PmsInvoiceInfo;
import org.dromara.procurement.domain.PmsProcurementRequest;
import org.dromara.procurement.mapper.PmsAcceptanceItemMapper;
import org.dromara.procurement.mapper.PmsAcceptanceMapper;
import org.dromara.procurement.mapper.PmsProcurementRequestMapper;
import org.dromara.system.domain.SysOssExt;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * 采购验收发票 AI 识别 + 持久化服务。
 *
 * <p>流程：上传 PDF → OSS 存档 → 调用 agents 识别 → 程序匹配 → 写入 invoice_info。
 * 有效发票：匹配到本订单商品、且发票代码+号码不重复。
 * 无效发票：未匹配到本订单商品、或发票号重复。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PmsAcceptanceInvoiceService {

    private final AgentsInvoiceMatchService agentsInvoiceMatchService;
    private final IPmsInvoiceInfoService invoiceInfoService;
    private final ISysOssService sysOssService;
    private final PmsAcceptanceMapper acceptanceMapper;
    private final PmsProcurementRequestMapper requestMapper;
    private final PmsAcceptanceItemMapper acceptanceItemMapper;

    /**
     * 手动上传发票（不走 AI）：直接把 PDF 挂到某条验收明细上并写入台账。
     *
     * <p>用于「发票上传」弹窗中用户明确知道某张 PDF 属于哪个商品明细的场景。
     * 台账来源标记为手工，validFlag=1（用户手工指定，视为有效）。</p>
     *
     * @param acceptanceId     验收单 ID（可空，由 requestId 兜底解析）
     * @param requestId        采购申请 ID
     * @param acceptanceItemId 验收明细 ID（可空；为空则仅挂订单级）
     * @param files            发票 PDF 文件
     */
    @Transactional(rollbackFor = Exception.class)
    public JSONObject manualUpload(Long acceptanceId, Long requestId, Long acceptanceItemId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("请先上传发票 PDF 文件");
        }

        // 关联解析
        Long effRequestId = requestId;
        Long effProjectId = null;
        Long effAcceptanceId = acceptanceId;
        if (effAcceptanceId != null) {
            PmsAcceptance acceptance = acceptanceMapper.selectById(effAcceptanceId);
            if (acceptance != null) {
                if (effRequestId == null) {
                    effRequestId = acceptance.getRequestId();
                }
                effProjectId = acceptance.getProjectId();
            }
        }
        if (effRequestId != null) {
            PmsProcurementRequest request = requestMapper.selectById(effRequestId);
            if (request != null && effProjectId == null) {
                effProjectId = request.getProjectId();
            }
        }

        // 明细名（用于台账展示）
        String itemName = null;
        if (acceptanceItemId != null) {
            PmsAcceptanceItem item = acceptanceItemMapper.selectById(acceptanceItemId);
            if (item != null) {
                itemName = item.getItemName();
                if (effAcceptanceId == null) {
                    effAcceptanceId = item.getAcceptanceId();
                }
            }
        }

        // 手动挂载也按「{序号}_{商品名}_发票.pdf」重命名 OSS 文件名（与 AI 匹配建议名同规则）
        String manualBaseName = null;
        if (acceptanceItemId != null && effAcceptanceId != null) {
            List<PmsAcceptanceItem> accItems = acceptanceItemMapper.selectList(
                Wrappers.<PmsAcceptanceItem>lambdaQuery()
                    .eq(PmsAcceptanceItem::getAcceptanceId, effAcceptanceId)
                    .orderByAsc(PmsAcceptanceItem::getId));
            for (int i = 0; i < accItems.size(); i++) {
                if (acceptanceItemId.equals(accItems.get(i).getId())
                    && StringUtils.isNotBlank(accItems.get(i).getItemName())) {
                    manualBaseName = sanitizeFileName(
                        (i + 1) + "_" + accItems.get(i).getItemName() + "_发票.pdf");
                    break;
                }
            }
        }

        JSONArray results = new JSONArray();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            InvoiceFile invFile;
            try {
                invFile = new InvoiceFile(file.getOriginalFilename(), file.getBytes(), file.getContentType());
            } catch (Exception e) {
                throw new IllegalStateException("读取发票文件失败: " + file.getOriginalFilename(), e);
            }
            if (manualBaseName != null) {
                invFile.filename = manualBaseName;
            }
            SysOssVo oss = uploadToOss(invFile);
            invFile.ossId = String.valueOf(oss.getOssId());
            invFile.ossUrl = oss.getUrl();

            PmsInvoiceInfo invoice = new PmsInvoiceInfo();
            invoice.setAcceptanceId(effAcceptanceId);
            invoice.setAcceptanceItemId(acceptanceItemId);
            invoice.setRequestId(effRequestId);
            invoice.setProjectId(effProjectId);
            invoice.setPdfUrl(invFile.ossUrl);
            invoice.setPdfOssId(invFile.ossId);
            invoice.setMatchedItems(itemName);
            invoice.setValidFlag(1);
            invoice.setStatus("submitted");
            invoice.setVerifyStatus("unverified");
            invoice.setRedFlag(0);
            invoice.setRemark("手工上传");
            invoiceInfoService.saveOrUpdateInvoice(invoice);

            JSONObject row = new JSONObject();
            row.set("originalName", invFile.filename);
            row.set("invoiceId", invoice.getId());
            row.set("ossId", invFile.ossId);
            row.set("ossUrl", invFile.ossUrl);
            JSONArray matchedNames = new JSONArray();
            if (itemName != null) {
                matchedNames.add(itemName);
            }
            row.set("matchedItemNames", matchedNames);
            row.set("matchStatus", "matched");
            row.set("persistValidFlag", 1);
            results.add(row);
        }

        JSONObject report = new JSONObject();
        report.set("results", results);
        report.set("manual", true);
        return report;
    }

    /**
     * AI 识别发票并持久化到发票台账。
     *
     * @param acceptanceId 验收单 ID（新增草稿时可能为空）
     * @param requestId    关联采购申请 ID（acceptanceId 为空时用于补关联台账，可选）
     * @param items        验收明细 JSON 数组
     * @param files        发票 PDF 文件
     * @return 带 ossId/invoiceId/invalidReason 的识别报告
     */
    @Transactional(rollbackFor = Exception.class)
    public JSONObject matchAndPersist(Long acceptanceId, Long requestId, List<Object> items, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("请先上传发票 PDF 文件");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("验收明细不能为空");
        }

        // 1. 读取文件字节（MultipartFile 只能读一次）
        List<InvoiceFile> invoiceFiles = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            try {
                invoiceFiles.add(new InvoiceFile(file.getOriginalFilename(), file.getBytes(), file.getContentType()));
            } catch (Exception e) {
                log.error("读取发票文件失败: {}", file.getOriginalFilename(), e);
                throw new IllegalStateException("读取发票文件失败: " + file.getOriginalFilename(), e);
            }
        }
        if (invoiceFiles.isEmpty()) {
            throw new IllegalArgumentException("请先上传发票 PDF 文件");
        }

        // 2. 先上传到 OSS（获得 ossId + URL）
        for (InvoiceFile invFile : invoiceFiles) {
            SysOssVo oss = uploadToOss(invFile);
            invFile.ossId = String.valueOf(oss.getOssId());
            invFile.ossUrl = oss.getUrl();
        }

        // 3. 调用 agents 识别（用字节构造新的 MultipartFile，避免原始文件被重复消费）
        List<MultipartFile> agentFiles = new ArrayList<>(invoiceFiles.size());
        for (InvoiceFile invFile : invoiceFiles) {
            agentFiles.add(new ByteArrayMultipartFile(invFile.bytes, invFile.filename, invFile.contentType));
        }
        JSONObject agentsResult = agentsInvoiceMatchService.matchInvoices(items, agentFiles);
        if (agentsResult == null) {
            throw new IllegalStateException("发票识别服务返回为空");
        }

        // 4. 关联 acceptance / request / project
        Long effRequestId = null;
        Long effProjectId = null;
        if (acceptanceId != null) {
            PmsAcceptance acceptance = acceptanceMapper.selectById(acceptanceId);
            if (acceptance != null) {
                effRequestId = acceptance.getRequestId();
                effProjectId = acceptance.getProjectId();
            }
        }
        // acceptanceId 为空（新增草稿未落库）时，用前端传的 requestId 兜底，保证台账关联不丢
        if (effRequestId == null && requestId != null) {
            effRequestId = requestId;
        }
        if (effRequestId != null) {
            PmsProcurementRequest request = requestMapper.selectById(effRequestId);
            if (request != null && effProjectId == null) {
                effProjectId = request.getProjectId();
            }
        }
        Long finalRequestId = effRequestId;
        Long finalProjectId = effProjectId;

        // 5. 持久化发票信息并回填结果
        // 申请明细 id → 验收明细 id 映射（AI 返回的 matchedItemIds 是申请明细 id，台账覆盖度按验收明细统计）
        Map<Long, Long> requestItemToAccItem = new HashMap<>();
        if (acceptanceId != null) {
            List<PmsAcceptanceItem> accItems = acceptanceItemMapper.selectList(
                Wrappers.<PmsAcceptanceItem>lambdaQuery().eq(PmsAcceptanceItem::getAcceptanceId, acceptanceId));
            for (PmsAcceptanceItem ai : accItems) {
                if (ai.getRequestItemId() != null) {
                    requestItemToAccItem.put(ai.getRequestItemId(), ai.getId());
                }
            }
        }
        JSONArray results = agentsResult.getJSONArray("results");
        if (results != null) {
            for (int i = 0; i < results.size(); i++) {
                JSONObject result = results.getJSONObject(i);
                String originalName = result.getStr("originalName");
                InvoiceFile invFile = findByName(invoiceFiles, originalName);
                PmsInvoiceInfo invoice = buildInvoiceInfo(result, acceptanceId, finalRequestId, finalProjectId, invFile);
                // 匹配命中时回填验收明细 id（多张命中取第一张，invoice_info 只有一个 acceptance_item_id 列）
                if (invoice.getAcceptanceItemId() == null && requestItemToAccItem.size() > 0) {
                    JSONArray mids = result.getJSONArray("matchedItemIds");
                    if (mids != null) {
                        for (Object m : mids) {
                            Long accItemId = requestItemToAccItem.get(Long.valueOf(m.toString()));
                            if (accItemId != null) {
                                invoice.setAcceptanceItemId(accItemId);
                                break;
                            }
                        }
                    }
                }

                // 重复检测：只与「有效」发票比较
                if (Boolean.TRUE.equals(invoice.getValidFlag())) {
                    PmsInvoiceInfo exist = invoiceInfoService.findValidByCodeAndNumber(
                        invoice.getInvoiceCode(), invoice.getInvoiceNumber());
                    if (exist != null) {
                        invoice.setValidFlag(0);
                        invoice.setInvalidReason("发票号重复，已存在发票记录（ID: " + exist.getId() + "）");
                    }
                }

                invoiceInfoService.saveOrUpdateInvoice(invoice);

                // 回填前端需要的信息
                result.set("invoiceId", invoice.getId());
                result.set("ossId", invFile != null ? invFile.ossId : null);
                result.set("ossUrl", invFile != null ? invFile.ossUrl : null);
                result.set("persistValidFlag", invoice.getValidFlag());
                if (StringUtils.isNotBlank(invoice.getInvalidReason())) {
                    result.set("invalidReason", invoice.getInvalidReason());
                }
            }
        }

        // 6. 识别报告沉淀到验收单 ai_detail（按轮次追加 JSON 数组，失败不影响主流程）
        appendAiDetail(acceptanceId, agentsResult);

        return agentsResult;
    }

    /**
     * 把本轮 AI 识别报告摘要追加写入验收单 ai_detail（JSON 数组字符串，一轮一条）。
     *
     * @param acceptanceId 验收单 ID（新增草稿未落库时为空，直接跳过）
     * @param agentsResult agents 返回的匹配报告（含 traceId/summary/results）
     */
    private void appendAiDetail(Long acceptanceId, JSONObject agentsResult) {
        if (acceptanceId == null || agentsResult == null) {
            return;
        }
        try {
            PmsAcceptance acceptance = acceptanceMapper.selectById(acceptanceId);
            if (acceptance == null) {
                return;
            }
            JSONArray rounds;
            String old = acceptance.getAiDetail();
            if (StringUtils.isNotBlank(old)) {
                try {
                    rounds = JSONUtil.parseArray(old);
                } catch (Exception e) {
                    log.warn("验收单 ai_detail 不是合法 JSON 数组，重新起记: acceptanceId={}", acceptanceId);
                    rounds = new JSONArray();
                }
            } else {
                rounds = new JSONArray();
            }

            JSONObject round = new JSONObject();
            round.set("time", LocalDateTime.now().toString());
            round.set("traceId", agentsResult.getStr("traceId"));
            round.set("summary", agentsResult.getJSONObject("summary"));
            JSONArray briefs = new JSONArray();
            JSONArray results = agentsResult.getJSONArray("results");
            if (results != null) {
                for (int i = 0; i < results.size(); i++) {
                    JSONObject r = results.getJSONObject(i);
                    if (r == null) {
                        continue;
                    }
                    JSONObject brief = new JSONObject();
                    brief.set("originalName", r.getStr("originalName"));
                    brief.set("matchStatus", r.getStr("matchStatus"));
                    brief.set("matchedItemNames", r.getJSONArray("matchedItemNames"));
                    brief.set("invalidReason", r.getStr("invalidReason"));
                    brief.set("aiConfidence", r.getObj("aiConfidence"));
                    briefs.add(brief);
                }
            }
            round.set("results", briefs);
            rounds.add(round);

            LambdaUpdateWrapper<PmsAcceptance> uw = Wrappers.lambdaUpdate();
            uw.eq(PmsAcceptance::getId, acceptanceId)
                .set(PmsAcceptance::getAiDetail, rounds.toString());
            acceptanceMapper.update(null, uw);
        } catch (Exception e) {
            log.error("写入验收单 ai_detail 留痕失败: acceptanceId={}", acceptanceId, e);
        }
    }

    private SysOssVo uploadToOss(InvoiceFile invFile) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("invoice_", "_" + (invFile.filename != null ? invFile.filename : ".pdf"));
            Files.write(tempFile.toPath(), invFile.bytes);
            SysOssExt ext = new SysOssExt();
            ext.setBizType("procurement_invoice");
            ext.setContentType(invFile.contentType);
            ext.setSource("ai_invoice_match");
            return sysOssService.upload(tempFile, ext);
        } catch (Exception e) {
            log.error("上传发票到 OSS 失败: {}", invFile.filename, e);
            throw new IllegalStateException("上传发票到 OSS 失败: " + invFile.filename, e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private InvoiceFile findByName(List<InvoiceFile> files, String originalName) {
        if (StringUtils.isBlank(originalName)) {
            return null;
        }
        return files.stream()
            .filter(f -> originalName.equals(f.filename))
            .findFirst()
            .orElse(null);
    }

    private PmsInvoiceInfo buildInvoiceInfo(JSONObject result, Long acceptanceId, Long requestId, Long projectId, InvoiceFile invFile) {
        PmsInvoiceInfo invoice = new PmsInvoiceInfo();
        invoice.setAcceptanceId(acceptanceId);
        invoice.setRequestId(requestId);
        invoice.setProjectId(projectId);
        if (invFile != null) {
            invoice.setPdfUrl(invFile.ossUrl);
            invoice.setPdfOssId(invFile.ossId);
        }

        JSONObject extracted = result.getJSONObject("extracted");
        if (extracted != null) {
            invoice.setInvoiceCode(extracted.getStr("invoice_code"));
            invoice.setInvoiceNumber(extracted.getStr("invoice_number"));
            invoice.setInvoiceType(extracted.getStr("invoice_type"));
            invoice.setSellerName(extracted.getStr("seller_name"));
            invoice.setBuyerName(extracted.getStr("buyer_name"));
            invoice.setTotalAmount(toBigDecimal(extracted.get("total_amount")));
            invoice.setTaxAmount(toBigDecimal(extracted.get("tax_amount")));
            invoice.setAmount(toBigDecimal(extracted.get("amount_without_tax")));
            invoice.setRedFlag(extracted.getBool("is_red_invoice") ? 1 : 0);
            String dateStr = extracted.getStr("invoice_date");
            if (StringUtils.isNotBlank(dateStr)) {
                try {
                    invoice.setInvoiceDate(DateUtil.parse(dateStr, "yyyy-MM-dd"));
                } catch (Exception e) {
                    log.warn("发票日期解析失败: {}", dateStr);
                }
            }
            invoice.setOcrJson(extracted.toString());
        }

        // 匹配到的商品名落库（台账展示用）
        JSONArray nameArr = result.getJSONArray("matchedItemNames");
        if (nameArr != null && !nameArr.isEmpty()) {
            List<String> matchedNames = new ArrayList<>();
            for (Object o : nameArr) {
                if (o != null && StringUtils.isNotBlank(o.toString())) {
                    matchedNames.add(o.toString());
                }
            }
            if (CollUtil.isNotEmpty(matchedNames)) {
                invoice.setMatchedItems(String.join(",", matchedNames));
            }
        }

        String matchStatus = result.getStr("matchStatus");
        if ("matched".equals(matchStatus)) {
            invoice.setValidFlag(1);
        } else if ("external".equals(matchStatus)) {
            invoice.setValidFlag(0);
            invoice.setInvalidReason("未匹配到本订单商品");
        } else {
            invoice.setValidFlag(0);
            invoice.setInvalidReason("AI 识别失败或未能匹配");
        }

        invoice.setStatus("submitted");
        invoice.setVerifyStatus("unverified");
        return invoice;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(Convert.toStr(value));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 过滤文件名非法字符（/\:*?"&lt;&gt;|），并把连续空白折叠为单个空格
     */
    private String sanitizeFileName(String name) {
        if (name == null) {
            return null;
        }
        String cleaned = name.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static class InvoiceFile {
        String filename;
        final byte[] bytes;
        final String contentType;
        String ossId;
        String ossUrl;

        InvoiceFile(String filename, byte[] bytes, String contentType) {
            this.filename = filename;
            this.bytes = bytes;
            this.contentType = contentType;
        }
    }
}
