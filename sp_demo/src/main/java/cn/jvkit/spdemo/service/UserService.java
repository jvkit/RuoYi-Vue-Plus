package cn.jvkit.spdemo.service;

import cn.jvkit.spdemo.entity.User;

import java.util.List;

/**
 * 用户业务接口
 *
 * 接口只定义"要做什么"，不定义"怎么做"
 */
public interface UserService {

    /**
     * 查询所有用户
     */
    List<User> list();

    /**
     * 根据 ID 查询用户
     */
    User getById(Long id);

    /**
     * 新增用户
     */
    Long add(User user);

    /**
     * 更新用户
     */
    void update(Long id, User user);

    /**
     * 删除用户
     */
    void delete(Long id);

    /**
     * 根据 ID 判断用户是否存在
     */
    boolean exists(Long id);

}
