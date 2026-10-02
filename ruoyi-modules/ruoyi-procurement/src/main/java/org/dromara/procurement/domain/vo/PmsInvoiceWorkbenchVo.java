package org.dromara.procurement.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 发票上传工作台视图对象（行 = 已验收的采购申请）
 *
 * @author procurement
 */
@Data
public class PmsInvoiceWorkbenchVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 采购申请ID
     */
    private Long requestId;

    /**
     * 申请编号
     */
    private String requestCode;

    /**
     * 申请标题（= title_name）
     */
    private String requestTitle;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 申请人昵称（申请单 create_by 对应用户）
     */
    private String applicantName;

    /**
     * 验收单ID
     */
    private Long acceptanceId;

    /**
     * 验收单编码
     */
    private String acceptanceCode;

    /**
     * 验收日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date acceptanceDate;

    /**
     * 验收明细条数
     */
    private Integer itemTotal;

    /**
     * 被有效发票覆盖的明细条数
     */
    private Integer itemCovered;

    /**
     * 该申请名下发票总数
     */
    private Integer invoiceTotal;

    /**
     * 该申请名下有效发票数
     */
    private Integer invoiceValid;

    /**
     * 上传状态（none未上传/processing部分上传/done已完成）
     */
    private String status;

    /**
     * 完成标志（0未完成 1已完成）
     */
    private Integer doneFlag;

    /**
     * 标记完成时间
     */
    private LocalDateTime doneTime;

    /**
     * 标记完成的操作人昵称
     */
    private String doneBy;

    /**
     * 该申请名下最新一张发票的上传时间
     */
    private LocalDateTime lastInvoiceTime;

}
