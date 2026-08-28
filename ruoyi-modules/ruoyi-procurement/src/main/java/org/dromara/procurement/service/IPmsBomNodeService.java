package org.dromara.procurement.service;

import org.dromara.procurement.domain.PmsBomNode;
import org.dromara.procurement.domain.bo.PmsBomNodeBo;
import org.dromara.procurement.domain.vo.PmsBomNodeVo;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-BOM节点Service接口
 *
 * @author procurement
 */
public interface IPmsBomNodeService {

    /**
     * 查询节点详情
     */
    PmsBomNodeVo queryById(Long id);

    /**
     * 查询节点平铺列表（按 bomTableId 过滤，前端自行组装树）
     */
    List<PmsBomNodeVo> queryList(PmsBomNodeBo bo);

    /**
     * 新增节点
     */
    Boolean insertByBo(PmsBomNodeBo bo);

    /**
     * 修改节点
     */
    Boolean updateByBo(PmsBomNodeBo bo);

    /**
     * 校验并删除节点
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<PmsBomNode> list);
}
