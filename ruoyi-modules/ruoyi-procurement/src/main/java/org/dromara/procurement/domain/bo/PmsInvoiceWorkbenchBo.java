package org.dromara.procurement.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 发票上传工作台业务对象
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PmsInvoiceWorkbenchBo extends BaseEntity {

    /**
     * 状态筛选（none未上传/processing部分上传/done已完成/unfinished未完成=none+processing，可空=全部）
     */
    private String status;

    /**
     * 项目ID筛选
     */
    private Long projectId;

    /**
     * 关键字（模糊匹配申请编号/标题）
     */
    private String keyword;

    /**
     * 采购申请ID（标记完成/取消完成用）
     */
    @NotNull(message = "采购申请ID不能为空")
    private Long requestId;

    /**
     * 是否标记完成（true完成/false取消）
     */
    @NotNull(message = "done 不能为空")
    private Boolean done;

}
