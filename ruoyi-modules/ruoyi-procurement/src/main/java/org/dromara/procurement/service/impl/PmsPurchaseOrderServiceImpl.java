package org.dromara.procurement.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.procurement.domain.PmsPurchaseOrder;
import org.dromara.procurement.domain.PmsPurchaseOrderItem;
import org.dromara.procurement.domain.bo.PmsPurchaseOrderBo;
import org.dromara.procurement.domain.bo.PmsPurchaseOrderItemBo;
import org.dromara.procurement.domain.vo.PmsPurchaseOrderItemVo;
import org.dromara.procurement.domain.vo.PmsPurchaseOrderVo;
import org.dromara.procurement.mapper.PmsPurchaseOrderItemMapper;
import org.dromara.procurement.mapper.PmsPurchaseOrderMapper;
import org.dromara.procurement.service.IPmsPurchaseOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 采购管理-采购订单Service业务层处理
 *
 * @author procurement
 */
@RequiredArgsConstructor
@Service
public class PmsPurchaseOrderServiceImpl implements IPmsPurchaseOrderService {

    private final PmsPurchaseOrderMapper baseMapper;
    private final PmsPurchaseOrderItemMapper itemMapper;

    @Override
    public PmsPurchaseOrderVo queryById(Long id) {
        PmsPurchaseOrderVo vo = baseMapper.selectVoById(id);
        if (ObjectUtil.isNotNull(vo)) {
            List<PmsPurchaseOrderItemVo> items = itemMapper.selectVoList(
                Wrappers.<PmsPurchaseOrderItem>lambdaQuery()
                    .eq(PmsPurchaseOrderItem::getOrderId, id)
                    .orderByAsc(PmsPurchaseOrderItem::getSortNo));
            vo.setItems(items);
        }
        return vo;
    }

    @Override
    public PageResult<PmsPurchaseOrderVo> queryPageList(PmsPurchaseOrderBo bo, PageQuery pageQuery) {
        Page<PmsPurchaseOrderVo> page = pageQuery.build();
        baseMapper.selectVoPageList(page, bo);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public List<PmsPurchaseOrderVo> queryList(PmsPurchaseOrderBo bo) {
        Page<PmsPurchaseOrderVo> page = new Page<>(1, Integer.MAX_VALUE);
        baseMapper.selectVoPageList(page, bo);
        return page.getRecords();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(PmsPurchaseOrderBo bo) {
        if (StringUtils.isBlank(bo.getOrderNo())) {
            bo.setOrderNo(generateOrderNo());
        }
        if (StringUtils.isBlank(bo.getStatus())) {
            bo.setStatus("draft");
        }
        if (ObjectUtil.isNull(bo.getOrderDate())) {
            bo.setOrderDate(new Date());
        }
        calcHeaderAmount(bo);
        PmsPurchaseOrder add = MapstructUtils.convert(bo, PmsPurchaseOrder.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
            saveItems(add.getId(), bo.getItems());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(PmsPurchaseOrderBo bo) {
        if (ObjectUtil.isNull(bo.getId())) {
            throw new ServiceException("订单ID不能为空");
        }
        PmsPurchaseOrder exist = baseMapper.selectById(bo.getId());
        if (ObjectUtil.isNull(exist)) {
            throw new ServiceException("采购订单不存在");
        }
        calcHeaderAmount(bo);
        PmsPurchaseOrder update = MapstructUtils.convert(bo, PmsPurchaseOrder.class);
        boolean flag = baseMapper.updateById(update) > 0;
        if (flag) {
            itemMapper.delete(Wrappers.<PmsPurchaseOrderItem>lambdaQuery()
                .eq(PmsPurchaseOrderItem::getOrderId, bo.getId()));
            saveItems(bo.getId(), bo.getItems());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        itemMapper.delete(Wrappers.<PmsPurchaseOrderItem>lambdaQuery()
            .in(PmsPurchaseOrderItem::getOrderId, ids));
        return baseMapper.deleteByIds(ids) > 0;
    }

    private void saveItems(Long orderId, List<PmsPurchaseOrderItemBo> itemBos) {
        if (CollUtil.isEmpty(itemBos)) {
            return;
        }
        List<PmsPurchaseOrderItem> items = new ArrayList<>();
        int sort = 1;
        for (PmsPurchaseOrderItemBo itemBo : itemBos) {
            PmsPurchaseOrderItem item = MapstructUtils.convert(itemBo, PmsPurchaseOrderItem.class);
            item.setOrderId(orderId);
            if (ObjectUtil.isNull(item.getSortNo())) {
                item.setSortNo(sort++);
            }
            calcItemAmount(item);
            items.add(item);
        }
        itemMapper.insertBatch(items);
    }

    private void calcHeaderAmount(PmsPurchaseOrderBo bo) {
        BigDecimal total = BigDecimal.ZERO;
        if (CollUtil.isNotEmpty(bo.getItems())) {
            for (PmsPurchaseOrderItemBo item : bo.getItems()) {
                total = total.add(calcItemAmount(item));
            }
        }
        bo.setAmount(total);
    }

    private BigDecimal calcItemAmount(PmsPurchaseOrderItemBo item) {
        BigDecimal qty = ObjectUtil.isNull(item.getQuantity()) ? BigDecimal.ZERO : item.getQuantity();
        BigDecimal price = ObjectUtil.isNull(item.getUnitPrice()) ? BigDecimal.ZERO : item.getUnitPrice();
        BigDecimal total = qty.multiply(price);
        item.setAmount(total);
        return total;
    }

    private void calcItemAmount(PmsPurchaseOrderItem item) {
        BigDecimal qty = ObjectUtil.isNull(item.getQuantity()) ? BigDecimal.ZERO : item.getQuantity();
        BigDecimal price = ObjectUtil.isNull(item.getUnitPrice()) ? BigDecimal.ZERO : item.getUnitPrice();
        item.setAmount(qty.multiply(price));
    }

    private String generateOrderNo() {
        String prefix = "PO-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        LambdaQueryWrapper<PmsPurchaseOrder> lqw = Wrappers.lambdaQuery();
        lqw.likeRight(PmsPurchaseOrder::getOrderNo, prefix);
        long count = baseMapper.selectCount(lqw);
        return prefix + String.format("%03d", count + 1);
    }

}
