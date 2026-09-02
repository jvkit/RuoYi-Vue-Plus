package cn.jvkit.spdemo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户实体类
 *
 * @Data            自动生成 getter/setter/toString/equals/hashCode
 * @NoArgsConstructor   自动生成无参构造方法
 * @AllArgsConstructor  自动生成全参构造方法
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private String name;
    private Integer age;

}
