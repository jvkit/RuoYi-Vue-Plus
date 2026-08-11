package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsProcurementRequestItem;

import java.math.BigDecimal;

/**
 * 采购管理-采购申请明细业务对象 pms_procurement_request_item
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsProcurementRequestItem.class, reverseConvertGenerate = false)
public class PmsProcurementRequestItemBo extends BaseEntity {

    /**
     * 明细ID
     */
    @NotNull(message = "明细ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 申请ID
     */
    private Long requestId;

    /**
     * 采购种类（科研类/非科研类）
     */
    private String purchaseType;

    /**
     * 一级分类
     */
    private String category1;

    /**
     * 二级分类
     */
    private String category2;

    /**
     * 项目归属
     */
    private String projectBelong;

    /**
     * 品名
     */
    @NotBlank(message = "品名不能为空", groups = {AddGroup.class, EditGroup.class})
    private String itemName;

    /**
     * 规格型号
     */
    private String spec;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 单位
     */
    private String unit;

    /**
     * 数量
     */
    @NotNull(message = "数量不能为空", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal quantity;

    /**
     * 单价
     */
    @NotNull(message = "单价不能为空", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal unitPrice;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 关联BOM条目ID
     */
    private Long bomItemId;

    /**
     * 排序号
     */
    private Integer sortNo;

    /**
     * 商品链接
     */
    private String link;

    /**
     * 平台（识别自链接，视为供应商）
     */
    private String platform;

    /**
     * 供应商ID（明细级，非必填）
     */
    private Long supplierId;

    /**
     * 备注
     */
    private String remark;

}
