package org.dromara.procurement.service.impl;

import org.dromara.procurement.domain.vo.PmsProcurementCategoryNode;
import org.dromara.procurement.service.IPmsProcurementCategoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 采购分类Service业务层处理
 * <p>
 * 分类为固定数据（来自《8.0105淘宝件采购.xlsx》二级分类目录），后续如需动态维护可改为表驱动。
 *
 * @author procurement
 */
@Service
public class PmsProcurementCategoryServiceImpl implements IPmsProcurementCategoryService {

    @Override
    public List<PmsProcurementCategoryNode> queryCategoryTree() {
        List<PmsProcurementCategoryNode> tree = new ArrayList<>();

        tree.add(node("100材料",
            "101金属材料", "102高分子材料", "103实验样品", "104光学实验材料", "105化学试剂",
            "106气体", "107理化实验耗材", "108生物实验耗材", "109实验动物", "110细胞",
            "111金属器件", "112科研类低值易耗品", "113超净间耗材"));

        tree.add(node("600其他",
            "101劳保用品", "102应急物资", "103维修工具", "104防护器具", "105办公用品",
            "106电子设备", "107办公家具", "108实验室配套设施、工具"));

        tree.add(node("700危险化学品",
            "101易制毒", "102易制爆", "103一般化学品"));

        tree.add(node("800设备",
            "101测试检测设备", "102材料加工设备", "103材料复合设备", "104材料处理设备",
            "105信息数据处理算力", "106光学设备", "107理化实验设备", "108生物实验设备",
            "109非标定制设备", "110实验工装夹治具", "111实验室基础配套设备", "112设备配件",
            "113动力及环保设备"));

        // 900费用 无二级，自己即叶子
        tree.add(node("900费用"));

        return tree;
    }

    private PmsProcurementCategoryNode node(String value, String... children) {
        PmsProcurementCategoryNode n = new PmsProcurementCategoryNode();
        n.setValue(value);
        n.setLabel(value);
        if (children.length > 0) {
            List<PmsProcurementCategoryNode> list = new ArrayList<>();
            for (String c : children) {
                PmsProcurementCategoryNode child = new PmsProcurementCategoryNode();
                child.setValue(c);
                child.setLabel(c);
                child.setChildren(new ArrayList<>());
                list.add(child);
            }
            n.setChildren(list);
        } else {
            n.setChildren(new ArrayList<>());
        }
        return n;
    }

}
