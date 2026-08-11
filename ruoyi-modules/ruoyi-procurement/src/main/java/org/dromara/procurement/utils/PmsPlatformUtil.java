package org.dromara.procurement.utils;

import org.dromara.common.core.utils.StringUtils;

/**
 * 采购平台识别工具
 *
 * <p>按"把平台视为供应商"的思路：从商品链接/标题识别平台（淘宝/天猫/京东/拼多多/1688/抖音/其他），
 * 供明细记录平台、并按平台匹配供应商使用。</p>
 *
 * @author procurement
 */
public class PmsPlatformUtil {

    private PmsPlatformUtil() {
    }

    /**
     * 从链接或标题文本识别平台。优先按链接域名判断，无链接时按标题结尾特征兜底。
     *
     * @param text 商品链接或标题（可为空）
     * @return 平台名；无法识别时返回"其他"，空文本返回空串
     */
    public static String detectPlatform(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        String t = text.toLowerCase();
        // 1) 链接域名精确识别
        if (t.contains("tmall.com")) {
            return "天猫";
        }
        if (t.contains("taobao.com")) {
            return "淘宝";
        }
        if (t.contains("jd.com")) {
            return "京东";
        }
        if (t.contains("pinduoduo.com") || t.contains("yangkeduo.com")) {
            return "拼多多";
        }
        if (t.contains("1688.com")) {
            return "1688";
        }
        if (t.contains("douyin.com")) {
            return "抖音";
        }
        // 2) 无链接，按标题/结尾特征兜底（如 "-tm"=淘宝、"-jd"=京东）
        if (t.contains("淘宝") || t.contains("-tm") || t.contains("taobao")) {
            return "淘宝";
        }
        if (t.contains("天猫") || t.contains("tmall")) {
            return "天猫";
        }
        if (t.contains("京东") || t.contains("-jd")) {
            return "京东";
        }
        if (t.contains("拼多多")) {
            return "拼多多";
        }
        if (t.contains("1688")) {
            return "1688";
        }
        if (t.contains("抖音")) {
            return "抖音";
        }
        return "其他";
    }
}
