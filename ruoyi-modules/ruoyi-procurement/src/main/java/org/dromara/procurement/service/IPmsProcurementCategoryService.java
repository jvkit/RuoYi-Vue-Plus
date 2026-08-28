package org.dromara.procurement.service;

import org.dromara.procurement.domain.vo.PmsProcurementCategoryNode;

import java.util.List;

/**
 * 采购分类Service接口
 *
 * @author procurement
 */
public interface IPmsProcurementCategoryService {

    /**
     * 查询采购分类树（一级 → 二级）
     */
    List<PmsProcurementCategoryNode> queryCategoryTree();

}
