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
 * 采购管理-项目对象 pms_project
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_project")
public class PmsProject extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 上级项目ID（0=主项目，非0=二级项目）
     */
    private Long parentId;

    /**
     * 项目编码
     */
    private String projectCode;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 归属部门ID
     */
    private Long deptId;

    /**
     * 项目负责人
     */
    private String leader;

    /**
     * 项目负责人用户ID（审批流分派用）
     */
    private Long leaderId;

    /**
     * 项目预算
     */
    private BigDecimal budget;

    /**
     * 已用金额（系统累计，采购申请通过后累加）
     */
    private BigDecimal usedAmount;

    /**
     * 开始日期
     */
    private Date startDate;

    /**
     * 结束日期
     */
    private Date endDate;

    /**
     * 状态（0停用 1正常）
     */
    private Integer status;

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
