package org.dromara.procurement.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 采购分类树节点
 *
 * @author procurement
 */
@Data
public class PmsProcurementCategoryNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分类值（如 100材料 / 101金属材料）
     */
    private String value;

    /**
     * 分类名称
     */
    private String label;

    /**
     * 子分类（二级分类）
     */
    private List<PmsProcurementCategoryNode> children;

}
