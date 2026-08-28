package org.dromara.procurement.domain.vo;

import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.procurement.domain.PmsBomTable;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 采购管理-BOM表(产品)视图对象 pms_bom_table
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsBomTable.class)
public class PmsBomTableVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * BOM表ID
     */
    @ExcelProperty(value = "BOM表ID")
    private Long id;

    /**
     * BOM表名称（产品名称）
     */
    @ExcelProperty(value = "BOM表名称")
    private String name;

    /**
     * 规格型号
     */
    @ExcelProperty(value = "规格型号")
    private String spec;

    /**
     * 项目ID
     */
    @ExcelProperty(value = "项目ID")
    private Long projectId;

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
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private LocalDateTime updateTime;

}
