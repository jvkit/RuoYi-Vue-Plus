// 独立 CLI：OA 测试导演模式入口
// 用法：node run.js           （进入交互模式，输入 /help 查看命令）
//       node run.js <name>    （直接运行某个测试，例如 node run.js invoice）
//       node run.js --help    （非交互列出可用命令）
const path = require('path');
const readline = require('readline');

// ---------- 目标环境 ----------
// 默认本地 dev；测试服务器：OA_BASE=http://172.16.16.110/oa node run.js <name>
const SERVER_BASE = 'http://172.16.16.110/oa';

// ---------- 颜色与日志 ----------
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  green: '\x1b[32m',
  cyan: '\x1b[36m',
  yellow: '\x1b[33m',
  red: '\x1b[31m',
  gray: '\x1b[90m'
};

function log(msg = '') { console.log(msg); }
function ok(msg) { console.log(`${colors.green}✔ ${msg}${colors.reset}`); }
function warn(msg) { console.log(`${colors.yellow}⚠ ${msg}${colors.reset}`); }
function err(msg) { console.log(`${colors.red}✖ ${msg}${colors.reset}`); }

// ---------- 测试注册表 ----------
const TESTS_DIR = __dirname;
const registry = {
  full: {
    name: 'full',
    file: 'director.js',
    desc: '完整导演模式：四窗口 2×2 摆位，30 步全链路（采购→验收→入库→出库）',
    usage: '/run full',
    examples: ['node run.js full', 'PW_AUTO=1 node run.js full']
  },
  invoice: {
    name: 'invoice',
    file: 'invoice_test.js',
    desc: '发票专项：一条采购→验收→两批 PDF AI 识别→补传→台账核对',
    usage: '/run invoice',
    examples: ['node run.js invoice', 'PW_AUTO=1 node run.js invoice']
  },
  reimburse: {
    name: 'reimburse',
    file: 'reimburse_test.js',
    desc: '报销专项：选已验收单→生成报销包→导出 zip',
    usage: '/run reimburse',
    examples: ['node run.js reimburse', 'PW_AUTO=1 node run.js reimburse']
  },
  smoke: {
    name: 'smoke',
    file: 'smoke_test.js',
    desc: '快速冒烟：登录 + 关键页面 200 + 权限检查',
    usage: '/run smoke',
    examples: ['node run.js smoke', 'PW_AUTO=1 node run.js smoke']
  }
};

function showHelp() {
  log();
  log(`${colors.bright}OA 测试导演模式 CLI${colors.reset} ${colors.gray}(v1.0.0)${colors.reset}`);
  log();
  log(`${colors.cyan}交互命令：${colors.reset}`);
  log('  /help             显示此帮助');
  log('  /list             列出所有可用测试');
  log('  /run <name>       运行指定测试（如 /run invoice）');
  log('  /info <name>      查看某个测试的详细信息');
  log('  /quit 或 /exit    退出交互模式');
  log('  /server [local]   切换目标：服务器 http://172.16.16.110/oa 或本地（默认本地）');
  log();
  log(`${colors.cyan}命令行直接运行：${colors.reset}`);
  log('  node run.js <name>     直接跑某个测试（等价 /run <name>）');
  log('  node run.js --help     列出所有测试（非交互）');
  log('  node run.js --version  显示版本');
  log();
  log(`${colors.cyan}环境变量：${colors.reset}`);
  log('  PW_AUTO=1     无头自动连跑（不暂停），适合 CI/自测');
  log('  PW_STEP=1     分步调试：每步截图后暂停，Inspector 点 Resume 继续');
  log();
  log(`${colors.cyan}可用测试（/list 实时刷新）：${colors.reset}`);
  showList();
}

function showList() {
  log();
  log('名称'.padEnd(12) + '文件'.padEnd(20) + '说明');
  log('─'.repeat(80));
  Object.values(registry).forEach(t => {
    const file = path.basename(t.file);
    const exists = require('fs').existsSync(path.join(TESTS_DIR, file));
    const status = exists ? colors.green + '●' : colors.red + '○';
    log(`${status} ${t.name.padEnd(10)}${file.padEnd(22)}${t.desc}`);
  });
  log();
}

function showInfo(name) {
  const t = registry[name];
  if (!t) { err(`未知测试：${name}`); return; }
  log();
  log(`${colors.bright}${t.name}${colors.reset}`);
  log(`  文件：${t.file}`);
  log(`  说明：${t.desc}`);
  log(`  用法：${t.usage}`);
  log(`  示例：${t.examples.join(' | ')}`);
  log();
}

function runTest(name) {
  const t = registry[name];
  if (!t) {
    err(`未知测试：${name}。输入 /list 查看可用测试。`);
    return Promise.resolve(false);
  }
  const filePath = path.join(TESTS_DIR, t.file);
  if (!require('fs').existsSync(filePath)) {
    err(`测试文件不存在：${t.file}（先实现再跑）`);
    return Promise.resolve(false);
  }
  log();
  const target = process.env.OA_BASE || '本地 http://127.0.0.1:8082';
  log(`${colors.bright}▶ 启动测试：${name}${colors.reset}`);
  log(`  ${t.desc}`);
  log(`  目标：${colors.cyan}${target}${colors.reset}`);
  log(`  命令等价：node ${t.file}`);
  log();
  return new Promise((resolve) => {
    const child = require('child_process').fork(filePath, process.argv.slice(3), {
      cwd: TESTS_DIR,
      stdio: 'inherit',
      env: { ...process.env, PW_AUTO: process.env.PW_AUTO || '' }
    });
    child.on('exit', (code) => {
      if (code === 0) ok(`测试 ${name} 完成`); else err(`测试 ${name} 退出码 ${code}`);
      resolve(code === 0);
    });
    child.on('error', (e) => { err(`启动失败：${e.message}`); resolve(false); });
  });
}

// ---------- 交互模式 ----------
function startREPL() {
  log();
  log(`${colors.bright}OA 测试导演模式${colors.reset} ${colors.gray}(输入 /help 查看命令，/quit 退出)${colors.reset}`);
  const rl = readline.createInterface({ input: process.stdin, output: process.stdout, prompt: 'oa> ' });
  rl.prompt();

  rl.on('line', async (line) => {
    const input = line.trim();
    if (!input) { rl.prompt(); return; }

    // 支持 /help 和 help 两种写法
    const cmd = input.startsWith('/') ? input.slice(1) : input;
    const [command, ...args] = cmd.split(/\s+/);

    switch (command.toLowerCase()) {
      case 'help': case '?': showHelp(); break;
      case 'list': case 'ls': showList(); break;
      case 'run':
        if (!args[0]) { warn('用法：/run <name>'); break; }
        await runTest(args[0]); break;
      case 'info':
        if (!args[0]) { warn('用法：/info <name>'); break; }
        showInfo(args[0]); break;
      case 'quit': case 'exit': case 'q':
        log('👋 已退出'); process.exit(0); break;
      case 'server': {
        // /server 或 /server local：切换目标环境（交互会话内 fork 子进程时透传 OA_BASE）
        const mode = args[0] || (process.env.OA_BASE ? 'local' : 'server');
        if (mode === 'local') { delete process.env.OA_BASE; ok('已切换到本地 http://127.0.0.1:8082'); }
        else { process.env.OA_BASE = SERVER_BASE; ok(`已切换到服务器 ${SERVER_BASE}`); }
        break;
      }
      default:
        // 允许直接输名字运行
        if (registry[command]) { await runTest(command); }
        else warn(`未知命令：${command}。输入 /help 查看帮助。`);
    }
    rl.prompt();
  }).on('close', () => { log('\n👋 已退出'); process.exit(0); });
}

// ---------- 入口 ----------
function main() {
  const arg = process.argv[2];

  if (!arg) {
    startREPL();
    return;
  }

  if (arg === '--help' || arg === '-h') { showHelp(); return; }
  if (arg === '--version' || arg === '-v') { log('oa-test-cli v1.0.0'); return; }
  if (arg === 'help') { showHelp(); return; }
  if (arg === 'list') { showList(); return; }

  // 直接运行：node run.js <name>
  runTest(arg).then(ok => process.exit(ok ? 0 : 1));
}

main();
