package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsReimbursement;

import java.util.HashMap;
import java.util.Map;

/**
 * 采购管理-报销业务对象 pms_reimbursement
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsReimbursement.class, reverseConvertGenerate = false)
public class PmsReimbursementBo extends BaseEntity {

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 报销ID
     */
    @NotNull(message = "报销ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 报销单号（自动生成）
     */
    private String reimbursementCode;

    /**
     * 采购申请ID
     */
    @NotNull(message = "采购申请ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long requestId;

    /**
     * 验收单ID
     */
    private Long acceptanceId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 申请人（自动带出当前用户）
     */
    private String applicant;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 报销内容JSON
     */
    private String contentJson;

    /**
     * 状态（packing待打包 packed已打包 sent已发送）
     */
    @NotBlank(message = "状态不能为空", groups = {AddGroup.class, EditGroup.class})
    private String status;

    /**
     * 备注
     */
    private String remark;

}
