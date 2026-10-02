package org.dromara.procurement.service;

import org.dromara.procurement.domain.PmsInvoiceInfo;
import org.dromara.procurement.domain.vo.PmsInvoiceInfoViewVo;

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
     * 人工改挂发票到指定验收明细（拖拽修正，即时生效）；
     * acceptanceItemId 为 null 表示取消挂载，发票回到未匹配池并记为无效
     */
    void assignItem(Long id, Long acceptanceItemId);

    /**
     * 查询发票列表，支持按 validFlag 筛选
     */
    List<PmsInvoiceInfo> listByCondition(PmsInvoiceInfo query);

    /**
     * 查询发票台账展示列表（补充项目名/申请标题/验收单号）
     */
    List<PmsInvoiceInfoViewVo> listViewByCondition(PmsInvoiceInfo query);
}
