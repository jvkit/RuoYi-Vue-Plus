package cn.jvkit.spdemo.controller;

import cn.jvkit.spdemo.common.Result;
import cn.jvkit.spdemo.entity.User;
import cn.jvkit.spdemo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理接口
 *
 * 演示：Controller 调用 Service，Service 处理业务数据
 */
@RestController
@RequestMapping("/users")
public class UserController {

    /**
     * 构造方法注入：Spring 会自动找到 UserService 的实现类，传进来
     *
     * 这里声明的是接口 UserService，不是具体实现类
     */
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 查询所有用户
     * GET /users
     */
    @GetMapping
    public Result<List<User>> list() {
        return Result.ok(userService.list());
    }

    /**
     * 根据 ID 查询用户
     * GET /users/1
     */
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        return Result.ok(user);
    }

    /**
     * 新增用户
     * POST /users
     * 请求体：{"name":"王五","age":30}
     */
    @PostMapping
    public Result<Long> add(@RequestBody User user) {
        Long id = userService.add(user);
        return Result.ok(id);
    }

    /**
     * 更新用户
     * PUT /users/1
     * 请求体：{"name":"王五","age":31}
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody User user) {
        userService.update(id, user);
        return Result.ok();
    }

    /**
     * 删除用户
     * DELETE /users/1
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

}
