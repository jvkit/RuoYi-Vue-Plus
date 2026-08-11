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
 * 采购管理-BOM/物料清单对象 pms_bom_item
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_bom_item")
public class PmsBomItem extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * BOM条目ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 物料分类
     */
    private String category;

    /**
     * 品名
     */
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

    /**
     * 删除标志
     */
    @TableLogic
    private Long delFlag;

}
