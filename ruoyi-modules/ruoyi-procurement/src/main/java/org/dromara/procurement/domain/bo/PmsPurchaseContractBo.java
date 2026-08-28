package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsPurchaseContract;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 采购管理-采购合同业务对象 pms_purchase_contract
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsPurchaseContract.class, reverseConvertGenerate = false)
public class PmsPurchaseContractBo extends BaseEntity {

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 合同ID
     */
    @NotNull(message = "合同ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 合同编号（自动生成）
     */
    private String contractNo;

    /**
     * 采购申请ID
     */
    @NotNull(message = "采购申请ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long requestId;

    /**
     * 合同标题
     */
    @NotBlank(message = "合同标题不能为空", groups = {AddGroup.class, EditGroup.class})
    private String title;

    /**
     * 发票信息JSON
     */
    private String invoiceInfoJson;

    /**
     * 合同明细JSON
     */
    private String itemsJson;

    /**
     * 合同金额
     */
    @NotNull(message = "合同金额不能为空", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal amount;

    /**
     * 报价单地址
     */
    private String quotationUrl;

    /**
     * 合同内容
     */
    private String content;

    /**
     * 合同文件地址
     */
    private String fileUrl;

    /**
     * 状态（draft草稿 generated已生成 sent已发送）
     */
    @NotBlank(message = "状态不能为空", groups = {AddGroup.class, EditGroup.class})
    private String status;

    /**
     * 生成时间
     */
    private Date generateTime;

    /**
     * 备注
     */
    private String remark;

}
