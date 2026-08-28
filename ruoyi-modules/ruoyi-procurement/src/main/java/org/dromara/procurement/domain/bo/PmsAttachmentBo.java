package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsAttachment;

import java.util.HashMap;
import java.util.Map;

/**
 * 采购管理-通用附件业务对象 pms_attachment
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsAttachment.class, reverseConvertGenerate = false)
public class PmsAttachmentBo extends BaseEntity {

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 附件ID
     */
    @NotNull(message = "附件ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 业务类型
     */
    @NotBlank(message = "业务类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private String bizType;

    /**
     * 业务ID
     */
    @NotNull(message = "业务ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long bizId;

    /**
     * 文件名称
     */
    @NotBlank(message = "文件名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String fileName;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 备注
     */
    private String remark;

}
