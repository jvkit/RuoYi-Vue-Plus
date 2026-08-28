package org.dromara.procurement.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.procurement.domain.PmsWarehouseStock;
import org.dromara.procurement.domain.bo.PmsWarehouseStockBo;
import org.dromara.procurement.domain.vo.PmsWarehouseStockVo;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-仓库库存Service接口
 *
 * @author procurement
 */
public interface IPmsWarehouseStockService {

    /**
     * 查询仓库库存详情
     */
    PmsWarehouseStockVo queryById(Long id);

    /**
     * 查询仓库库存分页列表
     */
    PageResult<PmsWarehouseStockVo> queryPageList(PmsWarehouseStockBo bo, PageQuery pageQuery);

    /**
     * 查询仓库库存列表
     */
    List<PmsWarehouseStockVo> queryList(PmsWarehouseStockBo bo);

    /**
     * 新增仓库库存
     */
    Boolean insertByBo(PmsWarehouseStockBo bo);

    /**
     * 修改仓库库存
     */
    Boolean updateByBo(PmsWarehouseStockBo bo);

    /**
     * 校验并删除仓库库存
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<PmsWarehouseStock> list);
}
