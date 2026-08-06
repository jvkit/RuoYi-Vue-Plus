package org.dromara.system.controller.system;

import cn.dev33.satoken.annotation.SaIgnore;
import org.dromara.common.core.domain.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后端入门示例：最简单的接口
 *
 * @RestController：告诉 Spring Boot 这个类里的方法返回 JSON 数据，不是页面
 * @RequestMapping("/system/test")：这个类里所有接口的 URL 前缀都是 /system/test
 */
@RestController
@RequestMapping("/system/test")
public class TestHelloController {

    /**
     * @GetMapping("/hello")：表示用 GET 方法访问 /system/test/hello 时，执行这个方法
     * R<String>：RuoYi 统一的返回包装，{ "code": 200, "msg": "操作成功", "data": "..." }
     */
    /**
     * @SaIgnore：表示这个接口不需要登录/token，直接放行
     */
    @SaIgnore
    @GetMapping("/hello")
    public R<String> hello() {
        // R.ok(...) 是 RuoYi 提供的快捷方法，表示"成功"并带上数据
        return R.ok("操作成功", "Hello from backend!");
    }
}
