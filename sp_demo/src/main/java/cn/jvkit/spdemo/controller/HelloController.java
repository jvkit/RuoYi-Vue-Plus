package cn.jvkit.spdemo.controller;

import cn.jvkit.spdemo.common.Result;
import cn.jvkit.spdemo.entity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第一个 Controller：Hello World
 *
 * @RestController = @Controller + @ResponseBody
 * 表示这个类里的方法返回的都是数据（通常是 JSON），不是页面
 */
@RestController
@RequestMapping("/hello")
public class HelloController {

    /**
     * 访问：http://localhost:8080/hello/world
     * 返回：字符串 "Hello, Spring Boot!"
     */
    @GetMapping("/world")
    public String helloWorld() {
        return "Hello, Spring Boot!";
    }

    /**
     * 访问：http://localhost:8080/hello/me
     * 返回：字符串
     */
    @GetMapping("/me")
    public String helloMe() {
        return "Hello, 学习者!";
    }

    /**
     * 返回 JSON 对象
     * 访问：http://localhost:8080/hello/user
     */
    
    @GetMapping("/user")
    public User helloUser() {
        return new User(1L, "张三", 20);
    }

    /**
     * 返回统一包装结果
     * 访问：http://localhost:8080/hello/result
     */
    @GetMapping("/result")
    public Result<User> helloResult() {
        User user = new User(1L, "张三", 20);
        return Result.ok(user);
    }

    /**
     * 从 URL 路径中取参数
     * 访问：http://localhost:8080/hello/user/100
     */
    @GetMapping("/user/{id}")
    public Result<User> helloPathVariable(@PathVariable Long id) {
        User user = new User(id, "用户" + id, 20);
        return Result.ok(user);
    }

    /**
     * 从 URL 查询参数中取参数
     * 访问：http://localhost:8080/hello/greet?name=李四
     */
    @GetMapping("/greet")
    public Result<String> helloRequestParam(@RequestParam(required = false) String name) {
        if (name == null || name.isEmpty()) {
            name = "陌生人";
        }
        return Result.ok("你好，" + name);
    }

}
