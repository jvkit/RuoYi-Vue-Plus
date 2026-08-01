package org.dromara.invoice.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.invoice.domain.InvoiceInfo;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 发票信息视图对象 invoice_info
 *
 * @author Lion Li
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = InvoiceInfo.class)
public class InvoiceInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 发票代码
     */
    @ExcelProperty(value = "发票代码")
    private String invoiceCode;

    /**
     * 发票号码
     */
    @ExcelProperty(value = "发票号码")
    private String invoiceNumber;

    /**
     * 发票类型（normal普票/special专票/electronic电子发票）
     */
    @ExcelProperty(value = "发票类型")
    private String invoiceType;

    /**
     * 不含税金额
     */
    @ExcelProperty(value = "不含税金额")
    private BigDecimal amount;

    /**
     * 税额
     */
    @ExcelProperty(value = "税额")
    private BigDecimal taxAmount;

    /**
     * 价税合计
     */
    @ExcelProperty(value = "价税合计")
    private BigDecimal totalAmount;

    /**
     * 开票日期
     */
    @ExcelProperty(value = "开票日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date invoiceDate;

    /**
     * 销售方名称
     */
    @ExcelProperty(value = "销售方名称")
    private String sellerName;

    /**
     * 购买方名称
     */
    @ExcelProperty(value = "购买方名称")
    private String buyerName;

    /**
     * 状态（draft草稿/submitted已提交/approved已认证/rejected已驳回）
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * AI审核意见
     */
    @ExcelProperty(value = "AI审核意见")
    private String aiOpinion;

}
