package org.dromara.procurement.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.common.core.exception.ServiceException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 备用金按人拆账工具。
 * <p>
 * 口径（docs/9.20/设计探讨-备用金与报销流程.md §1/§4）：
 * 按备用金人顺序逐个扣款，每人扣 min(剩余待扣, 该人当前可用额度)；
 * 顺序靠后的人兜尾差（最后一笔拿到剩余全部）。全部扣完仍有剩余说明额度数据异常，直接拒绝。
 *
 * @author procurement
 */
public final class PmsFundSplitUtil {

    private PmsFundSplitUtil() {
    }

    /**
     * 备用金出纳人（顺序即扣款顺序）
     */
    @Getter
    @AllArgsConstructor
    public static class Payer {
        private final Long personId;
        private final String personName;
    }

    /**
     * 拆账结果：每人一条
     */
    @Getter
    @AllArgsConstructor
    public static class Split {
        private final Long personId;
        private final String personName;
        private final BigDecimal amount;
    }

    /**
     * 按顺序拆账
     *
     * @param payers           备用金人列表（有序，不允许为空/空ID）
     * @param total            待拆总金额（正数）
     * @param availableLookup  可用额度查询（quota - 占用），入参 personId
     * @return 每人分摊额列表（顺序与 payers 一致，金额为 0 的人不会出现）
     */
    public static List<Split> split(List<Payer> payers, BigDecimal total, Function<Long, BigDecimal> availableLookup) {
        if (payers == null || payers.isEmpty()) {
            throw new ServiceException("请按扣款顺序选择备用金出纳人");
        }
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("拆账金额必须大于 0");
        }
        List<Split> splits = new ArrayList<>();
        BigDecimal remaining = total;
        for (Payer payer : payers) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            if (payer.getPersonId() == null) {
                throw new ServiceException("备用金出纳人不能有空人员");
            }
            BigDecimal available = availableLookup.apply(payer.getPersonId());
            if (available == null) {
                available = BigDecimal.ZERO;
            }
            BigDecimal deduct = remaining.min(available);
            if (deduct.compareTo(BigDecimal.ZERO) > 0) {
                splits.add(new Split(payer.getPersonId(), payer.getPersonName(), deduct));
                remaining = remaining.subtract(deduct);
            }
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new ServiceException("备用金可用额度不足，尚有 " + remaining.stripTrailingZeros().toPlainString()
                + " 元无法分摊，请检查各出纳人额度或调整顺序");
        }
        return splits;
    }

}
