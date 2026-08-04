package org.dromara.invoice.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.invoice.domain.InvoiceInfo;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 发票信息业务对象 invoice_info
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = InvoiceInfo.class, reverseConvertGenerate = false)
public class InvoiceInfoBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 发票代码
     */
    private String invoiceCode;

    /**
     * 发票号码
     */
    @NotBlank(message = "发票号码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String invoiceNumber;

    /**
     * 发票类型（normal普票/special专票/electronic电子发票）
     */
    @NotBlank(message = "发票类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private String invoiceType;

    /**
     * 不含税金额
     */
    private BigDecimal amount;

    /**
     * 税额
     */
    private BigDecimal taxAmount;

    /**
     * 价税合计
     */
    private BigDecimal totalAmount;

    /**
     * 开票日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date invoiceDate;

    /**
     * 销售方名称
     */
    private String sellerName;

    /**
     * 购买方名称
     */
    private String buyerName;

    /**
     * 状态（draft草稿/submitted已提交/approved已认证/rejected已驳回）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * AI审核意见
     */
    private String aiOpinion;

    /**
     * 真伪状态: unverified/real/fake/failed
     */
    private String verifyStatus;

    /**
     * 查验时间
     */
    private Date verifyTime;

    /**
     * 财务查询单号
     */
    private String finQueryNo;

    /**
     * 关联订单号
     */
    private String orderNo;

}
