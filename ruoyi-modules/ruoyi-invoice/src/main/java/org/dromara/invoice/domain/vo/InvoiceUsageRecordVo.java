package org.dromara.invoice.domain.vo;

import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.invoice.domain.InvoiceUsageRecord;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 发票使用记录视图对象 invoice_usage_record
 *
 * @author Lion Li
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = InvoiceUsageRecord.class)
public class InvoiceUsageRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 发票ID
     */
    @ExcelProperty(value = "发票ID")
    private Long invoiceId;

    /**
     * 业务类型
     */
    @ExcelProperty(value = "业务类型")
    private String bizType;

    /**
     * 业务单号
     */
    @ExcelProperty(value = "业务单号")
    private String bizNo;

    /**
     * 使用人
     */
    @ExcelProperty(value = "使用人")
    private Long usedBy;

    /**
     * 使用时间
     */
    @ExcelProperty(value = "使用时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date usedTime;

    /**
     * 使用金额
     */
    @ExcelProperty(value = "使用金额")
    private BigDecimal usedAmount;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;
}
