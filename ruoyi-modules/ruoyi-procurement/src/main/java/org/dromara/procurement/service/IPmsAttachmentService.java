package org.dromara.procurement.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.procurement.domain.PmsAttachment;
import org.dromara.procurement.domain.bo.PmsAttachmentBo;
import org.dromara.procurement.domain.vo.PmsAttachmentVo;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-通用附件Service接口
 *
 * @author procurement
 */
public interface IPmsAttachmentService {

    /**
     * 查询附件详情
     */
    PmsAttachmentVo queryById(Long id);

    /**
     * 查询附件分页列表
     */
    PageResult<PmsAttachmentVo> queryPageList(PmsAttachmentBo bo, PageQuery pageQuery);

    /**
     * 查询附件列表
     */
    List<PmsAttachmentVo> queryList(PmsAttachmentBo bo);

    /**
     * 按业务类型+业务ID查询附件列表
     */
    List<PmsAttachmentVo> listByBiz(String bizType, Long bizId);

    /**
     * 新增附件
     */
    Boolean insertByBo(PmsAttachmentBo bo);

    /**
     * 修改附件
     */
    Boolean updateByBo(PmsAttachmentBo bo);

    /**
     * 校验并删除附件
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<PmsAttachment> list);
}
