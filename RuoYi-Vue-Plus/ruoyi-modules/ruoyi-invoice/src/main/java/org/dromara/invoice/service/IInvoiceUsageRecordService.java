package org.dromara.invoice.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.invoice.domain.bo.InvoiceUsageRecordBo;
import org.dromara.invoice.domain.vo.InvoiceUsageRecordVo;

import java.util.List;

/**
 * 发票使用记录Service接口
 *
 * @author Lion Li
 */
public interface IInvoiceUsageRecordService {

    /**
     * 查询发票使用记录
     */
    InvoiceUsageRecordVo queryById(Long id);

    /**
     * 查询发票使用记录列表
     */
    TableDataInfo<InvoiceUsageRecordVo> queryPageList(InvoiceUsageRecordBo bo, PageQuery pageQuery);

    /**
     * 查询发票使用记录列表
     */
    List<InvoiceUsageRecordVo> queryList(InvoiceUsageRecordBo bo);

    /**
     * 新增发票使用记录
     */
    InvoiceUsageRecordVo insertByBo(InvoiceUsageRecordBo bo);

    /**
     * 校验并批量删除发票使用记录
     */
    Boolean deleteWithValidByIds(List<Long> ids);
}
