package org.dromara.procurement.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.procurement.domain.PmsFundSource;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 采购管理-项目归属(资金来源)视图对象 pms_fund_source
 *
 * @author procurement
 */
@Data
@AutoMapper(target = PmsFundSource.class)
public class PmsFundSourceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 归属ID
     */
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
     * 子归属列表（树形）
     */
    private List<PmsFundSourceVo> children;

}
