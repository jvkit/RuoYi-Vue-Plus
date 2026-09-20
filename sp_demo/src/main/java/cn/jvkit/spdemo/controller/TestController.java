package cn.jvkit.spdemo.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.ArrayList;
import  java.util.HashMap;
import java.util.List;
/**
 * 练习专用 Controller
 *
 * 所有练习接口都在 /test 前缀下，跟业务代码（/users、/hello）互不干扰
 * 练完想清场，直接删掉对应方法即可
 *
 * 练习方法：每加一个方法 -> 重启 -> curl 验证
 */
@RestController
@RequestMapping("/test")
public class TestController {

    // ========== 示范 1：最简单的 GET ==========
    // curl http://localhost:8080/test/ping
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    // ========== 示范 2：路径参数 @PathVariable ==========
    // curl http://localhost:8080/test/square/7   应返回 49
    @GetMapping("/square/{n}")
    public Long square(@PathVariable Long n) {
        return n * n;
    }

    @GetMapping ("/sum")
    public Long sum(@RequestParam Long a,@RequestParam Long b){
        return a+b;
    }

    @GetMapping ("/twice")
    public Long twice (@RequestParam(defaultValue = "5") Long a){
        return 2*a;
    }

    @PostMapping ("/echo")
    public Map<String,Object> echo(@RequestBody Map<String,Object> a){
        return a;

    }

    @GetMapping ("/play")
    public Map<String,Object> play (){
        Map<String,Object> map =new HashMap<String,Object>();
        map.put("name", "jvkit");
        map.put("feature",List.of("lovely","cute","mewo"));
        List<String> l=new ArrayList<>();
        l.addAll(List.of("weq","wqe","eqwe"));
        map.put("test",l);
        return map;
    }

    // ========== 练习 1：查询参数 @RequestParam ==========
    // 目标：curl "http://localhost:8080/test/sum?a=3&b=4" 返回 7
    // 提示：仿照 HelloController 的 /greet，两个参数各写一个 @RequestParam
    // TODO: 写一个 sum 方法

    // ========== 练习 2：带默认值的查询参数 ==========
    // 目标：curl "http://localhost:8080/test/twice?n=5" 返回 10
    //       curl "http://localhost:8080/test/twice"     也返回 10（n 没传时默认 5）
    // 提示：@RequestParam(defaultValue = "5")
    // TODO: 写一个 twice 方法

    // ========== 练习 3：POST + @RequestBody ==========
    // 目标：curl -X POST http://localhost:8080/test/echo \
    //          -H "Content-Type: application/json" \
    //          -d '{"name":"张三","age":20}'
    //       返回原样的 JSON
    // 提示：方法参数写 @RequestBody Map<String, Object> body，直接 return body
    //       Map<String, Object> 会被 Spring 自动转成 JSON，不用建新类
    // TODO: 写一个 echo 方法

    // ========== 练习 4：PUT 更新 ==========
    // 目标：curl -X PUT http://localhost:8080/test/price/3 \
    //          -H "Content-Type: application/json" \
    //          -d '{"price": 99}'
    //       返回 "id=3, 新价格=99.0"
    // 提示：路径参数和请求体可以同时接收，参数列表写两行
    // TODO: 写一个 updatePrice 方法

    // ========== 练习 5：DELETE ==========
    // 目标：curl -X DELETE http://localhost:8080/test/cache/1 返回 "已删除 1"
    // 提示：可以不用真删任何东西，先练通 HTTP 方法本身
    // TODO: 写一个 del 方法（注意 del 是 Java 关键字吗？不是，但变量名避开 delete 之外还得留意）
}

// =====================================================================================
//                              参 考 答 案（写完自己的再往下看）
//
//   练习 1：@RequestParam 接收 ?a=3&b=4
//       @GetMapping("/sum")
//       public long sum(@RequestParam Long a, @RequestParam Long b) {
//           return a + b;
//       }
//
//   练习 2：defaultValue 给默认值，不传参时 n=5
//       @GetMapping("/twice")
//       public long twice(@RequestParam(defaultValue = "5") Long n) {
//           return n * 2;
//       }
//
//   练习 3：POST + @RequestBody，JSON 自动转成 Map，再自动转回 JSON
//       @PostMapping("/echo")
//       public Map<String, Object> echo(@RequestBody Map<String, Object> body) {
//           return body;
//       }
//
//   练习 4：PUT，路径参数 + 请求体可以同时接收
//       @PutMapping("/price/{id}")
//       public String updatePrice(@PathVariable Long id, @RequestBody Map<String, Object> body) {
//           return "id=" + id + ", 新价格=" + body.get("price");
//       }
//
//   注意：body.get("price") 返回的是 Object，拼进字符串时是 99.0（Double）而不是 99
//       ——因为 JSON 的数字解析后默认是 Double。想显示 99 可以 (int) (double) body.get("price")
//
//   练习 5：DELETE，HTTP 方法本身也要用注解声明（@DeleteMapping），
//       和 GET/POST/PUT 一一对应
//       @DeleteMapping("/cache/{id}")
//       public String del(@PathVariable Long id) {
//           return "已删除 " + id;
//       }
//
//   验证命令汇总：
//       curl "http://localhost:8080/test/sum?a=3&b=4"
//       curl "http://localhost:8080/test/twice?n=5"
//       curl "http://localhost:8080/test/twice"
//       curl -X POST http://localhost:8080/test/echo -H "Content-Type: application/json" \
//            -d '{"name":"张三","age":20}'
//       curl -X PUT http://localhost:8080/test/price/3 -H "Content-Type: application/json" \
//            -d '{"price": 99}'
//       curl -X DELETE http://localhost:8080/test/cache/1
// =====================================================================================

