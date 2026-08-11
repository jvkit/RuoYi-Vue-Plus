package org.dromara.procurement.domain.vo;

import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.procurement.domain.PmsProject;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 采购管理-项目视图对象 pms_project
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsProject.class)
public class PmsProjectVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    @ExcelProperty(value = "项目ID")
    private Long id;

    /**
     * 上级项目ID（0=主项目，非0=二级项目）
     */
    private Long parentId;

    /**
     * 子项目列表（树形）
     */
    private List<PmsProjectVo> children;

    /**
     * 项目编码
     */
    @ExcelProperty(value = "项目编码")
    private String projectCode;

    /**
     * 项目名称
     */
    @ExcelProperty(value = "项目名称")
    private String projectName;

    /**
     * 归属部门ID
     */
    @ExcelProperty(value = "归属部门ID")
    private Long deptId;

    /**
     * 归属部门名称
     */
    @Translation(type = TransConstant.DEPT_ID_TO_NAME, mapper = "deptId")
    @ExcelProperty(value = "归属部门")
    private String deptName;

    /**
     * 项目负责人
     */
    @ExcelProperty(value = "项目负责人")
    private String leader;

    /**
     * 项目预算
     */
    @ExcelProperty(value = "项目预算")
    private BigDecimal budget;

    /**
     * 开始日期
     */
    @ExcelProperty(value = "开始日期")
    private Date startDate;

    /**
     * 结束日期
     */
    @ExcelProperty(value = "结束日期")
    private Date endDate;

    /**
     * 状态（0停用 1正常）
     */
    @ExcelProperty(value = "状态")
    private Integer status;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

}
