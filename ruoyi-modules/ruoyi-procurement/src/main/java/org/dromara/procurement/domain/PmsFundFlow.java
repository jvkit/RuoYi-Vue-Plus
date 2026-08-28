package org.dromara.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资金流水对象 pms_fund_flow
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_fund_flow")
public class PmsFundFlow extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 流水编号
     */
    private String flowNo;

    /**
     * 类型（out=流出 in=流入）
     */
    private String flowType;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 项目名快照
     */
    private String projectName;

    /**
     * 采购申请ID
     */
    private Long requestId;

    /**
     * 申请编号快照
     */
    private String requestCode;

    /**
     * 申请标题快照
     */
    private String requestTitle;

    /**
     * 金额（正数）
     */
    private BigDecimal amount;

    /**
     * 发生日期
     */
    private LocalDate occurDate;

    /**
     * 审批人ID
     */
    private Long operatorId;

    /**
     * 审批人姓名
     */
    private String operatorName;

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
