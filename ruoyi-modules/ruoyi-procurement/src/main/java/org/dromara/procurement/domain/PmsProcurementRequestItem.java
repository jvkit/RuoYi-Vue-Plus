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
 * 采购管理-采购申请明细对象 pms_procurement_request_item
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_procurement_request_item")
public class PmsProcurementRequestItem extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 明细ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 申请ID
     */
    private Long requestId;

    /**
     * 采购种类（科研类/非科研类，默认科研类）
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
    private BigDecimal quantity;

    /**
     * 单价
     */
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

    /**
     * 删除标志
     */
    @TableLogic
    private Long delFlag;

}
