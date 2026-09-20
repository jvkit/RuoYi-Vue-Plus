import { chromium } from "playwright";

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();
await page.goto("https://news.163.com/", { waitUntil: "networkidle" });

const box = page.locator("#js_top_news");

// ① 它里面有几个“子元素块”？
console.log("=== 容器里的直接子节点数:", await box.locator(":scope > *").count());

// ② 里面一共有多少个 <a>（链接=通常是标题/入口）
console.log("=== <a> 数量:", await box.locator("a").count());

// ③ <img> 数量
console.log("=== <img> 数量:", await box.locator("img").count());

// ④ 每个 <a> 的文字 + href
console.log("\n=== 所有链接 ===");
const links = await box.locator("a").evaluateAll(els =>
  els.map(a => ({ text: a.innerText.trim().slice(0, 40), href: a.getAttribute("href") }))
);
links.forEach((l, i) => console.log(i, JSON.stringify(l, null, 0)));

await browser.close();
