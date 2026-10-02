package org.dromara.procurement.service;

import org.dromara.procurement.domain.vo.PmsFundSourceVo;

import java.util.List;

/**
 * 采购管理-项目归属(资金来源)Service接口
 *
 * @author procurement
 */
public interface IPmsFundSourceService {

    /**
     * 查询归属树（全部正常状态节点）
     */
    List<PmsFundSourceVo> queryTree();

}
