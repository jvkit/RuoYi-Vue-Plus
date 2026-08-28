package org.dromara.procurement.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.procurement.domain.PmsAcceptanceItem;
import org.dromara.procurement.domain.bo.PmsAcceptanceItemBo;
import org.dromara.procurement.domain.vo.PmsAcceptanceItemVo;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-验收明细Service接口
 *
 * @author procurement
 */
public interface IPmsAcceptanceItemService {

    /**
     * 查询验收明细详情
     */
    PmsAcceptanceItemVo queryById(Long id);

    /**
     * 查询验收明细分页列表
     */
    PageResult<PmsAcceptanceItemVo> queryPageList(PmsAcceptanceItemBo bo, PageQuery pageQuery);

    /**
     * 查询验收明细列表
     */
    List<PmsAcceptanceItemVo> queryList(PmsAcceptanceItemBo bo);

    /**
     * 新增验收明细
     */
    Boolean insertByBo(PmsAcceptanceItemBo bo);

    /**
     * 修改验收明细
     */
    Boolean updateByBo(PmsAcceptanceItemBo bo);

    /**
     * 校验并删除验收明细
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<PmsAcceptanceItem> list);
}
