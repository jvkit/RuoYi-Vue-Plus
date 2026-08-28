package org.dromara.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 采购管理-采购申请对象 pms_procurement_request
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_procurement_request")
public class PmsProcurementRequest extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 申请ID
     */
    @TableId(value = "id")
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
    private Long projectId;

    /**
     * 采购对接人（admin 配置，系统自动带出）
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
     * 开票信息JSON（对公必填：抬头/税号/地址电话/开户行账号）
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
     * 状态（draft草稿 waiting待审核 finish已完成 back已退回 cancel已撤销 invalid已作废 termination已终止）
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
     * 删除标志
     */
    @TableLogic
    private Long delFlag;

}
