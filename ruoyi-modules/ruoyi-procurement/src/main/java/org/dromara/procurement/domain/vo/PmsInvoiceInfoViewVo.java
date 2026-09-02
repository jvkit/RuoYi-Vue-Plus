package org.dromara.procurement.domain.vo;

import lombok.Data;
import org.dromara.procurement.domain.PmsInvoiceInfo;

import java.io.Serial;
import java.io.Serializable;

/**
 * 采购管理-发票台账视图对象（列表展示用，补充关联名称）
 *
 * @author procurement
 */
@Data
public class PmsInvoiceInfoViewVo extends PmsInvoiceInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 项目名称（展示）
     */
    private String projectName;

    /**
     * 采购申请标题（展示）
     */
    private String requestTitle;

    /**
     * 验收单编码（展示）
     */
    private String acceptanceCode;
}
