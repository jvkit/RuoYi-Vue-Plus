package org.dromara.invoice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.invoice.domain.InvoiceInfo;
import org.dromara.invoice.domain.bo.InvoiceInfoBo;
import org.dromara.invoice.domain.vo.InvoiceInfoVo;
import org.dromara.invoice.mapper.InvoiceInfoMapper;
import org.dromara.invoice.service.IInvoiceInfoService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 发票信息Service业务层处理
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class InvoiceInfoServiceImpl implements IInvoiceInfoService {

    private final InvoiceInfoMapper baseMapper;

    /**
     * 查询发票信息
     */
    @Override
    public InvoiceInfoVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询发票信息列表
     */
    @Override
    public TableDataInfo<InvoiceInfoVo> queryPageList(InvoiceInfoBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<InvoiceInfo> lqw = buildQueryWrapper(bo);
        Page<InvoiceInfoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询发票信息列表
     */
    @Override
    public List<InvoiceInfoVo> queryList(InvoiceInfoBo bo) {
        LambdaQueryWrapper<InvoiceInfo> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<InvoiceInfo> buildQueryWrapper(InvoiceInfoBo bo) {
        LambdaQueryWrapper<InvoiceInfo> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getInvoiceNumber()), InvoiceInfo::getInvoiceNumber, bo.getInvoiceNumber());
        lqw.like(StringUtils.isNotBlank(bo.getSellerName()), InvoiceInfo::getSellerName, bo.getSellerName());
        lqw.like(StringUtils.isNotBlank(bo.getBuyerName()), InvoiceInfo::getBuyerName, bo.getBuyerName());
        lqw.eq(StringUtils.isNotBlank(bo.getInvoiceType()), InvoiceInfo::getInvoiceType, bo.getInvoiceType());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), InvoiceInfo::getStatus, bo.getStatus());
        lqw.orderByDesc(BaseEntity::getCreateTime);
        return lqw;
    }

    /**
     * 新增发票信息
     */
    @Override
    public InvoiceInfoVo insertByBo(InvoiceInfoBo bo) {
        // 自动计算价税合计
        if (bo.getAmount() != null && bo.getTaxAmount() != null) {
            bo.setTotalAmount(bo.getAmount().add(bo.getTaxAmount()));
        }
        if (StringUtils.isBlank(bo.getStatus())) {
            bo.setStatus(BusinessStatusEnum.DRAFT.getStatus());
        }
        InvoiceInfo add = MapstructUtils.convert(bo, InvoiceInfo.class);
        baseMapper.insert(add);
        bo.setId(add.getId());
        return MapstructUtils.convert(add, InvoiceInfoVo.class);
    }

    /**
     * 修改发票信息
     */
    @Override
    public InvoiceInfoVo updateByBo(InvoiceInfoBo bo) {
        // 自动计算价税合计
        if (bo.getAmount() != null && bo.getTaxAmount() != null) {
            bo.setTotalAmount(bo.getAmount().add(bo.getTaxAmount()));
        }
        InvoiceInfo update = MapstructUtils.convert(bo, InvoiceInfo.class);
        baseMapper.updateById(update);
        return MapstructUtils.convert(update, InvoiceInfoVo.class);
    }

    /**
     * 批量删除发票信息
     */
    @Override
    public Boolean deleteWithValidByIds(List<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 查重：按发票代码+发票号码检查是否已存在
     */
    @Override
    public InvoiceInfoVo checkDuplicate(String invoiceCode, String invoiceNumber) {
        if (StringUtils.isBlank(invoiceCode) || StringUtils.isBlank(invoiceNumber)) {
            return null;
        }
        LambdaQueryWrapper<InvoiceInfo> lqw = Wrappers.lambdaQuery();
        lqw.eq(InvoiceInfo::getInvoiceCode, invoiceCode);
        lqw.eq(InvoiceInfo::getInvoiceNumber, invoiceNumber);
        lqw.last("LIMIT 1");
        return baseMapper.selectVoOne(lqw);
    }

    /**
     * 查验发票真伪（mock：财务单号奇=真，偶=假）
     */
    @Override
    public InvoiceInfoVo verifyInvoice(Long id) {
        InvoiceInfoVo invoice = baseMapper.selectVoById(id);
        if (invoice == null) {
            return null;
        }

        // 如果还没有财务查询单号，生成一个
        String finQueryNo = invoice.getFinQueryNo();
        if (StringUtils.isBlank(finQueryNo)) {
            finQueryNo = generateFinQueryNo();
        }

        // mock 逻辑：取财务单号末尾数字，奇数=真，偶数=假
        String verifyStatus;
        String lastDigits = finQueryNo.replaceAll("[^0-9]", "");
        if (lastDigits.isEmpty()) {
            verifyStatus = "failed";
        } else {
            long lastNum = Long.parseLong(lastDigits.substring(lastDigits.length() - 1));
            verifyStatus = (lastNum % 2 == 1) ? "real" : "fake";
        }

        InvoiceInfo update = new InvoiceInfo();
        update.setId(id);
        update.setFinQueryNo(finQueryNo);
        update.setVerifyStatus(verifyStatus);
        update.setVerifyTime(new java.util.Date());
        baseMapper.updateById(update);

        return baseMapper.selectVoById(id);
    }

    /**
     * 生成财务查询单号：FINV-yyyyMMdd-序号
     */
    private String generateFinQueryNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 简单用时间戳末4位作为序号
        long seq = System.currentTimeMillis() % 10000;
        return String.format("FINV-%s-%04d", datePart, seq);
    }
}
