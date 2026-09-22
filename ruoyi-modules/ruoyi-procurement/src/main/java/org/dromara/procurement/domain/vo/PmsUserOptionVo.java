package org.dromara.procurement.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户下拉选项（备用金账户选人用，字段名与前端契约一致）
 *
 * @author procurement
 */
@Data
public class PmsUserOptionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickName;

}
