package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsProject;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 采购管理-项目业务对象 pms_project
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsProject.class, reverseConvertGenerate = false)
public class PmsProjectBo extends BaseEntity {

    /**
     * 项目ID
     */
    @NotNull(message = "项目ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 项目编码
     */
    @NotBlank(message = "项目编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String projectCode;

    /**
     * 项目名称
     */
    @NotBlank(message = "项目名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String projectName;

    /**
     * 归属部门ID
     */
    @NotNull(message = "归属部门不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long deptId;

    /**
     * 项目负责人
     */
    private String leader;

    /**
     * 项目预算
     */
    private BigDecimal budget;

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
    @NotNull(message = "状态不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer status;

    /**
     * 备注
     */
    private String remark;

}
