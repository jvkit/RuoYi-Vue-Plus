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
 * 采购管理-BOM物料库对象 pms_bom_catalog
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_bom_catalog")
public class PmsBomCatalog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 物料库ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 物料名称
     */
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
     * 参考单价
     */
    private BigDecimal refPrice;

    /**
     * 物料分类
     */
    private String category;

    /**
     * 关联链接
     */
    private String link;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 库存ID
     */
    private Long stockId;

    /**
     * 状态（0停用 1正常）
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
