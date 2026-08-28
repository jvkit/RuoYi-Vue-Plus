package org.dromara.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 热加载测试用 HelloWorld 接口
 */
@RestController
@RequestMapping("/hello")
public class HelloController {

    /**
     * GET /hello?name=xxx
     * 默认返回 "Hello World"
     */
    @GetMapping
    public String hello(@RequestParam(value = "name", required = false) String name) {
        if (name == null || name.isBlank()) {
            return "Hello World";
        }
        return "Hello " + name;
    }
}
