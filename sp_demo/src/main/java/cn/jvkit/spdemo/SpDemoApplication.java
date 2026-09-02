package cn.jvkit.spdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 应用入口
 *
 * @SpringBootApplication 是一个组合注解，包含：
 * 1. @Configuration：表示这是一个配置类
 * 2. @EnableAutoConfiguration：开启自动配置
 * 3. @ComponentScan：自动扫描当前包及其子包下的组件
 */
@SpringBootApplication
public class SpDemoApplication {

    public static void main(String[] args) {
        // SpringApplication.run 会启动 Spring 容器、启动内嵌 Tomcat、扫描组件
        SpringApplication.run(SpDemoApplication.class, args);
    }

}
