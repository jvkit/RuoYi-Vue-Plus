package org.dromara.procurement.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.procurement.domain.PmsReimbursement;
import org.dromara.procurement.domain.bo.PmsReimbursementBo;
import org.dromara.procurement.domain.vo.PmsReimbursementVo;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-报销Service接口
 *
 * @author procurement
 */
public interface IPmsReimbursementService {

    /**
     * 查询报销详情
     */
    PmsReimbursementVo queryById(Long id);

    /**
     * 查询报销分页列表
     */
    PageResult<PmsReimbursementVo> queryPageList(PmsReimbursementBo bo, PageQuery pageQuery);

    /**
     * 查询报销列表
     */
    List<PmsReimbursementVo> queryList(PmsReimbursementBo bo);

    /**
     * 新增报销
     */
    Boolean insertByBo(PmsReimbursementBo bo);

    /**
     * 修改报销
     */
    Boolean updateByBo(PmsReimbursementBo bo);

    /**
     * 校验并删除报销
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<PmsReimbursement> list);
}
