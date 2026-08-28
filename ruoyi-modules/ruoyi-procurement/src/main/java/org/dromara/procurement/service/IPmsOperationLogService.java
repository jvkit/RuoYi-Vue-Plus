package org.dromara.procurement.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.procurement.domain.bo.PmsOperationLogBo;
import org.dromara.procurement.domain.vo.PmsOperationLogVo;

import java.util.List;

/**
 * 采购管理-流转记录Service接口
 *
 * @author procurement
 */
public interface IPmsOperationLogService {

    /**
     * 查询流转记录详情
     */
    PmsOperationLogVo queryById(Long id);

    /**
     * 查询流转记录分页列表
     */
    PageResult<PmsOperationLogVo> queryPageList(PmsOperationLogBo bo, PageQuery pageQuery);

    /**
     * 查询流转记录列表
     */
    List<PmsOperationLogVo> queryList(PmsOperationLogBo bo);
}
