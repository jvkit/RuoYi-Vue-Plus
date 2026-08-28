package org.dromara.procurement.domain;

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
 * 采购管理-采购合同对象 pms_purchase_contract
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_purchase_contract")
public class PmsPurchaseContract extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 合同ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 采购申请ID
     */
    private Long requestId;

    /**
     * 合同标题
     */
    private String title;

    /**
     * 发票信息JSON
     */
    private String invoiceInfoJson;

    /**
     * 合同明细JSON
     */
    private String itemsJson;

    /**
     * 合同金额
     */
    private BigDecimal amount;

    /**
     * 报价单地址
     */
    private String quotationUrl;

    /**
     * 合同内容
     */
    private String content;

    /**
     * 合同文件地址
     */
    private String fileUrl;

    /**
     * 状态（draft草稿 generated已生成 sent已发送）
     */
    private String status;

    /**
     * 生成时间
     */
    private Date generateTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标志
     */
    @TableLogic
    private Long delFlag;

}
