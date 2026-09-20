// 网易新闻爬虫 - 第三步：精准过滤，只抓新闻文章链接
// 运行：node crawl-163.js
import { chromium } from "playwright";

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();

await page.goto("https://news.163.com/", { waitUntil: "networkidle" });

// 只保留真正的新闻文章链接：含 /article/，且是 .html 结尾
const articles = await page.$$eval("a", (as) =>
    as.map((a) => ({
        text: a.textContent.trim(),
        href: a.href,
    }))
        .filter((x) =>
            x.text.length > 8
            && x.href.includes("/article/")
            && x.href.endsWith(".html")
        )
);

// 去重（同一篇文章可能出现在多个位置）
const seen = new Set();
const unique = articles.filter((a) => {
    if (seen.has(a.href)) return false;
    seen.add(a.href);
    return true;
});

console.log("文章链接数:", unique.length);
console.log("前 10 条新闻:");
unique.slice(0, 10).forEach((a, i) => console.log(`  ${i + 1}. [${a.text}]`));
console.log(`\n第一条链接: ${unique[0]?.href}`);

await browser.close();


