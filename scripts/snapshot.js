'use strict';

/**
 * Screenshots one external page in headless Chrome, waiting for it to settle.
 *
 * Used for the GitHub pages that appear in the blog (commit history, pull request, release,
 * contributor graph). Those pages render part of their content with JavaScript, so a plain
 * `chrome --screenshot` fires too early; this script waits for a real pause before capturing.
 *
 * Usage:
 *   node scripts/snapshot.js <url> <output.png> [waitMs] [width] [height]
 */

const fs = require('fs');
const path = require('path');
const { launch, sleep } = require('./cdp');

async function main() {
    const [url, output] = process.argv.slice(2);
    if (!url || !output) {
        console.error('usage: node scripts/snapshot.js <url> <output.png> [waitMs] [width] [height]');
        process.exit(1);
    }
    const waitMs = Number(process.argv[4] || 12000);
    const width = Number(process.argv[5] || 1400);
    const height = Number(process.argv[6] || 1000);

    fs.mkdirSync(path.dirname(output), { recursive: true });

    const { devtools, chrome } = await launch({
        chrome: process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
        port: 9444,
        profile: path.join(__dirname, '..', 'docs', '.chrome-profile-snapshot'),
        width,
        height,
        args: process.env.CHROME_PROXY ? ['--proxy-server=' + process.env.CHROME_PROXY] : []
    });

    await devtools.send('Page.enable');
    await devtools.send('Runtime.enable');
    await devtools.send('Emulation.setDeviceMetricsOverride', {
        width, height, deviceScaleFactor: 1, mobile: false
    });
    await devtools.send('Page.navigate', { url });
    await sleep(waitMs);

    const title = await devtools.evaluate('document.title');
    const bytes = await devtools.screenshot(path.basename(output), path.dirname(output));
    console.log('captured', bytes, '-', title);
    chrome.kill();
    process.exit(0);
}

main().catch((error) => {
    console.error('Fatal:', error);
    process.exit(1);
});
