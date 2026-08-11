package org.dromara.procurement.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.procurement.domain.PmsBomItem;
import org.dromara.procurement.domain.bo.PmsBomItemBo;
import org.dromara.procurement.domain.vo.PmsBomItemVo;

/**
 * 采购管理-BOM/物料清单Mapper接口
 *
 * @author procurement
 */
public interface PmsBomItemMapper extends BaseMapperPlus<PmsBomItem, PmsBomItemVo> {

    /**
     * 查询BOM分页列表（关联项目、供应商）
     */
    Page<PmsBomItemVo> selectVoPageList(@Param("page") Page<PmsBomItemVo> page, @Param("bo") PmsBomItemBo bo);

}
