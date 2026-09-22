package org.dromara.procurement.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 资金状态看板行（催办用）：每个资金状态的笔数与金额合计
 *
 * @author procurement
 */
@Data
public class PmsFundStatusBoardVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 资金状态码
     */
    private String status;

    /**
     * 状态中文名
     */
    private String label;

    /**
     * 笔数
     */
    private Long count = 0L;

    /**
     * 金额合计
     */
    private BigDecimal amount = BigDecimal.ZERO;

}
