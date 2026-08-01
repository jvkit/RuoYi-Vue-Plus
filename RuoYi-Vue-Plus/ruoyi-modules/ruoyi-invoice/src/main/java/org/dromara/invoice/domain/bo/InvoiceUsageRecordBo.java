package org.dromara.invoice.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.invoice.domain.InvoiceUsageRecord;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 发票使用记录业务对象 invoice_usage_record
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = InvoiceUsageRecord.class, reverseConvertGenerate = false)
public class InvoiceUsageRecordBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 发票ID
     */
    @NotNull(message = "发票ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long invoiceId;

    /**
     * 业务类型: reimbursement/payment/purchase
     */
    @NotBlank(message = "业务类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private String bizType;

    /**
     * 业务单号(手填)
     */
    private String bizNo;

    /**
     * 使用人
     */
    private Long usedBy;

    /**
     * 使用时间
     */
    private Date usedTime;

    /**
     * 使用金额
     */
    private BigDecimal usedAmount;

    /**
     * 备注
     */
    private String remark;
}
