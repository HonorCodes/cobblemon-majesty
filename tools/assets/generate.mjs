import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import zlib from 'node:zlib';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '../..');
const assetsRoot = path.join(repoRoot, 'src/main/resources/assets/cobblemon_majesty');

const crcTable = new Uint32Array(256);
for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) {
        c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1);
    }
    crcTable[n] = c >>> 0;
}

function crc32(buf) {
    let c = 0xffffffff;
    for (let i = 0; i < buf.length; i++) {
        c = crcTable[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
    }
    return (c ^ 0xffffffff) >>> 0;
}

function pngChunk(type, data) {
    const typeBuf = Buffer.from(type, 'ascii');
    const lenBuf = Buffer.alloc(4);
    lenBuf.writeUInt32BE(data.length, 0);
    const crcInput = Buffer.concat([typeBuf, data]);
    const crcBuf = Buffer.alloc(4);
    crcBuf.writeUInt32BE(crc32(crcInput), 0);
    return Buffer.concat([lenBuf, typeBuf, data, crcBuf]);
}

function encodePng(width, height, rgba) {
    const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(width, 0);
    ihdr.writeUInt32BE(height, 4);
    ihdr[8] = 8;
    ihdr[9] = 6;
    ihdr[10] = 0;
    ihdr[11] = 0;
    ihdr[12] = 0;

    const stride = width * 4 + 1;
    const raw = Buffer.alloc(stride * height);
    for (let y = 0; y < height; y++) {
        raw[y * stride] = 0;
        rgba.copy(raw, y * stride + 1, y * width * 4, (y + 1) * width * 4);
    }
    const compressed = zlib.deflateSync(raw, { level: 9 });
    return Buffer.concat([
        signature,
        pngChunk('IHDR', ihdr),
        pngChunk('IDAT', compressed),
        pngChunk('IEND', Buffer.alloc(0))
    ]);
}

function setPixel(buf, width, height, x, y, r, g, b, a) {
    if (x < 0 || y < 0 || x >= width || y >= height) return;
    const idx = (y * width + x) * 4;
    buf[idx] = Math.max(0, Math.min(255, Math.round(r)));
    buf[idx + 1] = Math.max(0, Math.min(255, Math.round(g)));
    buf[idx + 2] = Math.max(0, Math.min(255, Math.round(b)));
    buf[idx + 3] = Math.max(0, Math.min(255, Math.round(a)));
}

function generateAuraWispStrip() {
    const frames = 8;
    const fw = 32;
    const fh = 32;
    const totalH = fh * frames;
    const rgba = Buffer.alloc(fw * totalH * 4, 0);

    for (let f = 0; f < frames; f++) {
        const phase = (f / frames) * Math.PI * 2;
        const yBase = f * fh;
        for (let y = 1; y < fh - 1; y++) {
            const v = 1 - y / (fh - 1);
            const env = Math.sin(v * Math.PI);
            if (env <= 0.01) continue;
            const halfWidth = 4.6 * Math.pow(env, 0.85);
            const spineX = 15.5 + Math.sin(phase + v * 3.6) * (2.2 * env);

            for (let x = 0; x < fw; x++) {
                const dist = Math.abs(x - spineX);
                const norm = dist / (halfWidth + 0.95);
                if (norm >= 1.0) continue;
                const radial = Math.pow(1.0 - norm * norm, 1.65);
                const alpha = Math.min(1.0, env * radial * 1.08);
                if (alpha < 0.012) continue;
                const coreMix = Math.max(0.0, 1.0 - dist / Math.max(0.9, halfWidth * 0.55)) * env;
                const brightness = 232 + 23 * Math.min(1.0, coreMix);
                setPixel(rgba, fw, totalH, x, yBase + y, brightness, brightness, 255, alpha * 245);
            }
        }
    }
    return encodePng(fw, totalH, rgba);
}

function generateCometEmber() {
    const w = 16;
    const h = 32;
    const rgba = Buffer.alloc(w * h * 4, 0);
    const hx = 7.5;
    const hy = 7.5;

    for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
            const dx = x - hx;
            if (y <= hy) {
                const dy = (y - hy) / 3.8;
                const r = Math.sqrt(Math.pow(dx / 3.6, 2) + dy * dy);
                if (r < 1.0) {
                    const fall = Math.pow(1.0 - r * r, 1.45);
                    const core = 235 + 20 * Math.max(0.0, 1.0 - r * 1.25);
                    setPixel(rgba, w, h, x, y, core, core, 255, fall * 252);
                }
            } else {
                const t = (y - hy) / 22.0;
                if (t >= 1.0) continue;
                const halfTail = 3.2 * Math.pow(1.0 - t, 1.15);
                const norm = Math.abs(dx) / Math.max(0.55, halfTail + 0.55);
                if (norm < 1.0) {
                    const fall = Math.pow(1.0 - norm * norm, 1.55) * Math.pow(1.0 - t, 1.3);
                    if (fall < 0.01) continue;
                    const core = 228 + 27 * Math.max(0.0, (1.0 - t * 1.5) * (1.0 - norm));
                    setPixel(rgba, w, h, x, y, core, core, 255, fall * 242);
                }
            }
        }
    }
    return encodePng(w, h, rgba);
}

function generateFilamentStrip() {
    const w = 16;
    const h = 16;
    const rgba = Buffer.alloc(w * h * 4, 0);
    const cx = 7.5;

    for (let x = 0; x < w; x++) {
        const norm = Math.abs(x - cx) / 7.5;
        if (norm >= 1.0) continue;
        const falloff = Math.pow(1.0 - norm * norm, 1.85);
        const core = 236 + 19 * Math.max(0.0, 1.0 - norm * 1.8);
        const alpha = falloff * 252;
        if (alpha < 4) continue;
        for (let y = 0; y < h; y++) {
            setPixel(rgba, w, h, x, y, core, core, 255, alpha);
        }
    }
    return encodePng(w, h, rgba);
}

function generateSparkDot() {
    const w = 16;
    const h = 16;
    const rgba = Buffer.alloc(w * h * 4, 0);
    const c = 7.5;

    for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
            const dx = (x - c) / 7.2;
            const dy = (y - c) / 7.2;
            const r = Math.sqrt(dx * dx + dy * dy);
            if (r >= 1.0) continue;
            const glow = Math.pow(1.0 - r, 2.2);
            const coreBoost = r < 0.28 ? 1.0 : glow;
            const alpha = Math.min(255, Math.round(coreBoost * 252));
            if (alpha > 4) {
                const rgb = r < 0.32 ? 255 : 240;
                setPixel(rgba, w, h, x, y, rgb, rgb, 255, alpha);
            }
        }
    }
    return encodePng(w, h, rgba);
}

function generateStarMote() {
    const w = 16;
    const h = 16;
    const rgba = Buffer.alloc(w * h * 4, 0);
    const c = 7.5;

    for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
            const dx = Math.abs(x - c);
            const dy = Math.abs(y - c);
            let alpha = 0;
            if (dx <= 0.65 && dy <= 0.65) {
                alpha = 255;
            } else if ((dx <= 0.7 && dy <= 5.2) || (dy <= 0.7 && dx <= 5.2)) {
                const arm = Math.pow(Math.max(0, 1 - Math.max(dx, dy) / 5.5), 1.35);
                alpha = Math.round(arm * 240);
            } else {
                const r = Math.sqrt(dx * dx + dy * dy);
                if (r < 2.6) {
                    alpha = Math.round(Math.pow(1 - r / 2.6, 1.8) * 165);
                }
            }
            if (alpha > 8) {
                setPixel(rgba, w, h, x, y, 255, 255, 255, alpha);
            }
        }
    }
    return encodePng(w, h, rgba);
}

function generateAreaGlow() {
    const w = 64;
    const h = 64;
    const rgba = Buffer.alloc(w * h * 4, 0);
    const cx = 31.5;
    const cy = 31.5;

    for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
            const dx = (x - cx) / 31.5;
            const dy = (y - cy) / 31.5;
            const r = Math.sqrt(dx * dx + dy * dy);
            if (r >= 1.0) continue;
            const edge = Math.pow(1.0 - r * r, 1.85);
            const gaussian = Math.exp(-Math.pow(r / 0.62, 2));
            const intensity = Math.min(1.0, gaussian * edge);
            const byteVal = Math.round(intensity * 255);
            if (byteVal > 0) {
                setPixel(rgba, w, h, x, y, byteVal, byteVal, byteVal, byteVal);
            }
        }
    }
    return encodePng(w, h, rgba);
}

function generateMajesticBadgeIcon() {
    const w = 16;
    const h = 16;
    const rgba = Buffer.alloc(w * h * 4, 0);
    const pattern = [
        '................',
        '.......GG.......',
        '.......WW.......',
        '......GWWG......',
        '......GWWG......',
        '.....GWCCWG.....',
        '.GGGGWWCCWWGGGG.',
        '.GWWWWCCCCWWWWG.',
        '.GWWWWCCCCWWWWG.',
        '.GGGGWWCCWWGGGG.',
        '.....GWCCWG.....',
        '......GWWG...G..',
        '......GWWG..GWG.',
        '.......WW....G..',
        '.......GG.......',
        '................'
    ];
    const palette = {
        G: [217, 119, 6, 255],
        W: [251, 191, 36, 255],
        C: [255, 251, 235, 255]
    };
    for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
            const ch = pattern[y][x];
            if (palette[ch]) {
                const [r, g, b, a] = palette[ch];
                setPixel(rgba, w, h, x, y, r, g, b, a);
            }
        }
    }
    return encodePng(w, h, rgba);
}

const targets = [
    ['textures/entity/aura_wisp.png', generateAuraWispStrip()],
    ['textures/entity/comet_ember.png', generateCometEmber()],
    ['textures/entity/filament_strip.png', generateFilamentStrip()],
    ['textures/entity/spark_dot.png', generateSparkDot()],
    ['textures/entity/star_mote.png', generateStarMote()],
    ['textures/entity/area_glow.png', generateAreaGlow()],
    ['textures/gui/icon_majestic.png', generateMajesticBadgeIcon()]
];

const checkOnly = process.argv.includes('--check');
let mismatches = 0;

for (const [relPath, bytes] of targets) {
    const fullPath = path.join(assetsRoot, relPath);
    if (checkOnly) {
        if (!fs.existsSync(fullPath)) {
            console.error(`Missing generated asset: ${relPath}`);
            mismatches++;
            continue;
        }
        const existing = fs.readFileSync(fullPath);
        if (!existing.equals(bytes)) {
            console.error(`Out-of-date generated asset: ${relPath}`);
            mismatches++;
        }
    } else {
        fs.mkdirSync(path.dirname(fullPath), { recursive: true });
        fs.writeFileSync(fullPath, bytes);
    }
}

const soundPath = path.join(assetsRoot, 'sounds/majestic_presence.ogg');
if (checkOnly) {
    if (!fs.existsSync(soundPath) || fs.statSync(soundPath).size < 256) {
        console.error('Missing or empty sound asset: sounds/majestic_presence.ogg');
        mismatches++;
    }
    if (mismatches > 0) {
        process.exit(1);
    }
    console.log(`Verified ${targets.length} generated textures and 1 sound asset.`);
} else {
    console.log(`Generated ${targets.length} textures into ${assetsRoot}.`);
}
