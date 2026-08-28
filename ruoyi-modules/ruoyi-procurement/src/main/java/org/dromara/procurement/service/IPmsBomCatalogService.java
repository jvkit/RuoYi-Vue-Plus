package org.dromara.procurement.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.procurement.domain.PmsBomCatalog;
import org.dromara.procurement.domain.bo.PmsBomCatalogBo;
import org.dromara.procurement.domain.vo.PmsBomCatalogVo;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-BOM物料库Service接口
 *
 * @author procurement
 */
public interface IPmsBomCatalogService {

    /**
     * 查询物料库详情
     */
    PmsBomCatalogVo queryById(Long id);

    /**
     * 查询物料库分页列表
     */
    PageResult<PmsBomCatalogVo> queryPageList(PmsBomCatalogBo bo, PageQuery pageQuery);

    /**
     * 查询物料库列表
     */
    List<PmsBomCatalogVo> queryList(PmsBomCatalogBo bo);

    /**
     * 新增物料库
     */
    Boolean insertByBo(PmsBomCatalogBo bo);

    /**
     * 修改物料库
     */
    Boolean updateByBo(PmsBomCatalogBo bo);

    /**
     * 校验并删除物料库
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<PmsBomCatalog> list);
}
