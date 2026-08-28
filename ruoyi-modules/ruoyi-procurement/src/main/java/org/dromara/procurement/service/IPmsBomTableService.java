package org.dromara.procurement.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.procurement.domain.PmsBomTable;
import org.dromara.procurement.domain.bo.PmsBomTableBo;
import org.dromara.procurement.domain.vo.PmsBomTableVo;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-BOM表(产品)Service接口
 *
 * @author procurement
 */
public interface IPmsBomTableService {

    /**
     * 查询BOM表详情
     */
    PmsBomTableVo queryById(Long id);

    /**
     * 查询BOM表分页列表
     */
    PageResult<PmsBomTableVo> queryPageList(PmsBomTableBo bo, PageQuery pageQuery);

    /**
     * 查询BOM表列表
     */
    List<PmsBomTableVo> queryList(PmsBomTableBo bo);

    /**
     * 新增BOM表
     */
    Boolean insertByBo(PmsBomTableBo bo);

    /**
     * 修改BOM表
     */
    Boolean updateByBo(PmsBomTableBo bo);

    /**
     * 校验并删除BOM表
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<PmsBomTable> list);
}
