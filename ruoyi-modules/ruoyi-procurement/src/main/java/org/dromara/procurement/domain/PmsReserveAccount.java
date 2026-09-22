package org.dromara.procurement.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 备用金额度账户对象 pms_reserve_account
 * <p>
 * 一人一行，额度由管理员配置（默认取 sys_config procurement.reserve.default_quota）。
 * 本表只存「额度」这一配置事实；已占用/可用/已回笼一律实时聚合采购申请得出，
 * 不落库，避免双写不一致（见 docs/9.20/资金与报销体系设计-v4-确认版.md §2.2）。
 *
 * @author procurement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_reserve_account")
public class PmsReserveAccount extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 用户ID（一人一行，唯一性由 Service 层保证：本表走逻辑删除，不建唯一索引）
     */
    private Long personId;

    /**
     * 姓名快照
     */
    private String personName;

    /**
     * 备用金额度（元）
     */
    private BigDecimal quota;

    /**
     * 备注（如调增原因）
     */
    private String remark;

    /**
     * 删除标志
     */
    @TableLogic
    private Long delFlag;

}
