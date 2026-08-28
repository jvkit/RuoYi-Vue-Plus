package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsBomCatalog;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 采购管理-BOM物料库业务对象 pms_bom_catalog
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsBomCatalog.class, reverseConvertGenerate = false)
public class PmsBomCatalogBo extends BaseEntity {

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 物料库ID
     */
    @NotNull(message = "物料库ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 物料名称
     */
    @NotBlank(message = "物料名称不能为空", groups = {AddGroup.class, EditGroup.class})
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
    @NotNull(message = "状态不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer status;

    /**
     * 备注
     */
    private String remark;

}
