package org.dromara.procurement.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.procurement.domain.PmsAcceptance;
import org.dromara.procurement.domain.bo.PmsAcceptanceBo;
import org.dromara.procurement.domain.vo.PmsAcceptanceVo;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-采购验收单Service接口
 *
 * @author procurement
 */
public interface IPmsAcceptanceService {

    /**
     * 查询采购验收单详情
     */
    PmsAcceptanceVo queryById(Long id);

    /**
     * 根据 ID 取原始实体（用于状态校验）
     */
    PmsAcceptance getById(Long id);

    /**
     * 查询采购验收单分页列表
     */
    PageResult<PmsAcceptanceVo> queryPageList(PmsAcceptanceBo bo, PageQuery pageQuery);

    /**
     * 查询采购验收单列表
     */
    List<PmsAcceptanceVo> queryList(PmsAcceptanceBo bo);

    /**
     * 新增采购验收单
     */
    Boolean insertByBo(PmsAcceptanceBo bo);

    /**
     * 修改采购验收单
     */
    Boolean updateByBo(PmsAcceptanceBo bo);

    /**
     * 提交验收并启动流程：验收人→采购申请人→项目负责人→团队上级→结束
     *
     * @param bo 验收单
     * @return 验收单详情
     */
    PmsAcceptanceVo submitAndStartFlow(PmsAcceptanceBo bo);

    /**
     * 校验并删除采购验收单
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<PmsAcceptance> list);
}
