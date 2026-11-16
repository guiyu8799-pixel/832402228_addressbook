'use strict';

/**
 * Drives the address book in a real headless Chrome and captures the screenshots used by the blog.
 *
 * Usage:
 *   node scripts/capture.js          # screenshots only
 *   node scripts/capture.js --video  # also record frames for the demo video
 *
 * Every screenshot is produced after real interaction (clicking the real buttons, typing into the
 * real inputs), so the images show the behaviour of the running application, not a mock-up.
 */

const fs = require('fs');
const path = require('path');
const { launch, sleep, click, fill, waitFor } = require('./cdp');

const APP = process.env.APP_URL || 'http://localhost:8080';
const ROOT = path.join(__dirname, '..');
const OUT = path.join(ROOT, 'docs', 'screenshots');
const DOWNLOADS = path.join(ROOT, 'docs', 'downloads');
const FRAMES = path.join(ROOT, 'docs', 'video-frames');
const DEMO_FILE = path.join(ROOT, 'docs', 'demo-contacts.xlsx');
const RECORD = process.argv.includes('--video');
const WIDTH = 1440;
const HEIGHT = 1000;

const manifest = [];

async function shot(devtools, file, label) {
    await devtools.screenshot(file, OUT);
    manifest.push({file, label});
    console.log('  screenshot ' + file);
}

async function setViewport(devtools, width, height, mobile) {
    await devtools.send('Emulation.setDeviceMetricsOverride', {
        width, height, deviceScaleFactor: 1, mobile: !!mobile
    });
    await sleep(250);
}

async function addMethodRow(devtools, type, label, value) {
    await click(devtools, '[data-add="' + type + '"]');
    await devtools.evaluate(`(function () {
        var rows = document.querySelectorAll('.method-row');
        var row = rows[rows.length - 1];
        var labelInput = row.querySelector('.m-label-input');
        var valueInput = row.querySelector('.m-value');
        labelInput.value = ${JSON.stringify(label)};
        labelInput.dispatchEvent(new Event('input', { bubbles: true }));
        valueInput.value = ${JSON.stringify(value)};
        valueInput.dispatchEvent(new Event('input', { bubbles: true }));
    })()`);
}

async function clickCardAction(devtools, name, action) {
    await devtools.evaluate(`(function () {
        var cards = document.querySelectorAll('.card');
        for (var i = 0; i < cards.length; i++) {
            if (cards[i].textContent.indexOf(${JSON.stringify(name)}) !== -1) {
                var button = cards[i].querySelector('[data-action="' + ${JSON.stringify(action)} + '"]');
                if (!button) { throw new Error('no ' + ${JSON.stringify(action)}); }
                button.click();
                return true;
            }
        }
        throw new Error('card not found: ' + ${JSON.stringify(name)});
    })()`);
}

async function uploadFile(devtools, selector, file) {
    const document = await devtools.send('DOM.getDocument', {depth: -1});
    const node = await devtools.send('DOM.querySelector', {
        nodeId: document.root.nodeId, selector
    });
    if (!node.nodeId) {
        throw new Error('file input not found: ' + selector);
    }
    await devtools.send('DOM.setFileInputFiles', {nodeId: node.nodeId, files: [file]});
    // DOM.setFileInputFiles already fires input/change in current Chromium; dispatching again here
    // would import the same file twice.
    await sleep(500);
}

async function main() {
    fs.mkdirSync(OUT, {recursive: true});
    fs.mkdirSync(DOWNLOADS, {recursive: true});
    if (RECORD) {
        fs.rmSync(FRAMES, {recursive: true, force: true});
        fs.mkdirSync(FRAMES, {recursive: true});
    }

    const {devtools, chrome} = await launch({
        chrome: process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
        port: 9333,
        profile: path.join(ROOT, 'docs', '.chrome-profile'),
        width: WIDTH,
        height: HEIGHT
    });

    let frameIndex = 0;
    const frameTimes = [];
    if (RECORD) {
        devtools.on('Page.screencastFrame', async (params) => {
            const file = 'frame-' + String(frameIndex++).padStart(5, '0') + '.jpg';
            fs.writeFileSync(path.join(FRAMES, file), Buffer.from(params.data, 'base64'));
            frameTimes.push({file, time: Date.now()});
            try {
                await devtools.send('Page.screencastFrameAck', {sessionId: params.sessionId});
            } catch (ignored) {
                // the session may already be closed at the very end of the recording
            }
        });
    }

    await devtools.send('Page.enable');
    await devtools.send('Runtime.enable');
    await devtools.send('DOM.enable');
    await devtools.send('Page.setDownloadBehavior', {
        behavior: 'allow', downloadPath: DOWNLOADS
    });
    await setViewport(devtools, WIDTH, HEIGHT, false);

    console.log('Opening ' + APP);
    await devtools.send('Page.navigate', {url: APP});
    await sleep(2500);

    if (RECORD) {
        await devtools.send('Page.startScreencast', {
            format: 'jpeg', quality: 82, maxWidth: 1280, maxHeight: 890, everyNthFrame: 1
        });
    }

    /* 1. empty state */
    await waitFor(devtools, '#emptyState:not(.is-hidden)');
    await sleep(700);
    await shot(devtools, '01-empty-state.png',
        '空状态：首次打开时通讯录为空，提示「新增联系人」或「下载模板」');

    /* 2. create dialog filled with four methods */
    await click(devtools, '[data-testid="add"]');
    await waitFor(devtools, '#modal:not(.is-hidden)');
    await fill(devtools, '[data-testid="field-name"]', '苏晴');
    await fill(devtools, '[data-testid="field-group"]', '同学');
    await fill(devtools, '[data-testid="field-company"]', '福州大学');
    await fill(devtools, '[data-testid="field-title"]', '计算机学院 本科生');
    await addMethodRow(devtools, 'PHONE', '手机', '13800138001');
    await addMethodRow(devtools, 'PHONE', '宿舍', '0591-22861234');
    await addMethodRow(devtools, 'EMAIL', '邮箱', 'suqing@example.com');
    await addMethodRow(devtools, 'SOCIAL', '微信', 'suqing_fzu');
    await addMethodRow(devtools, 'ADDRESS', '宿舍地址', '福州市闽侯县学园路 2 号');
    await fill(devtools, '[data-testid="field-notes"]', '同班同学，算法课队友');
    await sleep(500);
    await shot(devtools, '02-add-dialog.png',
        '新增联系人：一个联系人可添加多条联系方式（2 个电话 + 邮箱 + 微信 + 地址）');

    /* 3. saved card */
    await click(devtools, '[data-testid="save"]');
    await waitFor(devtools, '.card');
    await sleep(1200);
    await shot(devtools, '03-created-contact.png',
        '保存后列表出现联系人：联系方式按类型展示，数据已写入后端 SQLite');

    /* 4. bookmark */
    await devtools.evaluate(`(function () {
        document.querySelector('.card .star-btn').click();
    })()`);
    await sleep(1600);
    await shot(devtools, '04-bookmarked.png',
        '收藏联系人：点击星标后卡片高亮，收藏计数变为 1，卡片排到最前面');

    /* 5. favourites filter */
    await click(devtools, '[data-testid="filter-bookmarked"]');
    await sleep(1400);
    await shot(devtools, '05-favorites-filter.png',
        '收藏筛选：侧边栏「收藏」只显示已星标的联系人');
    await click(devtools, '[data-testid="filter-all"]');
    await sleep(900);

    /* 6. import the demo workbook */
    await setViewport(devtools, WIDTH, 1560, false);
    await uploadFile(devtools, '[data-testid="import-input"]', DEMO_FILE);
    await sleep(3200);
    await shot(devtools, '06-import-result.png',
        'Excel 导入：导入 demo-contacts.xlsx 中的 10 位联系人，列表变为 11 位');
    await setViewport(devtools, WIDTH, HEIGHT, false);
    await sleep(600);

    /* 7. export */
    await click(devtools, '[data-testid="export"]');
    await sleep(900);
    await shot(devtools, '07-export-toast.png',
        'Excel 导出：点击「导出 Excel」后后端生成 xlsx 并开始下载');
    await sleep(1500);

    /* 8. search by phone number */
    await fill(devtools, '[data-testid="search"]', '13800138000');
    await sleep(900);
    await shot(devtools, '08-search-by-phone.png',
        '搜索：输入手机号也能命中联系人（关键词会匹配所有联系方式）');
    await fill(devtools, '[data-testid="search"]', '');
    await sleep(900);

    /* 9. edit dialog with existing methods */
    await clickCardAction(devtools, '陈默', 'edit');
    await waitFor(devtools, '#modal:not(.is-hidden)');
    await sleep(600);
    await shot(devtools, '09-edit-dialog.png',
        '编辑联系人：弹窗回填该联系人的全部联系方式，可增删改任意一条');
    await click(devtools, '#cancelBtn');
    await sleep(600);

    /* 10. delete confirmation */
    await clickCardAction(devtools, '罗宾', 'delete');
    await waitFor(devtools, '#confirmModal:not(.is-hidden)');
    await sleep(500);
    await shot(devtools, '10-delete-confirm.png',
        '删除联系人：二次确认弹窗，确认后调用 DELETE 接口并刷新列表');
    await click(devtools, '[data-testid="confirm-delete"]');
    await sleep(1800);

    /* 11. table view */
    await click(devtools, '[data-testid="view-table"]');
    await sleep(900);
    await shot(devtools, '11-table-view.png',
        '表格视图：同一份数据可切换为表格，适合快速浏览和对比');
    await click(devtools, '[data-testid="view-cards"]');
    await sleep(600);

    /* 12. persistence after a full reload */
    await devtools.send('Page.reload', {ignoreCache: true});
    await sleep(3000);
    await shot(devtools, '12-persistence-after-reload.png',
        '刷新页面后数据仍在：联系人来自后端 SQLite，而不是前端缓存');

    /* 13. mobile layout */
    await setViewport(devtools, 430, 900, true);
    await devtools.send('Page.reload', {ignoreCache: true});
    await sleep(2500);
    await shot(devtools, '13-mobile-layout.png',
        '移动端布局：窄屏下侧边栏折叠为横向筛选，卡片单列显示');
    await setViewport(devtools, WIDTH, HEIGHT, false);

    fs.writeFileSync(path.join(OUT, 'manifest.json'), JSON.stringify(manifest, null, 2), 'utf8');

    if (RECORD) {
        await devtools.send('Page.stopScreencast');
        await sleep(400);
        const base = frameTimes.length ? frameTimes[0].time : Date.now();
        const lines = [];
        for (let index = 0; index < frameTimes.length; index++) {
            const current = frameTimes[index];
            const next = frameTimes[index + 1];
            const duration = next ? Math.min(1.2, (next.time - current.time) / 1000) : 0.6;
            lines.push("file '" + current.file + "'");
            lines.push('duration ' + duration.toFixed(3));
        }
        fs.writeFileSync(path.join(FRAMES, 'frames.txt'), lines.join('\n'), 'utf8');
        console.log('Recorded ' + frameTimes.length + ' frames at ' + base);
    }

    console.log('Captured ' + manifest.length + ' screenshots into ' + OUT);
    chrome.kill();
    process.exit(0);
}

main().catch((error) => {
    console.error('Fatal:', error);
    process.exit(1);
});
