package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsReserveAccount;

import java.math.BigDecimal;

/**
 * 备用金额度账户业务对象 pms_reserve_account
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsReserveAccount.class, reverseConvertGenerate = false)
public class PmsReserveAccountBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 用户ID（新增时必填）
     */
    private Long personId;

    /**
     * 姓名快照
     */
    private String personName;

    /**
     * 备用金额度（元），为空时取 sys_config 默认额度
     */
    private BigDecimal quota;

    /**
     * 备注
     */
    private String remark;

}
