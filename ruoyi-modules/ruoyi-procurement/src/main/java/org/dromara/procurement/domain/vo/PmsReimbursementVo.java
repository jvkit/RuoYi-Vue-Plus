package org.dromara.procurement.domain.vo;

import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.procurement.domain.PmsReimbursement;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 采购管理-报销视图对象 pms_reimbursement
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsReimbursement.class)
public class PmsReimbursementVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 报销ID
     */
    @ExcelProperty(value = "报销ID")
    private Long id;

    /**
     * 报销单号
     */
    @ExcelProperty(value = "报销单号")
    private String reimbursementCode;

    /**
     * 采购申请ID
     */
    @ExcelProperty(value = "采购申请ID")
    private Long requestId;

    /**
     * 验收单ID
     */
    @ExcelProperty(value = "验收单ID")
    private Long acceptanceId;

    /**
     * 项目ID
     */
    @ExcelProperty(value = "项目ID")
    private Long projectId;

    /**
     * 申请人
     */
    @ExcelProperty(value = "申请人")
    private String applicant;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 报销内容JSON
     */
    private String contentJson;

    /**
     * 状态（packing待打包 packed已打包 sent已发送）
     */
    @ExcelProperty(value = "状态")
    private String status;

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
