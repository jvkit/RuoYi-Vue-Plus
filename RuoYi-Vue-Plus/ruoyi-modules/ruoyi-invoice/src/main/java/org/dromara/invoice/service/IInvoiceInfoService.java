package org.dromara.invoice.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.invoice.domain.bo.InvoiceInfoBo;
import org.dromara.invoice.domain.vo.InvoiceInfoVo;

import java.util.List;

/**
 * 发票信息Service接口
 *
 * @author Lion Li
 */
public interface IInvoiceInfoService {

    /**
     * 查询发票信息
     */
    InvoiceInfoVo queryById(Long id);

    /**
     * 查询发票信息列表
     */
    TableDataInfo<InvoiceInfoVo> queryPageList(InvoiceInfoBo bo, PageQuery pageQuery);

    /**
     * 查询发票信息列表
     */
    List<InvoiceInfoVo> queryList(InvoiceInfoBo bo);

    /**
     * 新增发票信息
     */
    InvoiceInfoVo insertByBo(InvoiceInfoBo bo);

    /**
     * 修改发票信息
     */
    InvoiceInfoVo updateByBo(InvoiceInfoBo bo);

    /**
     * 校验并批量删除发票信息
     */
    Boolean deleteWithValidByIds(List<Long> ids);
}
