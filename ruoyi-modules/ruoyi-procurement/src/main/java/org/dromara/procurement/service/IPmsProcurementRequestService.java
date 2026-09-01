package org.dromara.procurement.service;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.PageResult;
import org.dromara.procurement.domain.PmsProcurementRequest;
import org.dromara.procurement.domain.bo.PmsProcurementRequestBo;
import org.dromara.procurement.domain.vo.PmsProcurementRequestVo;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-采购申请Service接口
 *
 * @author procurement
 */
public interface IPmsProcurementRequestService {

    /**
     * 查询采购申请详情（含明细）
     */
    PmsProcurementRequestVo queryById(Long id);

    /**
     * 根据 ID 取原始实体（用于状态校验）
     */
    PmsProcurementRequest getById(Long id);

    /**
     * 查询采购申请分页列表
     */
    PageResult<PmsProcurementRequestVo> queryPageList(PmsProcurementRequestBo bo, PageQuery pageQuery);

    /**
     * 查询采购申请列表
     */
    List<PmsProcurementRequestVo> queryList(PmsProcurementRequestBo bo);

    /**
     * 新增采购申请
     */
    Boolean insertByBo(PmsProcurementRequestBo bo);

    /**
     * 修改采购申请
     */
    Boolean updateByBo(PmsProcurementRequestBo bo);

    /**
     * 校验并删除采购申请
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 提交采购申请并启动流程
     */
    PmsProcurementRequestVo submitAndStartFlow(PmsProcurementRequestBo bo);

    /**
     * 查询已审批通过的采购申请（用于采购订单关联）
     */
    List<PmsProcurementRequestVo> queryApprovedList();

    /**
     * 查询可验收的采购申请（已审批通过且尚未创建验收单，验收下拉用）
     */
    List<PmsProcurementRequestVo> queryAcceptableList();

    /**
     * 导出采购申请表 Excel（按模板填充）
     *
     * @param id       采购申请ID
     * @param response HTTP响应
     */
    void exportFormExcel(Long id, HttpServletResponse response);

}
