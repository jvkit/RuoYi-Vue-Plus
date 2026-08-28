package org.dromara.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 采购管理-BOM表(产品)对象 pms_bom_table
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_bom_table")
public class PmsBomTable extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * BOM表ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * BOM表名称（产品名称）
     */
    private String name;

    /**
     * 规格型号
     */
    private String spec;

    /**
     * 项目ID
     */
    private Long projectId;

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
