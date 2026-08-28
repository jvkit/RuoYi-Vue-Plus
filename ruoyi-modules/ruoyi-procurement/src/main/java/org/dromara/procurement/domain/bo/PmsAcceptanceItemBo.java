package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsAcceptanceItem;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 采购管理-验收明细业务对象 pms_acceptance_item
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsAcceptanceItem.class, reverseConvertGenerate = false)
public class PmsAcceptanceItemBo extends BaseEntity {

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 明细ID
     */
    @NotNull(message = "明细ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 验收单ID
     */
    @NotNull(message = "验收单ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long acceptanceId;

    /**
     * 采购申请明细ID
     */
    private Long requestItemId;

    /**
     * 前端带出时的采购申请明细ID（sourceItemId），保存时映射到 requestItemId
     */
    private Long sourceItemId;

    /**
     * 物品名称
     */
    @NotBlank(message = "物品名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String itemName;

    /**
     * 规格型号
     */
    private String spec;

    /**
     * 申请单价
     */
    private BigDecimal applyPrice;

    /**
     * 发票单价
     */
    private BigDecimal invoicePrice;

    /**
     * 价格核查结果（pass通过 over超标）
     */
    private String priceCheck;

    /**
     * 验收照片地址
     */
    private String photoUrl;

    /**
     * 发票照片地址
     */
    private String invoiceUrl;

    /**
     * AI核查意见
     */
    private String aiOpinion;

    /**
     * 验收结果（pass通过 over不通过）
     */
    private String result;

    /**
     * 备注
     */
    private String remark;

}
