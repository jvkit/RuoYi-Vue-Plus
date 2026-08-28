package org.dromara.procurement.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.procurement.domain.PmsProcurementRequest;
import org.dromara.procurement.domain.bo.PmsProcurementRequestBo;
import org.dromara.procurement.domain.vo.PmsProcurementRequestVo;

/**
 * 采购管理-采购申请Mapper接口
 *
 * @author procurement
 */
public interface PmsProcurementRequestMapper extends BaseMapperPlus<PmsProcurementRequest, PmsProcurementRequestVo> {

    /**
     * 查询采购申请分页列表（关联项目、供应商）
     */
    Page<PmsProcurementRequestVo> selectVoPageList(@Param("page") Page<PmsProcurementRequestVo> page, @Param("bo") PmsProcurementRequestBo bo);

}
