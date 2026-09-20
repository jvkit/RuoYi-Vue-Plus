import { chromium } from "playwright";          // ← 改成 import

const browser = await chromium.launch({ headless: false });
const page = await browser.newPage();           // codegen 的 context.newPage() 合并简写

await page.goto("https://news.163.com/", { waitUntil: "networkidle" });

// ↓↓↓ 这三行是你要加的：把选择器塞进来，把文字抠出来 ↓↓↓
const locator = page.locator("#js_top_news");   // ← 你悬停拿到的那个
const text = await locator.innerText();          // ← 永远是这个模板
console.log("==== 头条区域文字 ====");
console.log(text);

await page.pause();   // 弹出 Playwright Inspector，浏览器保持打开

await browser.close();
