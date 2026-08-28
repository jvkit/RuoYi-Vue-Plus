package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsProcurementRequest;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采购管理-采购申请业务对象 pms_procurement_request
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsProcurementRequest.class, reverseConvertGenerate = false)
public class PmsProcurementRequestBo extends BaseEntity {

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 申请ID
     */
    @NotNull(message = "申请ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 申请编号
     */
    private String requestCode;

    /**
     * 申请标题（自动拼接：【自购/对公】+项目名+月份月日期日+名称）
     */
    private String title;

    /**
     * 标题类型（自购/对公）
     */
    private String titleType;

    /**
     * 标题名称（用户填写，参与标题拼接）
     */
    private String titleName;

    /**
     * 项目ID
     */
    @NotNull(message = "项目不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long projectId;

    /**
     * 采购对接人（系统自动带出，提交者不选）
     */
    private String procurementContact;

    /**
     * 付款截图（自购必填）
     */
    private String paymentScreenshot;

    /**
     * 报价单（对公必填）
     */
    private String quotationUrl;

    /**
     * 开票信息JSON（对公必填）
     */
    private String invoiceInfoJson;

    /**
     * 总金额
     */
    private BigDecimal amount;

    /**
     * 采购类型（goods物资 service服务 fixed_asset固定资产）
     */
    private String purchaseType;

    /**
     * 申请原因
     */
    private String applyReason;

    /**
     * 状态
     */
    private String status;

    /**
     * 验收标志（none未验收/processing验收中/done已完成验收）
     */
    private String acceptanceStatus;

    /**
     * 流程实例ID
     */
    private Long processInstanceId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 申请明细
     */
    @Valid
    private List<PmsProcurementRequestItemBo> items;

}
