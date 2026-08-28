package org.dromara.invoice.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 发票使用记录对象 invoice_usage_record
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("invoice_usage_record")
public class InvoiceUsageRecord extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private Long invoiceId;

    /** 业务类型: reimbursement/payment/purchase */
    private String bizType;

    /** 业务单号(手填) */
    private String bizNo;

    private Long usedBy;

    private Date usedTime;

    private BigDecimal usedAmount;

    private String remark;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private Long delFlag;
}
