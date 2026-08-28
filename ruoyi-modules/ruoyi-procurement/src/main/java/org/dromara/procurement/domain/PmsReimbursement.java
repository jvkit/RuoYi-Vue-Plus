package org.dromara.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 采购管理-报销对象 pms_reimbursement
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_reimbursement")
public class PmsReimbursement extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 报销ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 报销单号
     */
    private String reimbursementCode;

    /**
     * 采购申请ID
     */
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
     * 申请人
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
    private String status;

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
