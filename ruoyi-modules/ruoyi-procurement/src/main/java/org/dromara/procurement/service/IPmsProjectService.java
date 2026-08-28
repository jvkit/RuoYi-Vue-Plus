package org.dromara.procurement.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.procurement.domain.PmsProject;
import org.dromara.procurement.domain.bo.PmsProjectBo;
import org.dromara.procurement.domain.vo.PmsProjectVo;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-项目Service接口
 *
 * @author procurement
 */
public interface IPmsProjectService {

    /**
     * 查询项目详情
     */
    PmsProjectVo queryById(Long id);

    /**
     * 查询项目分页列表
     */
    PageResult<PmsProjectVo> queryPageList(PmsProjectBo bo, PageQuery pageQuery);

    /**
     * 查询项目列表
     */
    List<PmsProjectVo> queryList(PmsProjectBo bo);

    /**
     * 查询项目树形列表（全部正常项目，按 parentId 组装树）
     */
    List<PmsProjectVo> queryTreeList();

    /**
     * 新增项目
     */
    Boolean insertByBo(PmsProjectBo bo);

    /**
     * 修改项目
     */
    Boolean updateByBo(PmsProjectBo bo);

    /**
     * 校验并删除项目
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<PmsProject> list);
}
