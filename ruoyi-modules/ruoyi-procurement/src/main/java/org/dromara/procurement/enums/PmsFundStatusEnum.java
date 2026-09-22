package org.dromara.procurement.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.common.core.utils.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 采购申请资金状态枚举。
 * <p>
 * 状态机单向不可回溯：已采购未报销 → 已报销未汇款 → 已报销已汇款（终态），
 * 允许向前跳级（直接确认汇款时补齐报销留痕），不允许任何回退。
 * 对公申请置 {@link #NOT_APPLICABLE}，不进三态、不计入备用金占用。
 *
 * @author procurement
 */
@Getter
@AllArgsConstructor
public enum PmsFundStatusEnum {

    /**
     * 已采购未报销（审批通过后默认状态）
     */
    PURCHASED_UNREIMBURSED("purchased_unreimbursed", "已采购未报销"),

    /**
     * 已报销未汇款（资金管理员勾选已报销）
     */
    REIMBURSED_UNPAID("reimbursed_unpaid", "已报销未汇款"),

    /**
     * 已报销已汇款（终态，备用金占用释放）
     */
    REIMBURSED_PAID("reimbursed_paid", "已报销已汇款"),

    /**
     * 不适用（对公直付，不涉及备用金）
     */
    NOT_APPLICABLE("not_applicable", "不适用(对公)");

    /**
     * 状态
     */
    private final String status;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 资金状态枚举缓存
     */
    private static final Map<String, PmsFundStatusEnum> STATUS_MAP = Arrays.stream(PmsFundStatusEnum.values())
        .collect(Collectors.toConcurrentMap(PmsFundStatusEnum::getStatus, Function.identity()));

    /**
     * 状态机推进链，列表顺序即允许的推进方向（not_applicable 不在链上）
     */
    private static final List<PmsFundStatusEnum> CHAIN =
        List.of(PURCHASED_UNREIMBURSED, REIMBURSED_UNPAID, REIMBURSED_PAID);

    /**
     * 根据状态获取对应的枚举
     *
     * @param status 资金状态码
     * @return 对应枚举，找不到返回 null
     */
    public static PmsFundStatusEnum getByStatus(String status) {
        return STATUS_MAP.get(status);
    }

    /**
     * 根据状态获取中文描述
     *
     * @param status 资金状态码
     * @return 描述，状态码为空或未找到返回空字符串
     */
    public static String findByStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return StrUtil.EMPTY;
        }
        PmsFundStatusEnum statusEnum = STATUS_MAP.get(status);
        return statusEnum != null ? statusEnum.getDesc() : StrUtil.EMPTY;
    }

    /**
     * 全部状态码（看板固定 4 行用）
     */
    public static List<String> allStatus() {
        return Arrays.stream(PmsFundStatusEnum.values()).map(PmsFundStatusEnum::getStatus).toList();
    }

    /**
     * 是否在状态机推进链上（not_applicable 与空值均不在链上，不允许做报销/汇款操作）
     */
    public static boolean inChain(String status) {
        return chainIndex(status) >= 0;
    }

    /**
     * 单向校验：from → to 是否为合法推进（允许跳级，禁止回退与原地不动）
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 合法推进返回 true
     */
    public static boolean canTransfer(String from, String to) {
        int fromIndex = chainIndex(from);
        int toIndex = chainIndex(to);
        return fromIndex >= 0 && toIndex > fromIndex;
    }

    private static int chainIndex(String status) {
        PmsFundStatusEnum statusEnum = STATUS_MAP.get(status);
        return statusEnum == null ? -1 : CHAIN.indexOf(statusEnum);
    }

}
