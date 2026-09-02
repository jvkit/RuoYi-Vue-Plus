package cn.jvkit.spdemo.cli;

import cn.jvkit.spdemo.common.Result;
import cn.jvkit.spdemo.entity.User;
import cn.jvkit.spdemo.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

/**
 * 交互式 CLI 入口
 *
 * 启动应用后，在控制台输入斜杠命令来测试后端功能
 */
@Component
public class CliRunner implements CommandLineRunner {

    private final UserService userService;

    public CliRunner(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        // 单独一个线程跑 CLI，避免阻塞 Spring Boot 启动日志
        new Thread(this::startCli).start();
    }

    private void startCli() {
        // 等 Spring Boot 启动信息打印完再显示 CLI 欢迎语
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {
        }

        Scanner scanner = new Scanner(System.in);

        System.out.println("\n==============================================");
        System.out.println("  sp_demo CLI 已启动");
        System.out.println("  输入 /help 查看所有命令");
        System.out.println("  同时 HTTP 服务也在 http://localhost:8080 运行");
        System.out.println("==============================================\n");

        while (true) {
            System.out.print("sp_demo> ");
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.equals("/exit") || line.equals("/quit")) {
                System.out.println("再见！");
                System.exit(0);
            }

            handleCommand(line);
        }
    }

    private void handleCommand(String line) {
        // 把命令按空格拆分
        String[] parts = line.split("\\s+");
        String command = parts[0];

        try {
            switch (command) {
                case "/help":
                case "/h":
                    printHelp();
                    break;

                case "/users":
                case "/list":
                    listUsers();
                    break;

                case "/user":
                    if (parts.length < 2) {
                        System.out.println("用法：/user <id>   例如：/user 1");
                        return;
                    }
                    getUser(Long.parseLong(parts[1]));
                    break;

                case "/add":
                    if (parts.length < 3) {
                        System.out.println("用法：/add <姓名> <年龄>   例如：/add 王五 30");
                        return;
                    }
                    addUser(parts[1], Integer.parseInt(parts[2]));
                    break;

                case "/update":
                    if (parts.length < 4) {
                        System.out.println("用法：/update <id> <姓名> <年龄>   例如：/update 1 张三 21");
                        return;
                    }
                    updateUser(Long.parseLong(parts[1]), parts[2], Integer.parseInt(parts[3]));
                    break;

                case "/delete":
                case "/del":
                    if (parts.length < 2) {
                        System.out.println("用法：/delete <id>   例如：/delete 1");
                        return;
                    }
                    deleteUser(Long.parseLong(parts[1]));
                    break;

                case "/hello":
                    System.out.println("Hello, CLI 学习者！");
                    break;

                default:
                    System.out.println("未知命令：" + command + "，输入 /help 查看帮助");
            }
        } catch (NumberFormatException e) {
            System.out.println("参数格式错误：id 和 age 必须是数字");
        } catch (Exception e) {
            System.out.println("执行出错：" + e.getMessage());
        }
    }

    private void printHelp() {
        System.out.println("可用命令：");
        System.out.println("  /help              显示帮助");
        System.out.println("  /users             列出所有用户");
        System.out.println("  /user <id>         查询指定用户");
        System.out.println("  /add <name> <age>  新增用户");
        System.out.println("  /update <id> <name> <age>  更新用户");
        System.out.println("  /delete <id>       删除用户");
        System.out.println("  /hello             打个招呼");
        System.out.println("  /exit              退出程序");
    }

    private void listUsers() {
        Result.ok(userService.list()).getData().forEach(user -> {
            System.out.println("  [" + user.getId() + "] " + user.getName() + "，年龄：" + user.getAge());
        });
    }

    private void getUser(Long id) {
        User user = userService.getById(id);
        if (user == null) {
            System.out.println("用户不存在：" + id);
            return;
        }
        System.out.println("  [" + user.getId() + "] " + user.getName() + "，年龄：" + user.getAge());
    }

    private void addUser(String name, Integer age) {
        User user = new User();
        user.setName(name);
        user.setAge(age);
        Long id = userService.add(user);
        System.out.println("新增成功，id = " + id);
    }

    private void updateUser(Long id, String name, Integer age) {
        User user = new User();
        user.setName(name);
        user.setAge(age);
        userService.update(id, user);
        System.out.println("更新成功：" + id);
    }

    private void deleteUser(Long id) {
        userService.delete(id);
        System.out.println("删除成功：" + id);
    }

}
