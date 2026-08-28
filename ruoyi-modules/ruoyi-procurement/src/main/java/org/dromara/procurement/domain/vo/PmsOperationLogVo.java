package org.dromara.procurement.domain.vo;

import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.procurement.domain.PmsOperationLog;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.time.LocalDateTime;

/**
 * 采购管理-流转记录视图对象 pms_operation_log
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsOperationLog.class)
public class PmsOperationLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @ExcelProperty(value = "记录ID")
    private Long id;

    /**
     * 业务类型
     */
    @ExcelProperty(value = "业务类型")
    private String bizType;

    /**
     * 业务ID
     */
    @ExcelProperty(value = "业务ID")
    private Long bizId;

    /**
     * 操作动作
     */
    @ExcelProperty(value = "操作动作")
    private String action;

    /**
     * 操作人ID
     */
    private Long operator;

    /**
     * 操作人姓名
     */
    @ExcelProperty(value = "操作人姓名")
    private String operatorName;

    /**
     * 操作时间
     */
    @ExcelProperty(value = "操作时间")
    private Date operateTime;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 原状态
     */
    @ExcelProperty(value = "原状态")
    private String fromStatus;

    /**
     * 目标状态
     */
    @ExcelProperty(value = "目标状态")
    private String toStatus;

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
