package org.dromara.procurement.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.procurement.domain.PmsPurchaseOrder;
import org.dromara.procurement.domain.bo.PmsPurchaseOrderBo;
import org.dromara.procurement.domain.vo.PmsPurchaseOrderVo;

/**
 * 采购管理-采购订单Mapper接口
 *
 * @author procurement
 */
public interface PmsPurchaseOrderMapper extends BaseMapperPlus<PmsPurchaseOrder, PmsPurchaseOrderVo> {

    /**
     * 查询采购订单分页列表（关联项目、供应商、采购申请）
     */
    Page<PmsPurchaseOrderVo> selectVoPageList(@Param("page") Page<PmsPurchaseOrderVo> page, @Param("bo") PmsPurchaseOrderBo bo);

}
