jvkit@jvkitw:~/workspace/oa/RuoYi-Vue-Plus$  /usr/bin/env /usr/lib/jvm/java-17-openjdk-amd64/bin/java -agentlib:jdwp=transport=dt_socket,server=n,suspend=y,address=localhost:46017 @/tmp/cp_69b5az3bcnxgg1y8h60mwjr6f.argfile org.dromara.DromaraApplication --sa-token.timeout=31536000 
Application Version: 5.6.2
Spring Boot Version: 3.5.15
__________            _____.___.__         ____   ____                     __________.__
\______   \__ __  ____\__  |   |__|        \   \ /   /_ __   ____          \______   \  |  __ __  ______
 |       _/  |  \/  _ \/   |   |  |  ______ \   Y   /  |  \_/ __ \   ______ |     ___/  | |  |  \/  ___/
 |    |   \  |  (  <_> )____   |  | /_____/  \     /|  |  /\  ___/  /_____/ |    |   |  |_|  |  /\___ \
 |____|_  /____/ \____// ______|__|           \___/ |____/  \___  >         |____|   |____/____//____  >
        \/             \/                                       \/                                   \/

2026-08-04 15:54:44 [background-preinit] INFO  o.h.validator.internal.util.Version
 - HV000001: Hibernate Validator 8.0.3.Final
2026-08-04 15:54:44 [main] INFO  org.dromara.DromaraApplication
 - Starting DromaraApplication using Java 17.0.19 with PID 487971 (/home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-admin/target/classes started by jvkit in /home/jvkit/workspace/oa/RuoYi-Vue-Plus)
2026-08-04 15:54:44 [main] INFO  org.dromara.DromaraApplication
 - The following 1 profile is active: "dev"
2026-08-04 15:54:48 [main] WARN  o.s.b.w.s.c.AnnotationConfigServletWebServerApplicationContext
 - Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.CannotLoadBeanClassException: Error loading class [org.dromara.common.log.event.OperLogEventToSysOperLogBoMapperImpl] for bean with name 'operLogEventToSysOperLogBoMapperImpl' defined in file [/home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-admin/target/classes/org/dromara/common/log/event/OperLogEventToSysOperLogBoMapperImpl.class]: problem with class file or dependent class
2026-08-04 15:54:48 [main] ERROR o.s.boot.SpringApplication
 - Application run failed
org.springframework.beans.factory.CannotLoadBeanClassException: Error loading class [org.dromara.common.log.event.OperLogEventToSysOperLogBoMapperImpl] for bean with name 'operLogEventToSysOperLogBoMapperImpl' defined in file [/home/jvkit/workspace/oa/RuoYi-Vue-Plus/ruoyi-admin/target/classes/org/dromara/common/log/event/OperLogEventToSysOperLogBoMapperImpl.class]: problem with class file or dependent class
        at org.springframework.beans.factory.support.AbstractBeanFactory.resolveBeanClass(AbstractBeanFactory.java:1587)
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.determineTargetType(AbstractAutowireCapableBeanFactory.java:690)
        at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.predictBeanType(AbstractAutowireCapableBeanFactory.java:658)
        at org.springframework.beans.factory.support.AbstractBeanFactory.isFactoryBean(AbstractBeanFactory.java:1715)
        at org.springframework.beans.factory.support.DefaultListableBeanFactory.doGetBeanNamesForType(DefaultListableBeanFactory.java:640)
        at org.springframework.beans.factory.support.DefaultListableBeanFactory.getBeanNamesForType(DefaultListableBeanFactory.java:612)
        at org.springframework.beans.factory.support.DefaultListableBeanFactory.getBeanNamesForType(DefaultListableBeanFactory.java:606)
        at com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration$AutoConfiguredMapperScannerRegistrar.getBeanNameForType(MybatisPlusAutoConfiguration.java:367)
        at com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration$AutoConfiguredMapperScannerRegistrar.registerBeanDefinitions(MybatisPlusAutoConfiguration.java:341)
        at org.springframework.context.annotation.ImportBeanDefinitionRegistrar.registerBeanDefinitions(ImportBeanDefinitionRegistrar.java:86)
        at org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader.lambda$loadBeanDefinitionsFromRegistrars$1(ConfigurationClassBeanDefinitionReader.java:409)
        at java.base/java.util.LinkedHashMap.forEach(LinkedHashMap.java:721)
        at org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader.loadBeanDefinitionsFromRegistrars(ConfigurationClassBeanDefinitionReader.java:408)
        at org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader.loadBeanDefinitionsForConfigurationClass(ConfigurationClassBeanDefinitionReader.java:148)
        at org.springframework.context.annotation.ConfigurationClassBeanDefinitionReader.loadBeanDefinitions(ConfigurationClassBeanDefinitionReader.java:120)
        at org.springframework.context.annotation.ConfigurationClassPostProcessor.processConfigBeanDefinitions(ConfigurationClassPostProcessor.java:430)
        at org.springframework.context.annotation.ConfigurationClassPostProcessor.postProcessBeanDefinitionRegistry(ConfigurationClassPostProcessor.java:290)
        at org.springframework.context.support.PostProcessorRegistrationDelegate.invokeBeanDefinitionRegistryPostProcessors(PostProcessorRegistrationDelegate.java:349)
        at org.springframework.context.support.PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(PostProcessorRegistrationDelegate.java:118)
        at org.springframework.context.support.AbstractApplicationContext.invokeBeanFactoryPostProcessors(AbstractApplicationContext.java:792)
        at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:610)
        at org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext.refresh(ServletWebServerApplicationContext.java:146)
        at org.springframework.boot.SpringApplication.refresh(SpringApplication.java:752)
        at org.springframework.boot.SpringApplication.refreshContext(SpringApplication.java:439)
        at org.springframework.boot.SpringApplication.run(SpringApplication.java:318)
        at org.dromara.DromaraApplication.main(DromaraApplication.java:19)
Caused by: java.lang.ClassFormatError: Duplicate method name "convert" with signature "(LOperLogEvent;)Lorg.dromara.system.domain.bo.SysOperLogBo;" in class file org/dromara/common/log/event/OperLogEventToSysOperLogBoMapperImpl
        at java.base/java.lang.ClassLoader.defineClass1(Native Method)
        at java.base/java.lang.ClassLoader.defineClass(ClassLoader.java:1017)
        at java.base/java.security.SecureClassLoader.defineClass(SecureClassLoader.java:150)
        at java.base/jdk.internal.loader.BuiltinClassLoader.defineClass(BuiltinClassLoader.java:862)
        at java.base/jdk.internal.loader.BuiltinClassLoader.findClassOnClassPathOrNull(BuiltinClassLoader.java:760)
        at java.base/jdk.internal.loader.BuiltinClassLoader.loadClassOrNull(BuiltinClassLoader.java:681)
        at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:639)
        at java.base/jdk.internal.loader.ClassLoaders$AppClassLoader.loadClass(ClassLoaders.java:188)
        at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:525)
        at java.base/java.lang.Class.forName0(Native Method)
        at java.base/java.lang.Class.forName(Class.java:469)
        at org.springframework.util.ClassUtils.forName(ClassUtils.java:321)
        at org.springframework.beans.factory.support.AbstractBeanDefinition.resolveBeanClass(AbstractBeanDefinition.java:503)
        at org.springframework.beans.factory.support.AbstractBeanFactory.doResolveBeanClass(AbstractBeanFactory.java:1652)
        at org.springframework.beans.factory.support.AbstractBeanFactory.resolveBeanClass(AbstractBeanFactory.java:1577)
        ... 25 common frames omitted
jvkit@jvkitw:~/workspace/oa/RuoYi-Vue-Plus$ 