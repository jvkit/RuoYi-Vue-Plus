package org.dromara.procurement.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.procurement.domain.PmsSupplier;
import org.dromara.procurement.domain.bo.PmsSupplierBo;
import org.dromara.procurement.domain.vo.PmsSupplierVo;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-供应商Service接口
 *
 * @author procurement
 */
public interface IPmsSupplierService {

    /**
     * 查询供应商详情
     */
    PmsSupplierVo queryById(Long id);

    /**
     * 查询供应商分页列表
     */
    TableDataInfo<PmsSupplierVo> queryPageList(PmsSupplierBo bo, PageQuery pageQuery);

    /**
     * 查询供应商列表
     */
    List<PmsSupplierVo> queryList(PmsSupplierBo bo);

    /**
     * 新增供应商
     */
    Boolean insertByBo(PmsSupplierBo bo);

    /**
     * 修改供应商
     */
    Boolean updateByBo(PmsSupplierBo bo);

    /**
     * 校验并删除供应商
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<PmsSupplier> list);
}
