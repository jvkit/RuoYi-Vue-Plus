// ============================================================
// 第 1 步：打开网页，拿到 HTML 文本
// 运行：node crawler/step1-open.js
//
// 目的：证明 playwright 能打开网易新闻、拿到完整 HTML
// 学到：launch / newPage / goto / content 四个基本动作
// ============================================================

import { chromium } from "playwright";

import * as readline from "node:readline/promises"

// const rl = readline.createInterface({
//     input: process.stdin,
//     output:process.stdout,
// })

// ① 启动浏览器（headless: true = 不弹出窗口，后台跑）
//    相当于你双击 Chrome 图标，但看不见界面
const browser = await chromium.launch({ headless: false });

// ② 开一个新标签页（就像在浏览器里 Ctrl+T）
const page = await browser.newPage();

await page.pause();  

// ③ 跳转到目标网址
//    waitUntil: "networkidle" 的意思：
//    "等到网络安静下来再往下走"——图片、JS 都加载完了
//    如果不指定，默认 "load" 只等 HTML 加载完，JS 可能还没执行
await page.goto("https://news.163.com/", { waitUntil: "networkidle" });

// ④ 拿到"渲染后"的完整 HTML
//    注意：这里的 HTML 是浏览器执行完 JS 之后的最终样子
//    普通fetch拿到的是"空壳"，这里拿到的是"装修完的房子"
const html = await page.content();

console.log("HTML 总长度:", html.length);
console.log("页面标题:", await page.title());
console.log("\n===== HTML 开头 500 字 =====");
console.log(html.slice(0, 500));


// await rl.question("li kai ?")
// rl.close()


// ⑤ 用完关闭浏览器（不然进程挂着不退）
await browser.close();
