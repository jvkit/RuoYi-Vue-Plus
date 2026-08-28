package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsBomItem;

import java.math.BigDecimal;

/**
 * 采购管理-BOM/物料清单业务对象 pms_bom_item
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsBomItem.class, reverseConvertGenerate = false)
public class PmsBomItemBo extends BaseEntity {

    /**
     * BOM条目ID
     */
    @NotNull(message = "BOM条目ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 项目ID
     */
    @NotNull(message = "项目不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long projectId;

    /**
     * 物料分类
     */
    private String category;

    /**
     * 品名
     */
    @NotBlank(message = "品名不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /**
     * 规格型号
     */
    private String spec;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 数量
     */
    private BigDecimal qty;

    /**
     * 单位
     */
    private String unit;

    /**
     * 预估单价
     */
    private BigDecimal estPrice;

    /**
     * 预估总价
     */
    private BigDecimal estTotal;

    /**
     * 建议供应商ID
     */
    private Long supplierId;

    /**
     * 状态（0待采购 1已下单 2已到货）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

}
