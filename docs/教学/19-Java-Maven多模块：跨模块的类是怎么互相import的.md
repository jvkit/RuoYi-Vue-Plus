# 19. Java + Maven 多模块：跨模块的类是怎么互相 import 的

## 1. 先摆正一个观念：classpath 不是一个真实文件夹

classpath（类路径）是 Java 运行时的**查找清单**，可以包含多个目录或 jar 包。

它不是磁盘上一个叫 `classpath/` 的真实文件夹，而是 JVM 内部维护的多个"入口"的集合。

每个入口可以是：

- 一个目录：`/xxx/target/classes/`
- 一个 jar 文件：`/xxx/ruoyi-common-core-5.6.2.jar`

JVM 找类时，会依次到每个入口里按 package 路径查找。

## 2. 用我们项目的真实绝对路径来看

工作区根目录：

```text
/home/jvkit/workspace/oa/
```

后端项目根目录：

```text
/home/jvkit/workspace/oa/RuoYi-Vue-Plus/
```

下面是三个关键模块的**完整绝对路径**。

### 2.1 ruoyi-invoice 模块

源代码文件：

```text
/home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-modules/ruoyi-invoice/src/main/java/org/dromara/invoice/controller/InvoiceInfoController.java
```

编译后的 class 文件：

```text
/home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-modules/ruoyi-invoice/target/classes/org/dromara/invoice/controller/InvoiceInfoController.class
```

### 2.2 ruoyi-common-core 模块

源代码文件：

```text
/home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-common/ruoyi-common-core/src/main/java/org/dromara/common/core/domain/R.java
```

编译后的 class 文件：

```text
/home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-common/ruoyi-common-core/target/classes/org/dromara/common/core/domain/R.class
```

### 2.3 ruoyi-admin 模块

源代码文件：

```text
/home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-admin/src/main/java/org/dromara/DromaraApplication.java
```

编译后的 class 文件：

```text
/home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-admin/target/classes/org/dromara/DromaraApplication.class
```

## 3. 关键发现：class 文件比源文件少了 `src/main/java` 这一层

对比同一模块的源文件和 class 文件：

源文件：

```text
.../ruoyi-invoice/src/main/java/org/dromara/invoice/controller/InvoiceInfoController.java
                ↑↑↑↑↑↑↑↑↑
                这一层是源码根
```

class 文件：

```text
.../ruoyi-invoice/target/classes/org/dromara/invoice/controller/InvoiceInfoController.class
                ↑↑↑↑↑↑↑↑↑↑
                这一层是编译产物根
```

注意：

- 源文件路径 = `模块根目录/src/main/java/` + `package 路径` + `类名.java`
- class 文件路径 = `模块根目录/target/classes/` + `package 路径` + `类名.class`

**`src/main/java/` 和 `target/classes/` 是不同模块各自独立的根目录。**

`src/main/java/` 只存在源代码阶段。
`target/classes/` 只存在编译产物阶段。

JVM 运行时只看 `target/classes/`，不看 `src/main/java/`。

## 4. 什么是"classpath 入口"

对于 JVM 来说，每个模块的 `target/classes/` 目录就是一个独立的 classpath 入口。

`ruoyi-admin` 运行时，classpath 大致如下（简化版）：

```text
入口 1: /home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-admin/target/classes/
入口 2: /home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-modules/ruoyi-invoice/target/classes/
入口 3: /home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-common/ruoyi-common-core/target/classes/
入口 4: /home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-common/ruoyi-common-log/target/classes/
入口 5: /home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-common/ruoyi-common-idempotent/target/classes/
入口 6: ~/.m2/repository/org/springframework/boot/spring-boot/3.x.x/spring-boot-3.x.x.jar
入口 7: ~/.m2/repository/com/baomidou/mybatis-plus-core/3.x.x/mybatis-plus-core-3.x.x.jar
... 更多 jar
```

每个入口都是平等的，JVM 都会去里面找类。

## 5. JVM 是怎么找类的

当代码里写：

```java
import org.dromara.common.core.domain.R;
```

JVM 会把包名转换成路径：

```text
org.dromara.common.core.domain.R
        ↓
org/dromara/common/core/domain/R.class
```

然后依次到每个 classpath 入口后面拼接这个路径：

```text
入口 1/org/dromara/common/core/domain/R.class   ← 找不到
入口 2/org/dromara/common/core/domain/R.class   ← 找不到
入口 3/org/dromara/common/core/domain/R.class   ← 找到了！
```

展开入口 3 的完整路径：

```text
/home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-common/ruoyi-common-core/target/classes/org/dromara/common/core/domain/R.class
```

所以：

> **import 的包名决定相对路径，classpath 入口决定从哪个绝对路径开始拼接。**

## 6. 为什么不同模块的父目录不一样没关系

你看到的：

```text
/home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-modules/ruoyi-invoice/target/classes/org/dromara/invoice/controller/InvoiceInfoController.class
/home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-common/ruoyi-common-core/target/classes/org/dromara/common/core/domain/R.class
```

这两个文件的父目录确实不一样，但 JVM 不关心完整父目录。

JVM 只关心：

1. classpath 入口是什么
2. package 后面的相对路径是什么

拆开看：

```text
入口: /home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-invoice/target/classes/
相对路径: org/dromara/invoice/controller/InvoiceInfoController.class

入口: /home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-common/ruoyi-common-core/target/classes/
相对路径: org/dromara/common/core/domain/R.class
```

只要入口后面的相对路径能拼接出正确的 package 路径，就能找到类。

入口前面是什么（`ruoyi-modules` 还是 `ruoyi-common`）完全不影响。

## 7. 一个形象的比喻

想象 classpath 是一排抽屉柜：

```text
抽屉柜 1  （ruoyi-admin/target/classes/）
抽屉柜 2  （ruoyi-invoice/target/classes/）
抽屉柜 3  （ruoyi-common-core/target/classes/）
抽屉柜 4  （ruoyi-common-log/target/classes/）
抽屉柜 5  （某个 Maven jar 包）
```

每个抽屉柜里都有按照 `org/dromara/...` 分类的抽屉。

当 InvoiceInfoController 需要 `R.class` 时，JVM 就拿着标签 `org/dromara/common/core/domain/R`，依次打开每个抽屉柜，找这个标签对应的抽屉。

最终在抽屉柜 3 里找到了。

## 8. Maven 在这里做了什么

Maven 主要做两件事：

### 8.1 编译

把每个模块的 `src/main/java/` 下的 `.java` 文件编译成 `target/classes/` 下的 `.class` 文件。

例如：

```text
ruoyi-invoice/src/main/java/org/dromara/invoice/controller/InvoiceInfoController.java
        ↓ Maven 编译
ruoyi-invoice/target/classes/org/dromara/invoice/controller/InvoiceInfoController.class
```

### 8.2 管理依赖

读取每个模块的 `pom.xml`，把被依赖模块的 `target/classes/` 加入当前模块的 classpath。

例如 `ruoyi-invoice/pom.xml` 里有：

```xml
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-common-core</artifactId>
</dependency>
```

Maven 就会告诉编译器：

> 编译 `ruoyi-invoice` 时，把 `ruoyi-common-core/target/classes/` 加入 classpath。

这样 `ruoyi-invoice` 里的代码才能 import `ruoyi-common-core` 里的类。

## 9. 没有 pom.xml 依赖就一定找不到

假设把 `ruoyi-invoice/pom.xml` 里的 `ruoyi-common-core` 依赖删掉：

```xml
<!-- 删除这个依赖 -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-common-core</artifactId>
</dependency>
```

编译 `ruoyi-invoice` 时，classpath 里就没有 `ruoyi-common-core/target/classes/` 这个入口了。

代码里的：

```java
import org.dromara.common.core.domain.R;
```

JVM 会依次在所有入口后面拼接：

```text
ruoyi-invoice/target/classes/org/dromara/common/core/domain/R.class   ← 没有
其他入口/org/dromara/common/core/domain/R.class                       ← 没有
```

然后报错：

```text
程序包 org.dromara.common.core.domain 不存在
```

## 10. 实际验证命令

你可以在项目里执行这些命令，亲眼看到 classpath 和 class 文件。

### 10.1 查看编译产物

```bash
cd /home/jvkit/workspace/oa/RuoYi-Vue-Plus
mvn compile -pl ruoyi-modules/ruoyi-invoice -am -q
```

然后：

```bash
find /home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-modules/ruoyi-invoice/target/classes -name "InvoiceInfoController.class"
```

输出：

```text
/home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-modules/ruoyi-invoice/target/classes/org/dromara/invoice/controller/InvoiceInfoController.class
```

### 10.2 查看 classpath 入口

打印 `ruoyi-invoice` 模块编译时的 classpath：

```bash
cd /home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-modules/ruoyi-invoice
mvn dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt -q
cat /tmp/cp.txt | tr ':' '\n' | head -20
```

你会看到一长串路径，包括：

- 其他模块的 `target/classes/`
- Maven 仓库里的 jar 包

### 10.3 验证 class 文件在 jar 包里

打包后：

```bash
cd /home/jvkit/workspace/oa/RuoYi-Vue-Plus
mvn package -pl ruoyi-admin -am -DskipTests -q
```

然后查看 jar 包内容：

```bash
jar tf /home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-admin/target/ruoyi-admin.jar | grep "ruoyi-common-core"
```

输出类似：

```text
BOOT-INF/lib/ruoyi-common-core-5.6.2.jar
```

再看这个 jar 包里有没有 R.class：

```bash
jar tf /home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-admin/target/ruoyi-admin.jar | grep "BOOT-INF/lib/ruoyi-common-core"
unzip -l /home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-admin/target/ruoyi-admin.jar | grep "org/dromara/common/core/domain/R.class"
```

## 11. 为什么 package 路径要全局唯一

每个类都有完整限定名：`package + 类名`。

例如：

```text
org.dromara.common.core.domain.R
org.dromara.invoice.controller.InvoiceInfoController
org.dromara.system.domain.SysUser
```

这些完整限定名在 classpath 中必须唯一。

如果两个模块都定义了：

```text
org.dromara.common.core.domain.R
```

JVM 会先找到哪个就用哪个，导致不可预测的行为。

所以项目名要用公司域名反写 + 模块名 + 功能名，保证不冲突：

```text
org.dromara.模块名.功能名
```

## 12. 总结

1. **classpath 是多个入口的集合**，每个入口可以是一个目录或 jar 包。

2. **源代码根是 `src/main/java/`，编译产物根是 `target/classes/`**。每个模块各有一个，互相独立。

3. **JVM 运行时只看 `target/classes/`，不看 `src/main/java/`**。

4. **import 的包名 = 相对路径**。`org.dromara.common.core.domain.R` 对应 `org/dromara/common/core/domain/R.class`。

5. **JVM 找类 = classpath 入口 + 相对路径**。入口前面是什么父目录不重要。

6. **Maven 的作用**：
   - 把 `.java` 编译成 `.class`，输出到 `target/classes/`。
   - 根据 `pom.xml` 的 `<dependency>`，把被依赖模块的 `target/classes/` 加入 classpath。

7. **跨模块使用类的条件**：
   - 被使用的类必须已经被编译到 `target/classes/` 或 jar 包里。
   - 当前模块的 `pom.xml` 必须声明对被使用类所在模块的依赖。

记住这张图：

```text
多个模块的源代码：
  ruoyi-invoice/src/main/java/org/dromara/invoice/...
  ruoyi-common-core/src/main/java/org/dromara/common/core/...

Maven 编译后：
  ruoyi-invoice/target/classes/org/dromara/invoice/...
  ruoyi-common-core/target/classes/org/dromara/common/core/...

运行时 classpath（多个入口）：
  [入口1] .../ruoyi-invoice/target/classes/
  [入口2] .../ruoyi-common-core/target/classes/
  [入口3] .../某个第三方jar

import org.dromara.common.core.domain.R;
      ↓
查找：入口1/org/dromara/common/core/domain/R.class  不存在
      入口2/org/dromara/common/core/domain/R.class  存在！加载成功
```
