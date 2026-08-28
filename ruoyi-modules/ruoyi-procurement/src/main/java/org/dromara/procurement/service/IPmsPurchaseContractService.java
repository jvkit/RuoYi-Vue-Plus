package org.dromara.procurement.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.procurement.domain.PmsPurchaseContract;
import org.dromara.procurement.domain.bo.PmsPurchaseContractBo;
import org.dromara.procurement.domain.vo.PmsPurchaseContractVo;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-采购合同Service接口
 *
 * @author procurement
 */
public interface IPmsPurchaseContractService {

    /**
     * 查询合同详情
     */
    PmsPurchaseContractVo queryById(Long id);

    /**
     * 查询合同分页列表
     */
    PageResult<PmsPurchaseContractVo> queryPageList(PmsPurchaseContractBo bo, PageQuery pageQuery);

    /**
     * 查询合同列表
     */
    List<PmsPurchaseContractVo> queryList(PmsPurchaseContractBo bo);

    /**
     * 新增合同
     */
    Boolean insertByBo(PmsPurchaseContractBo bo);

    /**
     * 修改合同
     */
    Boolean updateByBo(PmsPurchaseContractBo bo);

    /**
     * 校验并删除合同
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<PmsPurchaseContract> list);
}
