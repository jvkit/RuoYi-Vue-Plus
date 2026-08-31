package org.dromara.procurement.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * agents 智能体服务转发：发票批量识别 + 匹配。
 *
 * <p>RuoYi 只做鉴权（Sa-Token）与转发，AI 认知任务全部交给独立的 agents 服务。
 * agents 返回统一的匹配报告，RuoYi 原样透传，不在此做业务判断。</p>
 */
@Slf4j
@Service
public class AgentsInvoiceMatchService {

    @Value("${agents.invoice-match-url:http://127.0.0.1:8093/invoice/match}")
    private String invoiceMatchUrl;

    /**
     * 批量发票识别 + 匹配。
     *
     * @param items 验收明细 JSON（id/itemName/spec/applyPrice/quantity 等）
     * @param files 发票 PDF 文件
     * @return agents 返回的匹配报告（JSON 对象）
     */
    public JSONObject matchInvoices(List<Object> items, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("请先上传发票 PDF 文件");
        }
        if (StrUtil.isBlank(invoiceMatchUrl)) {
            throw new IllegalStateException("agents 服务地址未配置（agents.invoice-match-url）");
        }

        // Hutool 的 form 方法会自动用 multipart/form-data 编码
        HttpRequest request = HttpRequest.post(invoiceMatchUrl)
            .timeout(300_000)  // 批量 + 并发识别，放宽到 5 分钟
            .form("items", JSONUtil.toJsonStr(items));

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            try {
                request.form("files", file.getBytes(), file.getOriginalFilename());
            } catch (Exception e) {
                log.error("读取发票文件失败: {}", file.getOriginalFilename(), e);
                throw new IllegalStateException("读取发票文件失败: " + file.getOriginalFilename(), e);
            }
        }

        try (HttpResponse response = request.execute()) {
            String body = response.body();
            if (!response.isOk()) {
                log.error("agents 发票匹配失败，status={}, body={}", response.getStatus(), body);
                throw new IllegalStateException("发票识别服务异常，请稍后重试");
            }
            JSONObject json = JSONUtil.parseObj(body);
            if (json.getInt("code", 200) != 200) {
                throw new IllegalStateException(json.getStr("detail", "发票识别服务返回异常"));
            }
            return json.getJSONObject("data");
        } catch (Exception e) {
            log.error("调用 agents 发票匹配失败: {}", e.getMessage(), e);
            throw new IllegalStateException("发票识别服务调用失败: " + e.getMessage(), e);
        }
    }
}
