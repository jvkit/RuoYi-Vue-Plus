package org.dromara.procurement.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.procurement.domain.PmsSupplier;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 采购管理-供应商视图对象 pms_supplier
 *
 * @author procurement
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsSupplier.class)
public class PmsSupplierVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 供应商ID
     */
    @ExcelProperty(value = "供应商ID")
    private Long id;

    /**
     * 供应商编码
     */
    @ExcelProperty(value = "供应商编码")
    private String supplierCode;

    /**
     * 供应商名称
     */
    @ExcelProperty(value = "供应商名称")
    private String supplierName;

    /**
     * 联系人
     */
    @ExcelProperty(value = "联系人")
    private String contactName;

    /**
     * 联系电话
     */
    @ExcelProperty(value = "联系电话")
    private String contactPhone;

    /**
     * 地址
     */
    @ExcelProperty(value = "地址")
    private String address;

    /**
     * 开户行
     */
    @ExcelProperty(value = "开户行")
    private String bankName;

    /**
     * 银行账号
     */
    @ExcelProperty(value = "银行账号")
    private String bankAccount;

    /**
     * 状态（0停用 1正常）
     */
    @ExcelProperty(value = "状态")
    private Integer status;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

}
