package org.dromara.procurement.domain.vo;

import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.procurement.domain.PmsPurchaseContract;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.time.LocalDateTime;

/**
 * 采购管理-采购合同视图对象 pms_purchase_contract
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsPurchaseContract.class)
public class PmsPurchaseContractVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 合同ID
     */
    @ExcelProperty(value = "合同ID")
    private Long id;

    /**
     * 合同编号
     */
    @ExcelProperty(value = "合同编号")
    private String contractNo;

    /**
     * 采购申请ID
     */
    @ExcelProperty(value = "采购申请ID")
    private Long requestId;

    /**
     * 合同标题
     */
    @ExcelProperty(value = "合同标题")
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
    @ExcelProperty(value = "合同金额")
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
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 生成时间
     */
    @ExcelProperty(value = "生成时间")
    private Date generateTime;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private LocalDateTime updateTime;

}
