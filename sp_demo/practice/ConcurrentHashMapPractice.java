import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConcurrentHashMap 接口练习
 *
 * 运行方式（JDK 11+ 单文件模式，无需编译打包）：
 *   java practice/ConcurrentHashMapPractice.java
 *
 * 每改一段代码就跑一次，观察输出变化。
 */
public class ConcurrentHashMapPractice {

    public static void main(String[] args) {

        ConcurrentHashMap<Long, String> map = new ConcurrentHashMap<>();

        // ========== 1. put / get ==========
        System.out.println("== 1. put / get ==");
        map.put(1L, "张三");
        map.put(2L, "李四");
        System.out.println("put 后整个 map: " + map);
        System.out.println("get(1)  = " + map.get(1L));
        System.out.println("get(99) = " + map.get(99L));   // key 不存在 -> null

        // ========== 2. size / isEmpty ==========
        System.out.println("\n== 2. size / isEmpty ==");
        System.out.println("size()    = " + map.size());
        System.out.println("isEmpty() = " + map.isEmpty());

        // ========== 3. containsKey ==========
        System.out.println("\n== 3. containsKey ==");
        System.out.println("containsKey(1)  = " + map.containsKey(1L));
        System.out.println("containsKey(99) = " + map.containsKey(99L));

        // ========== 4. putIfAbsent（幂等写入）==========
        System.out.println("\n== 4. putIfAbsent ==");
        System.out.println("putIfAbsent(1, '王五') 返回旧值 = " + map.putIfAbsent(1L, "王五"));
        System.out.println("1 -> " + map.get(1L) + "（key 已存在，没被覆盖）");
        map.putIfAbsent(3L, "王五");
        System.out.println("putIfAbsent(3, '王五') 返回 null（新放入），3 -> " + map.get(3L));

        // ========== 5. getOrDefault（省一次判空）==========
        System.out.println("\n== 5. getOrDefault ==");
        System.out.println("getOrDefault(2)        = " + map.getOrDefault(2L, ""));
        System.out.println("getOrDefault(99, 未知) = " + map.getOrDefault(99L, "未知"));

        // ========== 6. computeIfAbsent（懒加载/缓存常用）==========
        System.out.println("\n== 6. computeIfAbsent ==");
        map.computeIfAbsent(4L, k -> "现算出来的:" + k);
        System.out.println("第一次: 4 -> " + map.get(4L));
        map.computeIfAbsent(4L, k -> "这行不会执行");
        System.out.println("第二次: 4 -> " + map.get(4L) + "（已存在，lambda 不再执行）");

        // ========== 7. computeIfPresent（原地修改 value）==========
        System.out.println("\n== 7. computeIfPresent ==");
        map.computeIfPresent(1L, (k, v) -> v + "(改)");
        System.out.println("1 -> " + map.get(1L));
        map.computeIfPresent(99L, (k, v) -> "不会执行");
        System.out.println("99 -> " + map.get(99L) + "（key 不存在，不动）");

        // ========== 8. merge（计数经典用法）==========
        System.out.println("\n== 8. merge ==");
        ConcurrentHashMap<String, Integer> counter = new ConcurrentHashMap<>();
        for (String s : List.of("a", "b", "a", "a", "c", "b")) {
            counter.merge(s, 1, Integer::sum);   // 不存在放 1，存在则累加
        }
        System.out.println("计数结果: " + counter);

        // ========== 9. replace（CAS 式替换）==========
        System.out.println("\n== 9. replace ==");
        System.out.println("replace(2, '李四', '李老四') = "
                + map.replace(2L, "李四", "李老四") + "，2 -> " + map.get(2L));
        System.out.println("replace(2, '不存在的旧值', 'xxx') = "
                + map.replace(2L, "不存在的旧值", "xxx") + "，2 -> " + map.get(2L));

        // ========== 10. keySet / values / entrySet / forEach ==========
        System.out.println("\n== 10. 遍历视图 ==");
        System.out.println("keySet() = " + map.keySet());
        System.out.println("values() = " + map.values());
        System.out.println("forEach 逐对打印:");
        map.forEach((k, v) -> System.out.println("  " + k + " -> " + v));

        // ========== 11. remove ==========
        System.out.println("\n== 11. remove ==");
        System.out.println("remove(3) 返回 = " + map.remove(3L) + "（删到了）");
        System.out.println("remove(99) 返回 = " + map.remove(99L) + "（没有这个 key）");
        System.out.println("删除后: " + map);

        // ========== 12. 弱一致性遍历 ==========
        // 迭代时别的线程在写，不会抛 ConcurrentModificationException，
        // 但结果可能是"改动前/后"的任意快照。这里故意触发一次：
        System.out.println("\n== 12. 弱一致性遍历 ==");
        ConcurrentHashMap<Integer, String> busy = new ConcurrentHashMap<>();
        for (int i = 0; i < 5; i++) busy.put(i, "v" + i);
        Thread writer = new Thread(() -> {
            for (int i = 10; i < 15; i++) busy.put(i, "v" + i);   // 边遍历边塞
        });
        writer.start();
        System.out.println("遍历（期间另一线程在写入）:");
        busy.forEach((k, v) -> System.out.println("  " + k + " -> " + v));
        System.out.println("遍历结束 size = " + busy.size());
        System.out.println("注意：打印出来的条数可能是 5、6…10 中任意值，不报错也不保证最新");
    }
}
