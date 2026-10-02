package org.dromara.procurement.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.procurement.domain.bo.PmsInvoiceWorkbenchBo;
import org.dromara.procurement.domain.vo.PmsInvoiceWorkbenchVo;

/**
 * 发票上传工作台Service接口
 *
 * @author procurement
 */
public interface IPmsInvoiceWorkbenchService {

    /**
     * 工作台分页列表：行 = 已验收（验收单 status=finish）的采购申请
     */
    PageResult<PmsInvoiceWorkbenchVo> queryPageList(PmsInvoiceWorkbenchBo bo, PageQuery pageQuery);

    /**
     * 标记/取消标记发票上传完成（幂等）
     */
    void changeDoneFlag(PmsInvoiceWorkbenchBo bo);

}
