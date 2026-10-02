package org.dromara.procurement.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.procurement.domain.PmsAcceptance;
import org.dromara.procurement.domain.PmsAcceptanceItem;
import org.dromara.procurement.domain.PmsInvoiceInfo;
import org.dromara.procurement.domain.PmsProcurementRequest;
import org.dromara.procurement.domain.PmsReimbursement;
import org.dromara.procurement.mapper.PmsAcceptanceItemMapper;
import org.dromara.procurement.mapper.PmsAcceptanceMapper;
import org.dromara.procurement.mapper.PmsInvoiceInfoMapper;
import org.dromara.procurement.mapper.PmsProcurementRequestMapper;
import org.dromara.procurement.service.IPmsProcurementRequestService;
import org.dromara.procurement.service.IPmsReimbursementService;
import org.dromara.system.domain.SysOss;
import org.dromara.system.domain.SysOssExt;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.mapper.SysOssMapper;
import org.dromara.system.service.ISysOssService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 采购管理-报销打包服务。
 *
 * <p>报销包 = 采购申请 Excel + 验收图片/ + 发票pdf/ + 清单.txt，打包为 ZIP 上传 MinIO，
 * 回写 file_url/content_json，status: packing → packed。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PmsReimbursementPackService {

    private final IPmsReimbursementService reimbursementService;
    private final PmsAcceptanceMapper acceptanceMapper;
    private final PmsAcceptanceItemMapper acceptanceItemMapper;
    private final PmsProcurementRequestMapper requestMapper;
    private final PmsInvoiceInfoMapper invoiceInfoMapper;
    private final ISysOssService sysOssService;
    private final SysOssMapper sysOssMapper;
    private final IPmsProcurementRequestService requestService;

    /**
     * 为一条报销记录生成报销包（同步打包 → MinIO → 回写）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void pack(Long reimbursementId) {
        PmsReimbursement reimb = reimbursementService.queryByIdRaw(reimbursementId);
        if (reimb == null) {
            throw new ServiceException("报销记录不存在");
        }
        if (reimb.getRequestId() == null) {
            throw new ServiceException("报销记录未关联采购申请");
        }
        PmsProcurementRequest request = requestMapper.selectById(reimb.getRequestId());
        if (request == null) {
            throw new ServiceException("关联采购申请不存在");
        }

        // 验收单 + 明细（必须已验收，否则没有材料可打）
        PmsAcceptance acceptance = findFinishedAcceptance(reimb.getRequestId(), reimb.getAcceptanceId());
        if (acceptance == null) {
            throw new ServiceException("该采购申请尚无已完成的验收单，无法生成报销包");
        }
        List<PmsAcceptanceItem> items = acceptanceItemMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PmsAcceptanceItem>()
                .eq(PmsAcceptanceItem::getAcceptanceId, acceptance.getId())
                .orderByAsc(PmsAcceptanceItem::getId));
        if (items.isEmpty()) {
            throw new ServiceException("验收单内无商品明细，无法生成报销包");
        }

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("reimb_" + reimbursementId + "_");

            // 1. 采购申请 Excel（复用模板导出，文件名 = 标题 + .xlsx）
            byte[] excel = requestService.buildFormExcelBytes(reimb.getRequestId());
            String excelName = sanitizeFileName(request.getTitle()) + ".xlsx";
            Files.write(workDir.resolve(excelName), excel);

            // 2. 验收图片/ + 发票pdf/（按明细商品名命名，序号对应）
            Path photoDir = workDir.resolve("验收图片");
            Path invoiceDir = workDir.resolve("发票pdf");
            Files.createDirectories(photoDir);
            Files.createDirectories(invoiceDir);
            List<String> manifest = new ArrayList<>();
            AtomicInteger seq = new AtomicInteger(1);
            for (PmsAcceptanceItem item : items) {
                int no = seq.getAndIncrement();
                String base = no + "_" + sanitizeFileName(
                    StringUtils.isNotBlank(item.getItemName()) ? item.getItemName() : "明细" + no);
                manifest.add(no + ". " + (item.getItemName() == null ? "" : item.getItemName()));
                if (StringUtils.isNotBlank(item.getPhotoUrl())) {
                    String saved = downloadToDir(item.getPhotoUrl(), photoDir, base + "_验收图");
                    manifest.add(saved == null
                        ? "   验收图片: （附件读取失败，已跳过）"
                        : "   验收图片: 验收图片/" + saved);
                } else {
                    manifest.add("   验收图片: （未上传）");
                }
                if (StringUtils.isNotBlank(item.getInvoiceUrl())) {
                    String saved = downloadToDir(item.getInvoiceUrl(), invoiceDir, base + "_发票");
                    manifest.add(saved == null
                        ? "   发票: （附件读取失败，已跳过）"
                        : "   发票: 发票pdf/" + saved);
                } else {
                    manifest.add("   发票: （未上传）");
                }
            }

            // 3. 清单.txt（包内文件 ↔ 明细对应关系）
            StringBuilder sb = new StringBuilder();
            sb.append("报销包清单\n");
            sb.append("报销编号: ").append(reimb.getReimbursementCode()).append("\n");
            sb.append("采购申请: ").append(request.getTitle()).append("（").append(request.getRequestCode()).append("）\n");
            sb.append("生成时间: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
            sb.append("\n商品明细对应关系:\n");
            manifest.forEach(line -> sb.append(line).append("\n"));
            Files.writeString(workDir.resolve("清单.txt"), sb.toString());

            // 4. 打 ZIP → MinIO → 回写
            String zipBaseName = sanitizeFileName(request.getTitle()) + "_" + reimb.getReimbursementCode();
            File zipFile = File.createTempFile("reimb_", "_" + zipBaseName + ".zip");
            try {
                zipDir(workDir, zipFile);
                SysOssExt ext = new SysOssExt();
                ext.setBizType("procurement_reimbursement");
                ext.setContentType("application/zip");
                ext.setSource("reimbursement_pack");
                SysOssVo oss = sysOssService.upload(zipFile, ext);

                // 存 ossId 而非 URL：下载接口按 ossId 取流，URL 形式无法稳定反解（MinIO 文件名为 UUID）
                reimb.setFileUrl(String.valueOf(oss.getOssId()));
                reimb.setStatus("packed");
                java.util.Map<String, Object> content = new java.util.LinkedHashMap<>();
                content.put("ossId", String.valueOf(oss.getOssId()));
                content.put("excel", excelName);
                content.put("photoCount", items.stream().filter(i -> StringUtils.isNotBlank(i.getPhotoUrl())).count());
                content.put("invoiceCount", items.stream().filter(i -> StringUtils.isNotBlank(i.getInvoiceUrl())).count());
                content.put("itemCount", items.size());
                content.put("manifest", manifest);
                reimb.setContentJson(cn.hutool.json.JSONUtil.toJsonStr(content));
                reimbursementService.updateByRaw(reimb);
                log.info("报销包已生成: reimbursementId={}, zip={}", reimbursementId, zipBaseName);
            } finally {
                zipFile.delete();
            }
        } catch (IOException e) {
            log.error("报销包生成失败: reimbursementId={}", reimbursementId, e);
            throw new ServiceException("报销包生成失败：" + e.getMessage());
        } finally {
            if (workDir != null) {
                FileUtil.del(workDir.toFile());
            }
        }
    }

    /**
     * 导出前提醒文本：该采购申请的发票对应情况（验收单 AI 识别摘要 + 发票台账），只读不影响导出。
     */
    public String buildInvoiceTxt(Long requestId) {
        StringBuilder sb = new StringBuilder();
        sb.append("发票对应情况提醒\n");
        PmsProcurementRequest request = requestId != null ? requestMapper.selectById(requestId) : null;
        if (request != null) {
            sb.append("采购申请: ").append(request.getTitle())
                .append("（").append(request.getRequestCode()).append("）\n");
        }
        PmsAcceptance acceptance = requestId != null ? findFinishedAcceptance(requestId, null) : null;
        if (acceptance != null) {
            sb.append("验收单号: ").append(acceptance.getAcceptanceCode()).append("\n");
        }
        List<PmsAcceptanceItem> items = new ArrayList<>();
        java.util.Map<Long, String> itemNames = new java.util.HashMap<>();
        if (acceptance != null) {
            items = acceptanceItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PmsAcceptanceItem>()
                    .eq(PmsAcceptanceItem::getAcceptanceId, acceptance.getId())
                    .orderByAsc(PmsAcceptanceItem::getId));
            for (PmsAcceptanceItem item : items) {
                if (item.getId() != null) {
                    itemNames.put(item.getId(), item.getItemName());
                }
            }
        }

        // 1. AI 识别对应摘要（ai_detail 按轮次追加，取最新一轮 summary.lines）
        boolean hasAi = false;
        if (acceptance != null && StringUtils.isNotBlank(acceptance.getAiDetail())) {
            try {
                JSONArray rounds = JSONUtil.parseArray(acceptance.getAiDetail());
                if (!rounds.isEmpty()) {
                    JSONObject summary = rounds.getJSONObject(rounds.size() - 1).getJSONObject("summary");
                    if (summary != null) {
                        sb.append("\n【AI 发票识别对应结果】\n");
                        if (summary.getStr("matchedInvoiceCount") != null) {
                            sb.append("匹配发票 ").append(summary.getStr("matchedInvoiceCount"))
                                .append(" 张，匹配商品 ").append(summary.getStr("matchedItemCount"))
                                .append(" 项，不属于本订单 ").append(summary.getStr("externalInvoiceCount"))
                                .append(" 张\n");
                        }
                        JSONArray lines = summary.getJSONArray("lines");
                        if (lines != null) {
                            for (int i = 0; i < lines.size(); i++) {
                                sb.append(lines.getStr(i)).append("\n");
                            }
                        }
                        hasAi = true;
                    }
                }
            } catch (Exception e) {
                log.warn("验收单 ai_detail 解析失败，跳过 AI 摘要: acceptanceId={}", acceptance.getId(), e);
            }
        }

        // 2. 发票台账（invoice_info 当前有效数据）
        List<PmsInvoiceInfo> invoices = new ArrayList<>();
        if (requestId != null) {
            invoices = invoiceInfoMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PmsInvoiceInfo>()
                    .eq(PmsInvoiceInfo::getRequestId, requestId)
                    .orderByAsc(PmsInvoiceInfo::getId));
        }
        if (!invoices.isEmpty()) {
            long validCount = invoices.stream().filter(i -> i.getValidFlag() != null && i.getValidFlag() == 1).count();
            sb.append("\n【发票台账】共 ").append(invoices.size())
                .append(" 张（有效 ").append(validCount)
                .append(" / 无效 ").append(invoices.size() - validCount).append("）\n");
            int no = 1;
            for (PmsInvoiceInfo inv : invoices) {
                boolean valid = inv.getValidFlag() != null && inv.getValidFlag() == 1;
                sb.append("  ").append(no++).append(". ").append(nullToDash(inv.getSellerName()))
                    .append("  价税合计 ").append(inv.getTotalAmount() == null ? "-" : inv.getTotalAmount().toPlainString())
                    .append(" 元  票号 ").append(StringUtils.isNotBlank(inv.getInvoiceNumber()) ? inv.getInvoiceNumber() : nullToDash(inv.getInvoiceCode()));
                if (valid) {
                    String matched = StringUtils.isNotBlank(inv.getMatchedItems())
                        ? inv.getMatchedItems()
                        : (inv.getAcceptanceItemId() != null ? nullToDash(itemNames.get(inv.getAcceptanceItemId())) : "-");
                    sb.append("  → 对应商品: ").append(matched);
                } else {
                    sb.append("  无效原因: ").append(nullToDash(inv.getInvalidReason()));
                }
                sb.append("\n");
            }
        } else if (!hasAi) {
            sb.append("\n未查询到该申请的发票识别记录与发票台账，请确认发票已上传并完成 AI 匹配。\n");
        }

        // 3. 尚无发票对应的验收明细
        if (!items.isEmpty()) {
            Set<Long> matchedItemIds = new HashSet<>();
            for (PmsInvoiceInfo inv : invoices) {
                if (inv.getValidFlag() != null && inv.getValidFlag() == 1 && inv.getAcceptanceItemId() != null) {
                    matchedItemIds.add(inv.getAcceptanceItemId());
                }
            }
            List<String> missing = new ArrayList<>();
            for (PmsAcceptanceItem item : items) {
                if (item.getId() != null && !matchedItemIds.contains(item.getId())) {
                    missing.add(item.getItemName());
                }
            }
            if (!missing.isEmpty()) {
                sb.append("\n【尚无发票对应的验收明细】\n");
                for (String name : missing) {
                    sb.append("  - ").append(name).append("\n");
                }
            }
        }

        sb.append("\n提示: 以上内容仅为导出前核对提醒，不影响导出；报销包内「清单.txt」为打包时的文件对应关系。\n");
        return sb.toString();
    }

    private String nullToDash(String s) {
        return StringUtils.isBlank(s) ? "-" : s;
    }

    /**
     * 查找采购申请"已验收完成"的验收单：优先 reimbursement.acceptance_id，否则取该申请最新 finish 验收单
     */
    private PmsAcceptance findFinishedAcceptance(Long requestId, Long acceptanceId) {
        if (acceptanceId != null) {
            PmsAcceptance acc = acceptanceMapper.selectById(acceptanceId);
            if (acc != null && "finish".equals(acc.getStatus())) {
                return acc;
            }
        }
        List<PmsAcceptance> list = acceptanceMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PmsAcceptance>()
                .eq(PmsAcceptance::getRequestId, requestId)
                .eq(PmsAcceptance::getStatus, "finish")
                .orderByDesc(PmsAcceptance::getId)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 递归打包目录为 ZIP（java.util.zip 标准库：UTF-8 文件名编码 + 保留目录结构，
     * Hutool ZipUtil 会压平目录且中文文件名乱码）
     */
    private void zipDir(Path dir, File zipFile) throws IOException {
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
            Files.newOutputStream(zipFile.toPath()), StandardCharsets.UTF_8)) {
            Files.walk(dir).filter(Files::isRegularFile).forEach(path -> {
                String entryName = dir.relativize(path).toString().replace('\\', '/');
                try {
                    zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                    Files.copy(path, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    /**
     * 下载附件到目录，返回实际落盘文件名（保留原后缀，按商品基名重命名）。
     * 单个附件读取失败不阻断整包：返回 null，由调用方在清单中注明。
     */
    private String downloadToDir(String urlOrOssId, Path dir, String baseName) {
        byte[] data;
        try {
            data = sysOssService.download(resolveOssId(urlOrOssId)).getBody();
        } catch (Exception e) {
            log.warn("报销包附件下载失败，已跳过: {}", urlOrOssId, e);
            return null;
        }
        if (data == null || data.length == 0) {
            log.warn("报销包附件内容为空，已跳过: {}", urlOrOssId);
            return null;
        }
        String filename = baseName + resolveSuffix(urlOrOssId);
        try {
            Files.write(dir.resolve(filename), data);
        } catch (IOException e) {
            log.warn("报销包附件写入失败，已跳过: {}", urlOrOssId, e);
            return null;
        }
        return filename;
    }

    /**
     * 供下载接口使用：从 file_url 解析 MinIO ossId
     */
    public Long resolveOssIdFromUrl(String urlOrOssId) {
        return resolveOssId(urlOrOssId);
    }

    /**
     * photo_url/invoice_url/file_url 存的是 ossId（字符串）。
     * 兼容历史误存 URL 的情况：先查 sys_oss 反查，再退化为 URL 数字段正则。
     */
    private Long resolveOssId(String urlOrOssId) {
        if (StringUtils.isBlank(urlOrOssId)) {
            throw new ServiceException("附件标识为空");
        }
        String s = urlOrOssId.trim();
        if (s.matches("\\d+")) {
            return Long.valueOf(s);
        }
        // URL 形式：按 url 在 sys_oss 反查 ossId（MinIO 文件名是 UUID，正则取不出数字段）
        SysOss byUrl = sysOssMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysOss>()
                .eq(SysOss::getUrl, s).last("LIMIT 1"));
        if (byUrl != null) {
            return byUrl.getOssId();
        }
        // 兜底：/ruoyi/2026/09/02/<ossId>.pdf 取文件名数字段
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{6,})\\.[a-zA-Z0-9]+$").matcher(s);
        if (m.find()) {
            return Long.valueOf(m.group(1));
        }
        throw new ServiceException("无法解析附件 ossId: " + urlOrOssId);
    }

    /**
     * 从 sys_oss 查真实后缀；查不到再从字符串猜，最后兜底 .bin
     */
    private String resolveSuffix(String urlOrOssId) {
        String s = urlOrOssId.trim();
        if (s.matches("\\d+")) {
            try {
                SysOssVo oss = sysOssService.getById(Long.valueOf(s));
                if (oss != null && StringUtils.isNotBlank(oss.getFileSuffix())) {
                    return oss.getFileSuffix();
                }
            } catch (Exception ignored) {
            }
        }
        int dot = s.lastIndexOf('.');
        if (dot >= 0 && dot > s.lastIndexOf('/')) {
            String suffix = s.substring(dot);
            if (suffix.matches("\\.[a-zA-Z0-9]{1,8}")) {
                return suffix;
            }
        }
        return ".bin";
    }

    private String sanitizeFileName(String name) {
        if (StringUtils.isBlank(name)) {
            return "未命名";
        }
        return name.replaceAll("[\\\\/:*?\"<>|,]", "_").trim();
    }
}
