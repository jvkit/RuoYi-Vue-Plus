package org.dromara.procurement.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.procurement.domain.PmsIssueRequest;
import org.dromara.procurement.domain.bo.PmsIssueRequestBo;
import org.dromara.procurement.domain.vo.PmsIssueRequestVo;

import java.util.Collection;
import java.util.List;

/**
 * 采购管理-领用申请Service接口
 *
 * @author procurement
 */
public interface IPmsIssueRequestService {

    /**
     * 查询领用申请详情
     */
    PmsIssueRequestVo queryById(Long id);

    /**
     * 查询领用申请分页列表
     */
    PageResult<PmsIssueRequestVo> queryPageList(PmsIssueRequestBo bo, PageQuery pageQuery);

    /**
     * 查询领用申请列表
     */
    List<PmsIssueRequestVo> queryList(PmsIssueRequestBo bo);

    /**
     * 新增领用申请
     */
    Boolean insertByBo(PmsIssueRequestBo bo);

    /**
     * 修改领用申请
     */
    Boolean updateByBo(PmsIssueRequestBo bo);

    /**
     * 审批领用申请
     *
     * @param id     领用申请ID
     * @param status 目标状态（approved已通过 rejected已驳回）
     */
    Boolean approve(Long id, String status);

    /**
     * 提交领用申请并启动流程：发起人 → 仓库管理员 → 结束
     *
     * @param bo 领用申请
     * @return 领用申请详情
     */
    PmsIssueRequestVo submitAndStartFlow(PmsIssueRequestBo bo);

    /**
     * 校验并删除领用申请
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量保存
     */
    Boolean saveBatch(List<PmsIssueRequest> list);
}
