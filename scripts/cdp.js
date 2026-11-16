'use strict';

/**
 * Minimal Chrome DevTools Protocol client.
 *
 * Talks to Chrome over a raw WebSocket so that no npm package has to be installed: the automation
 * in this repository only needs Node.js itself. Screenshots and video frames are produced by really
 * driving the page (clicking buttons, typing into inputs), never by mocking the UI.
 */

const { spawn } = require('child_process');
const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function httpGetJson(url) {
    return new Promise((resolve, reject) => {
        http.get(url, (response) => {
            let body = '';
            response.setEncoding('utf8');
            response.on('data', (chunk) => { body += chunk; });
            response.on('end', () => {
                try {
                    resolve(JSON.parse(body));
                } catch (error) {
                    reject(error);
                }
            });
        }).on('error', reject);
    });
}

class DevTools {
    constructor(wsUrl) {
        this.wsUrl = wsUrl;
        this.nextId = 1;
        this.pending = new Map();
        this.handlers = new Map();
        this.socket = null;
    }

    on(method, handler) {
        this.handlers.set(method, handler);
    }

    connect() {
        return new Promise((resolve, reject) => {
            const parsed = new URL(this.wsUrl);
            const key = crypto.randomBytes(16).toString('base64');
            const request = http.request({
                hostname: parsed.hostname,
                port: parsed.port,
                path: parsed.pathname + parsed.search,
                headers: {
                    Connection: 'Upgrade',
                    Upgrade: 'websocket',
                    'Sec-WebSocket-Key': key,
                    'Sec-WebSocket-Version': '13'
                }
            });
            request.on('upgrade', (response, socket) => {
                this.socket = socket;
                this.buffer = Buffer.alloc(0);
                socket.on('data', (chunk) => this.onData(chunk));
                socket.on('error', reject);
                resolve();
            });
            request.on('error', reject);
            request.end();
        });
    }

    onData(chunk) {
        this.buffer = Buffer.concat([this.buffer, chunk]);
        for (;;) {
            const frame = this.readFrame();
            if (!frame) {
                return;
            }
            if (frame.opcode === 0x1) {
                const message = JSON.parse(frame.payload.toString('utf8'));
                if (message.id !== undefined) {
                    const entry = this.pending.get(message.id);
                    if (entry) {
                        this.pending.delete(message.id);
                        if (message.error) {
                            entry.reject(new Error(JSON.stringify(message.error)));
                        } else {
                            entry.resolve(message.result);
                        }
                    }
                } else if (message.method) {
                    const handler = this.handlers.get(message.method);
                    if (handler) {
                        handler(message.params);
                    }
                }
            } else if (frame.opcode === 0x8) {
                this.socket.end();
                return;
            }
        }
    }

    readFrame() {
        const buffer = this.buffer;
        if (buffer.length < 2) {
            return null;
        }
        const opcode = buffer[0] & 0x0f;
        let length = buffer[1] & 0x7f;
        let offset = 2;
        if (length === 126) {
            if (buffer.length < 4) {
                return null;
            }
            length = buffer.readUInt16BE(2);
            offset = 4;
        } else if (length === 127) {
            if (buffer.length < 10) {
                return null;
            }
            length = Number(buffer.readBigUInt64BE(2));
            offset = 10;
        }
        if (buffer.length < offset + length) {
            return null;
        }
        const payload = buffer.subarray(offset, offset + length);
        this.buffer = buffer.subarray(offset + length);
        return { opcode, payload };
    }

    send(method, params) {
        const id = this.nextId++;
        const payload = Buffer.from(JSON.stringify({ id, method, params: params || {} }), 'utf8');
        const mask = crypto.randomBytes(4);
        const masked = Buffer.alloc(payload.length);
        for (let i = 0; i < payload.length; i++) {
            masked[i] = payload[i] ^ mask[i % 4];
        }

        let header;
        if (payload.length < 126) {
            header = Buffer.alloc(6);
            header[0] = 0x81;
            header[1] = 0x80 | payload.length;
            mask.copy(header, 2);
        } else if (payload.length < 65536) {
            header = Buffer.alloc(8);
            header[0] = 0x81;
            header[1] = 0x80 | 126;
            header.writeUInt16BE(payload.length, 2);
            mask.copy(header, 4);
        } else {
            header = Buffer.alloc(14);
            header[0] = 0x81;
            header[1] = 0x80 | 127;
            header.writeBigUInt64BE(BigInt(payload.length), 2);
            mask.copy(header, 10);
        }

        return new Promise((resolve, reject) => {
            this.pending.set(id, { resolve, reject });
            this.socket.write(Buffer.concat([header, masked]));
        });
    }

    async evaluate(expression) {
        const result = await this.send('Runtime.evaluate', {
            expression,
            returnByValue: true,
            awaitPromise: true
        });
        if (result.exceptionDetails) {
            throw new Error('Page exception: ' + JSON.stringify(result.exceptionDetails));
        }
        return result.result.value;
    }

    async screenshot(fileName, outDir) {
        const result = await this.send('Page.captureScreenshot', { format: 'png' });
        fs.mkdirSync(outDir, { recursive: true });
        fs.writeFileSync(path.join(outDir, fileName), Buffer.from(result.data, 'base64'));
        return fileName;
    }
}

async function waitForChrome(port) {
    for (let attempt = 0; attempt < 80; attempt++) {
        try {
            return await httpGetJson('http://127.0.0.1:' + port + '/json/version');
        } catch (error) {
            await sleep(500);
        }
    }
    throw new Error('Chrome did not expose its debugging port');
}

/**
 * Launches a throwaway headless Chrome and returns a connected client.
 *
 * @param {object} options chrome executable, debugging port, window size and profile directory
 * @returns {Promise<{devtools: DevTools, chrome: import('child_process').ChildProcess}>}
 */
async function launch(options) {
    const extra = options.args || [];
    const chrome = spawn(options.chrome, [
        '--headless=new',
        '--no-sandbox',
        '--disable-gpu',
        '--hide-scrollbars',
        '--no-first-run',
        '--no-default-browser-check',
        '--disable-extensions',
        '--mute-audio',
        '--remote-debugging-port=' + options.port,
        '--user-data-dir=' + options.profile,
        '--window-size=' + options.width + ',' + options.height,
        ...extra,
        'about:blank'
    ], { stdio: 'ignore' });

    await waitForChrome(options.port);
    const targets = await httpGetJson('http://127.0.0.1:' + options.port + '/json/list');
    const target = targets.find((item) => item.type === 'page');
    if (!target) {
        chrome.kill();
        throw new Error('no page target available');
    }
    const devtools = new DevTools(target.webSocketDebuggerUrl);
    await devtools.connect();
    return { devtools, chrome };
}

/** Clicks an element by CSS selector, failing loudly when it is missing. */
async function click(devtools, selector) {
    await devtools.evaluate(`(function () {
        var element = document.querySelector(${JSON.stringify(selector)});
        if (!element) { throw new Error('missing element: ' + ${JSON.stringify(selector)}); }
        element.click();
    })()`);
}

/** Sets an input/textarea value and notifies the page, as a real keystroke would. */
async function fill(devtools, selector, value) {
    await devtools.evaluate(`(function () {
        var element = document.querySelector(${JSON.stringify(selector)});
        if (!element) { throw new Error('missing field: ' + ${JSON.stringify(selector)}); }
        element.value = ${JSON.stringify(value)};
        element.dispatchEvent(new Event('input', { bubbles: true }));
        element.dispatchEvent(new Event('change', { bubbles: true }));
    })()`);
}

/** Waits until a selector exists (or a timeout expires). */
async function waitFor(devtools, selector, timeoutMs) {
    const deadline = Date.now() + (timeoutMs || 8000);
    while (Date.now() < deadline) {
        const found = await devtools.evaluate(
            `!!document.querySelector(${JSON.stringify(selector)})`);
        if (found) {
            return true;
        }
        await sleep(120);
    }
    throw new Error('timeout waiting for ' + selector);
}

module.exports = { DevTools, launch, sleep, click, fill, waitFor, httpGetJson };
