package org.dromara.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 采购管理-项目归属(资金来源)对象 pms_fund_source
 *
 * <p>自维护的树形归属（如 长三角 / 天目湖），与 sys_dept 无任何关系，
 * 仅被 pms_project.owner_id 引用。维护方式见仓库根目录《项目归属.md》。</p>
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_fund_source")
public class PmsFundSource extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 归属ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 上级归属ID（0=顶级）
     */
    private Long parentId;

    /**
     * 归属名称
     */
    private String name;

    /**
     * 显示顺序
     */
    private Integer sort;

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
