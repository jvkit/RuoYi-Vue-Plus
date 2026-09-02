package cn.jvkit.spdemo.service.impl;

import cn.jvkit.spdemo.entity.User;
import cn.jvkit.spdemo.service.UserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户业务实现类
 *
 * @Service 表示交给 Spring 管理
 * implements UserService 表示实现了 UserService 接口
 */
@Service
public class UserServiceImpl implements UserService {

    /**
     * 用内存 Map 模拟数据库
     */
    private final Map<Long, User> userStore = new ConcurrentHashMap<>();

    /**
     * 自增 ID 生成器
     * 初始值设为 2，因为下面已经放了 ID 为 1 和 2 的数据
     */
    private final AtomicLong idGenerator = new AtomicLong(2);

    /**
     * 构造方法里先放几条测试数据
     */
    public UserServiceImpl() {
        userStore.put(1L, new User(1L, "张三", 20));
        userStore.put(2L, new User(2L, "李四", 25));
        System.out.println("UserServiceImpl 被创建了，只会创建一次（单例）");
    }

    @Override
    public List<User> list() {
        return new ArrayList<>(userStore.values());
    }

    @Override
    public User getById(Long id) {
        return userStore.get(id);
    }

    @Override
    public Long add(User user) {
        Long id = idGenerator.incrementAndGet();
        user.setId(id);
        userStore.put(id, user);
        return id;
    }

    @Override
    public void update(Long id, User user) {
        user.setId(id);
        userStore.put(id, user);
    }

    @Override
    public void delete(Long id) {
        userStore.remove(id);
    }


    @Override
    public boolean exists(Long id) {
        return userStore.containsKey(id);
    }    

}
