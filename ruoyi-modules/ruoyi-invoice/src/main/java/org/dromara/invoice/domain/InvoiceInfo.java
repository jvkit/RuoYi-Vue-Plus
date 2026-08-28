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
 * 发票信息对象 invoice_info
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("invoice_info")
public class InvoiceInfo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 发票代码
     */
    private String invoiceCode;

    /**
     * 发票号码
     */
    private String invoiceNumber;

    /**
     * 发票类型（normal普票/special专票/electronic电子发票）
     */
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

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private Long delFlag;

}
