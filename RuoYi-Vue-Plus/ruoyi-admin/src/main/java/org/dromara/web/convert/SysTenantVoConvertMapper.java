package org.dromara.web.convert;

import io.github.linpeilie.BaseMapper;
import org.dromara.system.domain.vo.SysTenantVo;
import org.dromara.web.domain.vo.TenantListVo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 补充 mapstruct-plus 反向转换器缺失问题
 * <p>
 * 由于 org.dromara.system.domain.vo.SysTenantVoToTenantListVoMapperImpl
 * 在 ruoyi-admin 的 antrun 插件中被清理，且 ruoyi-system 模块无法生成该实现类，
 * 因此在此处显式提供一个 Spring Bean 转换器。
 */
@Component
public class SysTenantVoConvertMapper implements BaseMapper<SysTenantVo, TenantListVo> {

    @Override
    public TenantListVo convert(SysTenantVo source) {
        if (source == null) {
            return null;
        }
        TenantListVo target = new TenantListVo();
        target.setTenantId(source.getTenantId());
        target.setCompanyName(source.getCompanyName());
        target.setDomain(source.getDomain());
        return target;
    }

    @Override
    public TenantListVo convert(SysTenantVo source, TenantListVo target) {
        if (source == null) {
            return target;
        }
        if (target == null) {
            return convert(source);
        }
        target.setTenantId(source.getTenantId());
        target.setCompanyName(source.getCompanyName());
        target.setDomain(source.getDomain());
        return target;
    }

    @Override
    public List<TenantListVo> convert(List<SysTenantVo> sources) {
        if (sources == null) {
            return null;
        }
        List<TenantListVo> list = new ArrayList<>(sources.size());
        for (SysTenantVo source : sources) {
            list.add(convert(source));
        }
        return list;
    }
}
