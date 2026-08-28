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
 * 采购管理-验收明细对象 pms_acceptance_item
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_acceptance_item")
public class PmsAcceptanceItem extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 明细ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 验收单ID
     */
    private Long acceptanceId;

    /**
     * 采购申请明细ID
     */
    private Long requestItemId;

    /**
     * 物品名称
     */
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

    /**
     * 删除标志
     */
    @TableLogic
    private Long delFlag;

}
