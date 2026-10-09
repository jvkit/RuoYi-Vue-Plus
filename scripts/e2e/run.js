// OA 测试导演中心
// 独立 CLI：进入虚拟 shell 管理并运行测试脚本
//
// 用法：
//   node run.js              → 进入交互 shell（推荐）
//   node run.js <name>       → 直接跑某个测试
//   node run.js <name>@server→ 直接跑服务器版本
//   node run.js --help       → 非交互列出所有测试
//   node run.js --version    → 版本
//
// 交互命令（进入 shell 后）：
//   /help              显示帮助
//   /list              列出所有测试（标注目标环境）
//   /run <name>        运行测试（name 可带 @server / @local 后缀）
//   /combo <name>      运行组合测试
//   /combos            列出所有组合
//   /tag <关键词>       按标签/描述筛选测试
//   /info <name>       查看测试详情
//   /server            切换默认目标到服务器
//   /local             切换默认目标到本地
//   /target            显示当前默认目标
//   /quit | /exit      退出
//
// 环境变量（CI/脚本化用）：
//   OA_BASE=http://172.16.16.110/oa   指定目标（不进入 shell 时）
//   PW_AUTO=1                          无头模式
//   PW_HEADED=1                        有头但不暂停（director 支持）
//   PW_STEP=1                          分步调试（director 支持）
const path = require('path');
const fs = require('fs');
const readline = require('readline');

const VERSION = '2.0.0';

// ---------- 颜色 ----------
const C = {
  reset: '\x1b[0m', bright: '\x1b[1m',
  green: '\x1b[32m', cyan: '\x1b[36m',
  yellow: '\x1b[33m', red: '\x1b[31m', gray: '\x1b[90m'
};
const ok = m => console.log(`${C.green}✔ ${m}${C.reset}`);
const warn = m => console.log(`${C.yellow}⚠ ${m}${C.reset}`);
const err = m => console.log(`${C.red}✖ ${m}${C.reset}`);
const log = m => console.log(m ?? '');

// ---------- 目标环境 ----------
const SERVER_URL = 'http://172.16.16.110/oa';
const LOCAL_URL = 'http://127.0.0.1:8082';

function resolveTarget(name, defaultTarget) {
  // name 可带 @server / @local 后缀，覆盖默认值
  if (name.endsWith('@server')) return { base: SERVER_URL, short: name.replace('@server', ''), env: 'server' };
  if (name.endsWith('@local')) return { base: LOCAL_URL, short: name.replace('@local', ''), env: 'local' };
  return { base: defaultTarget.base, short: name, env: defaultTarget.env };
}

// ---------- 测试注册表 ----------
// target: 'both' | 'local' | 'server'（标注该测试适用于哪个环境）
// tags:   中文标签，供 /tag 筛选
const TESTS = [
  {
    name: 'smoke',
    file: 'smoke_test.js',
    target: 'both',
    tags: ['冒烟', '权限', '登录'],
    desc: '快速冒烟：登录 + 6 个关键页面 200/权限检查'
  },
  {
    name: 'director',
    file: 'director.js',
    target: 'both',
    tags: ['全流程', '导演', '采购', '验收', '入库', '出库'],
    desc: '完整导演模式：四窗口 2×2 摆位，30 步全链路'
  },
  {
    name: 'invoice',
    file: 'invoice_test.js',
    target: 'both',
    tags: ['发票', 'AI', '识别', '上传'],
    desc: '发票专项：采购→验收→两批 PDF AI 识别→补传→台账核对'
  },
  {
    name: 'reimburse',
    file: 'reimburse_test.js',
    target: 'both',
    tags: ['报销', '导出', '资金'],
    desc: '报销专项：选已验收单→生成报销包→导出 zip'
  }
];

// ---------- 组合测试 ----------
const COMBOS = {
  daily: { desc: '日常回归：冒烟 + 发票 + 报销', tests: ['smoke', 'invoice', 'reimburse'] },
  full: { desc: '完整链路：导演模式 30 步', tests: ['director'] },
  quick: { desc: '快速检查：仅冒烟', tests: ['smoke'] }
};

// ---------- 辅助 ----------
function findTest(name) {
  return TESTS.find(t => t.name === name || t.file === name);
}

function targetBadge(t) {
  if (t.target === 'both') return `${C.cyan}[通用]${C.reset}`;
  if (t.target === 'server') return `${C.yellow}[服务器]${C.reset}`;
  return `${C.green}[本地]${C.reset}`;
}

function pad(s, n) { return String(s).padEnd(n); }

function showList() {
  log();
  log(`${C.bright}可用测试${C.reset}（${TESTS.length} 个）`);
  log(`${C.gray}${'─'.repeat(72)}${C.reset}`);
  log(`${pad('名称', 14)}${pad('目标', 10)}${pad('标签', 24)}说明`);
  log(`${C.gray}${'─'.repeat(72)}${C.reset}`);
  for (const t of TESTS) {
    log(`${pad(t.name, 14)}${pad(targetBadge(t), 20)}${pad(t.tags.join('/'), 24)}${t.desc}`);
  }
  log();
  log(`${C.gray}提示：/run <name>@server 可临时指定目标环境${C.reset}`);
}

function showCombos() {
  log();
  log(`${C.bright}组合测试${C.reset}`);
  log(`${C.gray}${'─'.repeat(60)}${C.reset}`);
  for (const [k, v] of Object.entries(COMBOS)) {
    log(`${pad(k, 12)}${v.desc}  →  ${v.tests.join(' → ')}`);
  }
}

function showHelp() {
  log();
  log(`${C.bright}OA 测试导演中心${C.reset} ${C.gray}v${VERSION}${C.reset}`);
  log();
  log(`${C.cyan}命令：${C.reset}`);
  log('  /help              显示此帮助');
  log('  /list              列出所有测试（标注目标环境）');
  log('  /run <name>        运行测试（name 可带 @server / @local）');
  log('  /combo <name>      运行组合测试');
  log('  /combos            列出所有组合');
  log('  /tag <关键词>       按标签或描述筛选');
  log('  /info <name>       查看测试详情');
  log('  /server            默认目标切到服务器');
  log('  /local             默认目标切到本地');
  log('  /target            显示当前默认目标');
  log('  /quit | /exit      退出');
  log();
  log(`${C.cyan}非交互用法：${C.reset}`);
  log('  node run.js <name>          直接跑（用默认目标）');
  log('  node run.js <name>@server   直接跑服务器版本');
  log('  OA_BASE=<url> node run.js <name>  用自定义地址');
  log();
  log(`${C.cyan}环境变量：${C.reset}`);
  log('  PW_AUTO=1    无头模式（默认有头会暂停，加这个后台跑）');
  log('  PW_HEADED=1  有头但不暂停（director 支持）');
  log('  PW_STEP=1    分步调试，每步暂停（director 支持）');
  log();
  showList();
  showCombos();
}

function showInfo(name) {
  const t = findTest(name);
  if (!t) { err(`未知测试：${name}`); return; }
  log();
  log(`${C.bright}${t.name}${C.reset}`);
  log(`  文件：${t.file}`);
  log(`  目标：${targetBadge(t)}`);
  log(`  标签：${t.tags.join('、')}`);
  log(`  说明：${t.desc}`);
  log();
}

function searchTag(kw) {
  const hit = TESTS.filter(t =>
    t.tags.some(tag => tag.includes(kw)) ||
    t.desc.includes(kw) ||
    t.name.includes(kw)
  );
  if (!hit.length) { warn(`没有匹配「${kw}」的测试`); return; }
  log();
  log(`匹配「${kw}」的测试（${hit.length} 个）：`);
  hit.forEach(t => log(`  ${t.name}  ${targetBadge(t)}  ${t.desc}`));
}

// ---------- 运行单个测试 ----------
function runTest(name, defaultTarget) {
  const { base, short, env } = resolveTarget(name, defaultTarget);
  const t = findTest(short);
  if (!t) {
    err(`未知测试：${short}。/list 查看可用测试。`);
    return Promise.resolve(false);
  }
  // target 校验
  if (t.target === 'local' && env === 'server') {
    warn(`「${t.name}」仅支持本地，已自动切换到本地`);
    return runTest(short + '@local', defaultTarget);
  }
  if (t.target === 'server' && env === 'local') {
    warn(`「${t.name}」仅支持服务器，已自动切换到服务器`);
    return runTest(short + '@server', defaultTarget);
  }

  const filePath = path.join(__dirname, t.file);
  if (!fs.existsSync(filePath)) {
    err(`文件不存在：${t.file}`);
    return Promise.resolve(false);
  }

  log();
  log(`${C.bright}▶ ${t.name}${C.reset}  ${targetBadge(t)}`);
  log(`  ${t.desc}`);
  log(`  目标：${C.cyan}${base}${C.reset}`);
  log(`  文件：${t.file}`);
  log();

  return new Promise(resolve => {
    const child = require('child_process').fork(filePath, [], {
      cwd: __dirname,
      stdio: 'inherit',
      env: { ...process.env, OA_BASE: base, PW_AUTO: process.env.PW_AUTO || '' }
    });
    child.on('exit', code => {
      if (code === 0) ok(`${t.name} 完成`);
      else err(`${t.name} 退出码 ${code}`);
      resolve(code === 0);
    });
    child.on('error', e => { err(`启动失败：${e.message}`); resolve(false); });
  });
}

// ---------- 运行组合 ----------
async function runCombo(name, defaultTarget) {
  const combo = COMBOS[name];
  if (!combo) {
    err(`未知组合：${name}。/combos 查看可用组合。`);
    return false;
  }
  log();
  log(`${C.bright}▶ 组合：${name}${C.reset}  ${combo.desc}`);
  log(`  包含：${combo.tests.join(' → ')}`);
  log();
  let allOk = true;
  for (const t of combo.tests) {
    const pass = await runTest(t, defaultTarget);
    if (!pass) { allOk = false; err(`组合中断：「${t}」失败`); break; }
  }
  if (allOk) ok(`组合 ${name} 全部通过`);
  return allOk;
}

// ---------- 交互 Shell ----------
function startShell() {
  // 默认目标：环境变量 OA_BASE > 本地
  let defaultTarget = {
    base: process.env.OA_BASE || LOCAL_URL,
    env: process.env.OA_BASE ? 'server' : 'local'
  };

  log();
  log(`${C.bright}OA 测试导演中心${C.reset} ${C.gray}v${VERSION}${C.reset}`);
  log(`默认目标：${C.cyan}${defaultTarget.base}${C.reset}  （输 /help 查看命令）`);
  log();

  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    prompt: `oa-test[${defaultTarget.env}]> `
  });
  rl.prompt();

  rl.on('line', async (line) => {
    const input = line.trim();
    if (!input) { rl.prompt(); return; }

    const [cmd, ...args] = input.startsWith('/') ? input.slice(1).split(/\s+/) : input.split(/\s+/);

    switch (cmd.toLowerCase()) {
      case 'help': case '?': case 'h':
        showHelp(); break;
      case 'list': case 'ls': case 'l':
        showList(); break;
      case 'run': case 'r':
        if (!args[0]) { warn('用法：/run <name> 或 /run <name>@server'); break; }
        await runTest(args[0], defaultTarget); break;
      case 'combo': case 'c':
        if (!args[0]) { warn('用法：/combo <name>'); showCombos(); break; }
        await runCombo(args[0], defaultTarget); break;
      case 'combos':
        showCombos(); break;
      case 'tag': case 'search': case 't':
        if (!args[0]) { warn('用法：/tag <关键词>'); break; }
        searchTag(args[0]); break;
      case 'info': case 'i':
        if (!args[0]) { warn('用法：/info <name>'); break; }
        showInfo(args[0]); break;
      case 'server': case 's':
        defaultTarget = { base: SERVER_URL, env: 'server' };
        rl.setPrompt(`oa-test[server]> `);
        ok(`默认目标已切换到服务器 ${SERVER_URL}`); break;
      case 'local': case 'loc':
        defaultTarget = { base: LOCAL_URL, env: 'local' };
        rl.setPrompt(`oa-test[local]> `);
        ok(`默认目标已切换到本地 ${LOCAL_URL}`); break;
      case 'target':
        log(`当前默认目标：${C.cyan}${defaultTarget.base}${C.reset} (${defaultTarget.env})`); break;
      case 'quit': case 'exit': case 'q':
        log('👋 已退出'); process.exit(0); break;
      default:
        // 允许直接输测试名运行
        if (findTest(cmd)) { await runTest(cmd, defaultTarget); }
        else warn(`未知命令：${cmd}。/help 查看帮助。`);
    }
    rl.prompt();
  }).on('close', () => { log('\n👋 已退出'); process.exit(0); });
}

// ---------- 入口 ----------
function main() {
  const arg = process.argv[2];
  if (!arg) { startShell(); return; }
  if (arg === '--help' || arg === '-h') { showHelp(); return; }
  if (arg === '--version' || arg === '-v') { log(`oa-test-cli v${VERSION}`); return; }
  if (arg === 'list') { showList(); return; }
  if (arg === 'combos') { showCombos(); return; }

  // node run.js <name> 或 <name>@server
  const defaultTarget = {
    base: process.env.OA_BASE || LOCAL_URL,
    env: process.env.OA_BASE ? 'server' : 'local'
  };
  runTest(arg, defaultTarget).then(pass => process.exit(pass ? 0 : 1));
}

main();
