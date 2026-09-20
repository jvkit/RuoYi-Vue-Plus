// sp_demo 接口练习脚本
// 运行方式：node test-api.js
// 每个函数对应一个接口，想测哪个就取消哪个注释，或直接跑全部

const BASE = "http://localhost:8080";

// ---------- GET，带查询参数 ----------
async function sum() {
  const res = await fetch(`${BASE}/test/sum?a=3&b=4`);
  console.log("sum:", await res.json());
}

// ---------- GET，带默认值 ----------
async function twice() {
  const res = await fetch(`${BASE}/test/twice?n=5`);
  console.log("twice:", await res.json());
}

// ---------- POST，传 JSON 请求体 ----------
async function echo() {
  const res = await fetch(`${BASE}/test/echo`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name: "jvkit", age: 20 }),
  });
  console.log("echo:", await res.json());
}

// ---------- 练习：sum 带变量，体会拼接 ----------
async function sumWithVars(a, b) {
  const res = await fetch(`${BASE}/test/sum?a=${a}&b=${b}`);
  console.log(`sumWithVars(${a},${b}):`, await res.json());
}

// 逐个执行（想单独跑哪个就只留那一行）
async function main() {
  await sum();
  await twice();
  await echo();
  await sumWithVars(10, 5);
}

main().catch((e) => console.error("请求失败:", e.message));
