package org.dromara.procurement.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.procurement.domain.PmsOperationLog;

import java.util.HashMap;
import java.util.Map;

/**
 * 采购管理-流转记录业务对象 pms_operation_log
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsOperationLog.class, reverseConvertGenerate = false)
public class PmsOperationLogBo extends BaseEntity {

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务ID
     */
    private Long bizId;

    /**
     * 操作动作
     */
    private String action;

    /**
     * 操作人ID
     */
    private Long operator;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 原状态
     */
    private String fromStatus;

    /**
     * 目标状态
     */
    private String toStatus;

}
