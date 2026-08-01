package org.dromara.workflow.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.domain.bo.ProcurementRequestBo;
import org.dromara.workflow.domain.vo.ProcurementRequestVo;

import java.util.List;

/**
 * 采购申请Service接口
 *
 * @author Lion Li
 */
public interface IProcurementRequestService {

    /**
     * 查询采购申请
     */
    ProcurementRequestVo queryById(Long id);

    /**
     * 查询采购申请列表
     */
    TableDataInfo<ProcurementRequestVo> queryPageList(ProcurementRequestBo bo, PageQuery pageQuery);

    /**
     * 查询采购申请列表
     */
    List<ProcurementRequestVo> queryList(ProcurementRequestBo bo);

    /**
     * 新增采购申请
     */
    ProcurementRequestVo insertByBo(ProcurementRequestBo bo);

    /**
     * 提交采购申请并发起流程
     */
    ProcurementRequestVo submitAndFlowStart(ProcurementRequestBo bo);

    /**
     * 修改采购申请
     */
    ProcurementRequestVo updateByBo(ProcurementRequestBo bo);

    /**
     * 校验并批量删除采购申请信息
     */
    Boolean deleteWithValidByIds(List<Long> ids);
}
