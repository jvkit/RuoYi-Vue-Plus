package org.dromara.procurement.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.procurement.domain.bo.PmsPurchaseOrderBo;
import org.dromara.procurement.domain.vo.PmsPurchaseOrderVo;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-采购订单Service接口
 *
 * @author procurement
 */
public interface IPmsPurchaseOrderService {

    /**
     * 查询采购订单详情（含明细）
     */
    PmsPurchaseOrderVo queryById(Long id);

    /**
     * 查询采购订单分页列表
     */
    PageResult<PmsPurchaseOrderVo> queryPageList(PmsPurchaseOrderBo bo, PageQuery pageQuery);

    /**
     * 查询采购订单列表
     */
    List<PmsPurchaseOrderVo> queryList(PmsPurchaseOrderBo bo);

    /**
     * 新增采购订单
     */
    Boolean insertByBo(PmsPurchaseOrderBo bo);

    /**
     * 修改采购订单
     */
    Boolean updateByBo(PmsPurchaseOrderBo bo);

    /**
     * 校验并删除采购订单
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

}
