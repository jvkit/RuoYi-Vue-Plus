package org.dromara.invoice.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dify发票AI审核服务
 * 调用Dify API对发票进行智能识别和审核
 */
@Slf4j
@Service
public class DifyInvoiceReviewService {

    @Value("${dify.api-url:http://172.16.16.110:8090}")
    private String difyApiUrl;

    @Value("${dify.app-id:}")
    private String difyAppId;

    @Value("${dify.api-key:}")
    private String difyApiKey;

    /**
     * 上传发票图片到Dify并提取字段+审核
     *
     * @param imageFile 发票图片
     * @return 提取结果+审核结果
     */
    public ExtractResult extractAndReviewFromImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return ExtractResult.error("请先上传发票图片");
        }

        try {
            // 1. 上传图片到Dify
            String uploadFileId = uploadImageToDify(imageFile);
            if (uploadFileId == null) {
                return ExtractResult.error("图片上传失败，无法调用AI识别");
            }

            // 2. 调用Dify进行识别和审核
            String prompt = """
                你是一名财务审核助手。请识别上传的发票图片，并按以下规则判断：
                1. 从图片中提取：发票代码、发票号码、发票类型（normal普票/special专票/electronic电子发票）、不含税金额、税额、价税合计、开票日期、销售方名称、购买方名称。
                2. 判断发票是否合规：发票代码和号码是否完整规范；金额逻辑是否正确（价税合计 = 不含税金额 + 税额）；销售方是否合规（不能是个人、小卖部、便利店等）；开票日期是否合理。
                3. 必须严格按以下JSON格式返回，不要添加任何额外说明：
                {
                  "invoiceCode": "发票代码，未识别填null",
                  "invoiceNumber": "发票号码，未识别填null",
                  "invoiceType": "normal 或 special 或 electronic，未识别填null",
                  "amount": "不含税金额数字字符串，未识别填null",
                  "taxAmount": "税额数字字符串，未识别填null",
                  "totalAmount": "价税合计数字字符串，未识别填null",
                  "invoiceDate": "开票日期yyyy-MM-dd，未识别填null",
                  "sellerName": "销售方名称，未识别填null",
                  "buyerName": "购买方名称，未识别填null",
                  "passed": true或false,
                  "opinion": "详细的通过或驳回意见"
                }
                """;

            String response = callDifyChat(prompt, uploadFileId);
            return parseExtractResponse(response);
        } catch (Exception e) {
            log.error("Failed to extract invoice from image", e);
            return ExtractResult.error("AI识别失败：" + e.getMessage());
        }
    }

    /**
     * 基于已有字段进行AI审核（旧接口兼容）
     */
    public ReviewResult reviewInvoice(String invoiceCode, String invoiceNumber, String invoiceType,
                                       String amount, String taxAmount, String totalAmount,
                                       String invoiceDate, String sellerName, String buyerName,
                                       String imageBase64) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("请审核以下发票信息，并给出明确的通过或驳回结论：\n");
        queryBuilder.append("发票代码：").append(invoiceCode != null ? invoiceCode : "未填写").append("\n");
        queryBuilder.append("发票号码：").append(invoiceNumber != null ? invoiceNumber : "未填写").append("\n");
        queryBuilder.append("发票类型：").append(invoiceType != null ? invoiceType : "未填写").append("\n");
        queryBuilder.append("不含税金额：").append(amount != null ? amount : "未填写").append("\n");
        queryBuilder.append("税额：").append(taxAmount != null ? taxAmount : "未填写").append("\n");
        queryBuilder.append("价税合计：").append(totalAmount != null ? totalAmount : "未填写").append("\n");
        queryBuilder.append("开票日期：").append(invoiceDate != null ? invoiceDate : "未填写").append("\n");
        queryBuilder.append("销售方名称：").append(sellerName != null ? sellerName : "未填写").append("\n");
        queryBuilder.append("购买方名称：").append(buyerName != null ? buyerName : "未填写");

        if (imageBase64 != null && !imageBase64.isEmpty()) {
            queryBuilder.append("\n\n发票图片已提供，请结合图片内容进行审核。");
        }

        // 真正把图片上传到 Dify 并随请求发送（此前只追加了文字提示，图片从未被发送）
        String uploadFileId = null;
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(imageBase64);
                uploadFileId = uploadImageToDifyBytes(bytes);
                if (uploadFileId == null) {
                    log.warn("Failed to upload review image to Dify, will review by text fields only");
                }
            } catch (Exception e) {
                log.warn("Failed to decode/upload review image", e);
            }
        }

        String response = callDifyChat(queryBuilder.toString(), uploadFileId);
        return parseReviewResponse(response);
    }

    /**
     * 上传字节数组图片到Dify获取upload_file_id
     */
    private String uploadImageToDifyBytes(byte[] bytes) {
        try {
            String url = difyApiUrl + "/v1/files/upload";
            HttpResponse response = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + difyApiKey)
                    .form("file", bytes, "invoice_review.png")
                    .form("user", "invoice-system")
                    .timeout(60000)
                    .execute();
            String body = response.body();
            log.debug("Dify upload bytes response: {}", body);
            var json = JSONUtil.parseObj(body);
            return json.getStr("id");
        } catch (Exception e) {
            log.error("Failed to upload image bytes to Dify", e);
            return null;
        }
    }

    /**
     * 上传图片到Dify获取upload_file_id
     */
    private String uploadImageToDify(MultipartFile imageFile) {
        try {
            String url = difyApiUrl + "/v1/files/upload";
            log.info("Uploading image to Dify: {}", url);

            HttpResponse response = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + difyApiKey)
                    .form("file", imageFile.getBytes(), imageFile.getOriginalFilename())
                    .form("user", "invoice-system")
                    .timeout(60000)
                    .execute();

            String body = response.body();
            log.debug("Dify upload response: {}", body);

            var json = JSONUtil.parseObj(body);
            return json.getStr("id");
        } catch (Exception e) {
            log.error("Failed to upload image to Dify", e);
            return null;
        }
    }

    /**
     * 调用Dify chat-messages接口
     */
    private String callDifyChat(String query, String uploadFileId) {
        Map<String, Object> body = new HashMap<>();
        body.put("inputs", new HashMap<>());
        body.put("query", query);
        body.put("response_mode", "blocking");
        body.put("user", "invoice-system");

        if (uploadFileId != null && !uploadFileId.isEmpty()) {
            Map<String, Object> fileObj = new HashMap<>();
            fileObj.put("type", "image");
            fileObj.put("transfer_method", "local_file");
            fileObj.put("upload_file_id", uploadFileId);
            body.put("files", List.of(fileObj));
        }

        String jsonBody = JSONUtil.toJsonStr(body);
        log.info("Calling Dify chat-messages API");

        String response = HttpRequest.post(difyApiUrl + "/v1/chat-messages")
                .header("Authorization", "Bearer " + difyApiKey)
                .header("Content-Type", "application/json")
                .body(jsonBody)
                .timeout(120000)
                .execute()
                .body();

        log.debug("Dify chat response: {}", response);
        return response;
    }

    /**
     * 解析图片识别+审核的JSON响应
     */
    private ExtractResult parseExtractResponse(String response) {
        try {
            var json = JSONUtil.parseObj(response);
            String answer = json.getStr("answer");

            // Dify 返回错误时透出真实错误信息，便于定位（如模型不支持视觉/多模态）
            if (answer == null || answer.isEmpty()) {
                String errorMsg = extractDifyError(response);
                if (errorMsg != null) {
                    log.warn("Dify returned error while extracting invoice: {}", errorMsg);
                    return ExtractResult.error("AI识别失败：" + errorMsg);
                }
                return ExtractResult.error("AI返回格式异常，未能识别发票");
            }

            // 尝试从answer中提取JSON
            String jsonStr = extractJsonFromAnswer(answer);
            if (jsonStr == null) {
                log.warn("Could not extract JSON from Dify answer: {}", answer);
                return ExtractResult.error("AI返回格式异常，未能识别发票");
            }

            var resultJson = JSONUtil.parseObj(jsonStr);
            // 兼容AI返回的 snake_case / 下划线字段
            normalizeExtractFields(resultJson);

            ExtractResult result = new ExtractResult();
            result.setSuccess(true);
            result.setInvoiceCode(getStringOrNull(resultJson, "invoiceCode"));
            result.setInvoiceNumber(getStringOrNull(resultJson, "invoiceNumber"));
            result.setInvoiceType(getStringOrNull(resultJson, "invoiceType"));
            result.setAmount(getBigDecimalOrNull(resultJson, "amount"));
            result.setTaxAmount(getBigDecimalOrNull(resultJson, "taxAmount"));
            result.setTotalAmount(getBigDecimalOrNull(resultJson, "totalAmount"));
            result.setInvoiceDate(getStringOrNull(resultJson, "invoiceDate"));
            result.setSellerName(getStringOrNull(resultJson, "sellerName"));
            result.setBuyerName(getStringOrNull(resultJson, "buyerName"));
            result.setPassed(resultJson.getBool("passed", true));
            result.setOpinion(resultJson.getStr("opinion", ""));
            result.setRawAnswer(answer);

            return result;
        } catch (Exception e) {
            log.error("Failed to parse Dify extract response: {}", response, e);
            return ExtractResult.error("解析AI识别结果失败：" + e.getMessage());
        }
    }

    /**
     * 将AI返回的各种字段名统一为驼峰命名
     */
    private void normalizeExtractFields(cn.hutool.json.JSONObject resultJson) {
        Map<String, String> fieldMap = Map.of(
            "invoice_code", "invoiceCode",
            "invoice_number", "invoiceNumber",
            "invoice_type", "invoiceType",
            "type", "invoiceType",
            "tax_amount", "taxAmount",
            "total_amount", "totalAmount",
            "invoice_date", "invoiceDate",
            "date", "invoiceDate",
            "seller_name", "sellerName",
            "buyer_name", "buyerName"
        );
        for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
            if (resultJson.containsKey(entry.getKey()) && !resultJson.containsKey(entry.getValue())) {
                resultJson.set(entry.getValue(), resultJson.get(entry.getKey()));
            }
        }
    }

    /**
     * 解析普通审核响应
     */
    private ReviewResult parseReviewResponse(String response) {
        try {
            var json = JSONUtil.parseObj(response);
            String answer = json.getStr("answer");

            // Dify 返回错误时透出真实错误信息
            if (answer == null || answer.isEmpty()) {
                String errorMsg = extractDifyError(response);
                if (errorMsg != null) {
                    log.warn("Dify returned error while reviewing invoice: {}", errorMsg);
                    ReviewResult r = new ReviewResult();
                    r.setPassed(false);
                    r.setOpinion("AI审核失败：" + errorMsg);
                    r.setRawAnswer(response);
                    return r;
                }
            }

            ReviewResult result = new ReviewResult();
            result.setRawAnswer(answer);
            result.setPassed(!answer.contains("驳回") && !answer.contains("不通过") && !answer.contains("拒绝"));
            result.setOpinion(answer);
            return result;
        } catch (Exception e) {
            log.error("Failed to parse Dify response: {}", response, e);
            return ReviewResult.defaultPass();
        }
    }

    /**
     * 从Dify响应中提取错误信息（message / error.message 等字段）
     */
    private String extractDifyError(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        try {
            var json = JSONUtil.parseObj(response);
            String message = json.getStr("message");
            if (message == null || message.isEmpty()) {
                var error = json.getJSONObject("error");
                if (error != null) {
                    message = error.getStr("message");
                }
            }
            return message;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从AI回答中提取JSON字符串
     */
    private String extractJsonFromAnswer(String answer) {
        if (answer == null || answer.isEmpty()) {
            return null;
        }

        // 先尝试找 ```json ... ``` 代码块
        Pattern codeBlockPattern = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```");
        Matcher matcher = codeBlockPattern.matcher(answer);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 再尝试找 ``` ... ```
        codeBlockPattern = Pattern.compile("```\\s*([\\s\\S]*?)\\s*```");
        matcher = codeBlockPattern.matcher(answer);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 最后尝试找第一个 { 到最后一个 }
        int start = answer.indexOf('{');
        int end = answer.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return answer.substring(start, end + 1);
        }

        return null;
    }

    private String getStringOrNull(cn.hutool.json.JSONObject json, String key) {
        String value = json.getStr(key);
        if (value == null || value.isEmpty() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    private BigDecimal getBigDecimalOrNull(cn.hutool.json.JSONObject json, String key) {
        String value = json.getStr(key);
        if (value == null || value.isEmpty() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "").replace("¥", "").replace("￥", "").trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse decimal for key {}: {}", key, value);
            return null;
        }
    }

    /**
     * 提取识别结果
     */
    @Data
    public static class ExtractResult {
        private boolean success;
        private String errorMsg;

        private String invoiceCode;
        private String invoiceNumber;
        private String invoiceType;
        private BigDecimal amount;
        private BigDecimal taxAmount;
        private BigDecimal totalAmount;
        private String invoiceDate;
        private String sellerName;
        private String buyerName;

        private boolean passed;
        private String opinion;
        private String rawAnswer;

        public static ExtractResult error(String msg) {
            ExtractResult r = new ExtractResult();
            r.setSuccess(false);
            r.setErrorMsg(msg);
            r.setPassed(false);
            r.setOpinion(msg);
            return r;
        }
    }

    /**
     * 审核结果（旧接口兼容）
     */
    @Data
    public static class ReviewResult {
        private boolean passed;
        private String opinion;
        private String rawAnswer;

        public static ReviewResult defaultPass() {
            ReviewResult r = new ReviewResult();
            r.setPassed(true);
            r.setOpinion("AI审核服务暂时不可用，已自动通过。建议人工复核。");
            r.setRawAnswer("");
            return r;
        }
    }
}
