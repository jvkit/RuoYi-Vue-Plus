package org.dromara.procurement.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.procurement.domain.PmsBomItem;
import org.dromara.procurement.domain.bo.PmsBomItemBo;
import org.dromara.procurement.domain.vo.PmsBomItemVo;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-BOM/物料清单Service接口
 *
 * @author procurement
 */
public interface IPmsBomItemService {

    /**
     * 查询BOM详情
     */
    PmsBomItemVo queryById(Long id);

    /**
     * 查询BOM分页列表
     */
    TableDataInfo<PmsBomItemVo> queryPageList(PmsBomItemBo bo, PageQuery pageQuery);

    /**
     * 查询BOM列表
     */
    List<PmsBomItemVo> queryList(PmsBomItemBo bo);

    /**
     * 新增BOM
     */
    Boolean insertByBo(PmsBomItemBo bo);

    /**
     * 修改BOM
     */
    Boolean updateByBo(PmsBomItemBo bo);

    /**
     * 校验并删除BOM
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<PmsBomItem> list);
}
