package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsSupplier;

/**
 * 采购管理-供应商业务对象 pms_supplier
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsSupplier.class, reverseConvertGenerate = false)
public class PmsSupplierBo extends BaseEntity {

    /**
     * 供应商ID
     */
    @NotNull(message = "供应商ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 供应商编码
     */
    @NotBlank(message = "供应商编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String supplierCode;

    /**
     * 供应商名称
     */
    @NotBlank(message = "供应商名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String supplierName;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 地址
     */
    private String address;

    /**
     * 开户行
     */
    private String bankName;

    /**
     * 银行账号
     */
    private String bankAccount;

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
