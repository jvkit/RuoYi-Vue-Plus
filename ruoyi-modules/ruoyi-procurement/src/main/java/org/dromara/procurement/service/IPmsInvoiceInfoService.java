package org.dromara.procurement.service;

import org.dromara.procurement.domain.PmsInvoiceInfo;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-发票信息Service接口
 *
 * @author procurement
 */
public interface IPmsInvoiceInfoService {

    /**
     * 根据发票代码+号码查询有效发票（用于重复检测）
     */
    PmsInvoiceInfo findValidByCodeAndNumber(String invoiceCode, String invoiceNumber);

    /**
     * 根据 ID 查询发票信息
     */
    PmsInvoiceInfo getById(Long id);

    /**
     * 保存或更新发票信息（来自验收 AI 识别）
     */
    boolean saveOrUpdateInvoice(PmsInvoiceInfo invoice);

    /**
     * 批量删除发票台账
     */
    boolean deleteByIds(Collection<Long> ids);

    /**
     * 查询发票列表，支持按 validFlag 筛选
     */
    List<PmsInvoiceInfo> listByCondition(PmsInvoiceInfo query);
}
