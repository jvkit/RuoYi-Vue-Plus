package org.dromara.procurement.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.dromara.procurement.domain.PmsAcceptanceItem;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购管理-验收明细视图对象 pms_acceptance_item
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsAcceptanceItem.class)
public class PmsAcceptanceItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 明细ID
     */
    @ExcelProperty(value = "明细ID")
    private Long id;

    /**
     * 验收单ID
     */
    @ExcelProperty(value = "验收单ID")
    private Long acceptanceId;

    /**
     * 采购申请明细ID
     */
    @ExcelProperty(value = "采购申请明细ID")
    private Long requestItemId;

    /**
     * 前端回显用：关联采购申请明细ID（等价于 requestItemId）
     */
    private Long sourceItemId;

    /**
     * 物品名称
     */
    @ExcelProperty(value = "物品名称")
    private String itemName;

    /**
     * 规格型号
     */
    @ExcelProperty(value = "规格型号")
    private String spec;

    /**
     * 申请单价
     */
    @ExcelProperty(value = "申请单价")
    private BigDecimal applyPrice;

    /**
     * 发票单价
     */
    @ExcelProperty(value = "发票单价")
    private BigDecimal invoicePrice;

    /**
     * 价格核查结果（pass通过 over超标）
     */
    @ExcelProperty(value = "价格核查结果")
    private String priceCheck;

    /**
     * 验收照片地址
     */
    @ExcelProperty(value = "验收照片地址")
    private String photoUrl;

    /**
     * 发票照片地址
     */
    @ExcelProperty(value = "发票照片地址")
    private String invoiceUrl;

    /**
     * AI核查意见
     */
    @ExcelProperty(value = "AI核查意见")
    private String aiOpinion;

    /**
     * 验收结果（pass通过 over不通过）
     */
    @ExcelProperty(value = "验收结果")
    private String result;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private LocalDateTime updateTime;

}
