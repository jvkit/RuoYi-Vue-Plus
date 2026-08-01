package org.dromara.test;

import org.dromara.DromaraApplication;
import org.dromara.invoice.domain.bo.InvoiceInfoBo;
import org.dromara.invoice.domain.vo.InvoiceInfoVo;
import org.dromara.invoice.service.DifyInvoiceReviewService;
import org.dromara.invoice.service.IInvoiceInfoService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 发票AI审核本地测试入口
 * 直接启动 Spring Boot 上下文并跑通 AI 审核流程
 */
public class InvoiceAiReviewMain {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(DromaraApplication.class, args);
        try {
            IInvoiceInfoService invoiceInfoService = ctx.getBean(IInvoiceInfoService.class);
            DifyInvoiceReviewService difyInvoiceReviewService = ctx.getBean(DifyInvoiceReviewService.class);

            System.out.println("========== 开始发票AI审核流程测试 ==========");

            // 1. 测试Dify连接
            System.out.println("\n[1/4] 测试Dify服务连通性...");
            DifyInvoiceReviewService.ReviewResult ping = difyInvoiceReviewService.reviewInvoice(
                "011001900211", "12345678", "special",
                "1000.00", "130.00", "1130.00",
                "2026-07-01", "测试销售方", "测试购买方", null);
            System.out.println("Dify返回: passed=" + ping.isPassed() + ", opinion=" + ping.getOpinion());

            // 2. 创建问题发票并审核（应驳回）
            System.out.println("\n[2/4] 创建问题发票并审核...");
            InvoiceInfoBo badBo = new InvoiceInfoBo();
            badBo.setInvoiceCode("011001900211");
            badBo.setInvoiceNumber("BAD-20260731-001");
            badBo.setInvoiceType("special");
            badBo.setAmount(new BigDecimal("100000.00"));
            badBo.setTaxAmount(new BigDecimal("13000.00"));
            badBo.setTotalAmount(new BigDecimal("113000.00"));
            badBo.setInvoiceDate(new Date());
            badBo.setSellerName("个人小卖部");
            badBo.setBuyerName("本公司");
            badBo.setStatus("draft");
            badBo.setRemark("测试：金额异常、销售方疑似不合规");
            InvoiceInfoVo badInvoice = invoiceInfoService.insertByBo(badBo);
            System.out.println("问题发票创建成功，ID=" + badInvoice.getId());

            DifyInvoiceReviewService.ReviewResult badResult = reviewAndUpdate(
                invoiceInfoService, difyInvoiceReviewService, badInvoice.getId());
            System.out.println("问题发票审核结果: passed=" + badResult.isPassed() + ", status=" + getStatus(badInvoice.getId(), invoiceInfoService));
            System.out.println("AI意见: " + badResult.getOpinion());
            if (badResult.isPassed()) {
                System.err.println("【失败】问题发票应被驳回，但Dify通过了");
            } else {
                System.out.println("【通过】问题发票已被正确驳回");
            }

            // 3. 修改后重新提交（应通过）
            System.out.println("\n[3/4] 员工按AI意见修改后重新提交...");
            InvoiceInfoBo fixBo = new InvoiceInfoBo();
            fixBo.setId(badInvoice.getId());
            fixBo.setAmount(new BigDecimal("1000.00"));
            fixBo.setTaxAmount(new BigDecimal("130.00"));
            fixBo.setTotalAmount(new BigDecimal("1130.00"));
            fixBo.setSellerName("合规科技有限公司");
            fixBo.setStatus("draft");
            fixBo.setRemark("已按AI意见修改：调整金额和销售方");
            invoiceInfoService.updateByBo(fixBo);

            DifyInvoiceReviewService.ReviewResult fixedResult = reviewAndUpdate(
                invoiceInfoService, difyInvoiceReviewService, badInvoice.getId());
            System.out.println("修改后审核结果: passed=" + fixedResult.isPassed() + ", status=" + getStatus(badInvoice.getId(), invoiceInfoService));
            System.out.println("AI意见: " + fixedResult.getOpinion());
            if (!fixedResult.isPassed()) {
                System.err.println("【失败】修改后发票应通过，但Dify驳回了");
            } else {
                System.out.println("【通过】修改后发票已通过审核");
            }

            // 4. 创建正常发票并审核（应直接通过）
            System.out.println("\n[4/4] 创建正常发票并审核...");
            InvoiceInfoBo goodBo = new InvoiceInfoBo();
            goodBo.setInvoiceCode("011001900211");
            goodBo.setInvoiceNumber("GOOD-20260731-002");
            goodBo.setInvoiceType("normal");
            goodBo.setAmount(new BigDecimal("1000.00"));
            goodBo.setTaxAmount(new BigDecimal("130.00"));
            goodBo.setTotalAmount(new BigDecimal("1130.00"));
            goodBo.setInvoiceDate(new Date());
            goodBo.setSellerName("合规科技有限公司");
            goodBo.setBuyerName("本公司");
            goodBo.setStatus("draft");
            goodBo.setRemark("测试：正常发票");
            InvoiceInfoVo goodInvoice = invoiceInfoService.insertByBo(goodBo);
            System.out.println("正常发票创建成功，ID=" + goodInvoice.getId());

            DifyInvoiceReviewService.ReviewResult goodResult = reviewAndUpdate(
                invoiceInfoService, difyInvoiceReviewService, goodInvoice.getId());
            System.out.println("正常发票审核结果: passed=" + goodResult.isPassed() + ", status=" + getStatus(goodInvoice.getId(), invoiceInfoService));
            System.out.println("AI意见: " + goodResult.getOpinion());
            if (!goodResult.isPassed()) {
                System.err.println("【失败】正常发票应直接通过，但Dify驳回了");
            } else {
                System.out.println("【通过】正常发票已通过审核");
            }

            System.out.println("\n========== 发票AI审核流程测试结束 ==========");
        } finally {
            ctx.close();
        }
    }

    private static DifyInvoiceReviewService.ReviewResult reviewAndUpdate(
            IInvoiceInfoService invoiceInfoService,
            DifyInvoiceReviewService difyInvoiceReviewService,
            Long id) {
        InvoiceInfoVo invoice = invoiceInfoService.queryById(id);
        DifyInvoiceReviewService.ReviewResult result = difyInvoiceReviewService.reviewInvoice(
            invoice.getInvoiceCode(), invoice.getInvoiceNumber(), invoice.getInvoiceType(),
            invoice.getAmount() != null ? invoice.getAmount().toString() : null,
            invoice.getTaxAmount() != null ? invoice.getTaxAmount().toString() : null,
            invoice.getTotalAmount() != null ? invoice.getTotalAmount().toString() : null,
            invoice.getInvoiceDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(invoice.getInvoiceDate()) : null,
            invoice.getSellerName(), invoice.getBuyerName(), null);

        InvoiceInfoBo updateBo = new InvoiceInfoBo();
        updateBo.setId(id);
        updateBo.setAiOpinion(result.getOpinion());
        updateBo.setStatus(result.isPassed() ? "submitted" : "rejected");
        invoiceInfoService.updateByBo(updateBo);
        return result;
    }

    private static String getStatus(Long id, IInvoiceInfoService invoiceInfoService) {
        return invoiceInfoService.queryById(id).getStatus();
    }
}
