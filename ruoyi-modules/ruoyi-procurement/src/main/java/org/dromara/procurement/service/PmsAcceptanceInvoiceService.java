package org.dromara.procurement.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.procurement.domain.PmsAcceptance;
import org.dromara.procurement.domain.PmsInvoiceInfo;
import org.dromara.procurement.domain.PmsProcurementRequest;
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
import java.util.ArrayList;
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
        JSONArray results = agentsResult.getJSONArray("results");
        if (results != null) {
            for (int i = 0; i < results.size(); i++) {
                JSONObject result = results.getJSONObject(i);
                String originalName = result.getStr("originalName");
                InvoiceFile invFile = findByName(invoiceFiles, originalName);
                PmsInvoiceInfo invoice = buildInvoiceInfo(result, acceptanceId, finalRequestId, finalProjectId, invFile);

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

        return agentsResult;
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

    private static class InvoiceFile {
        final String filename;
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
