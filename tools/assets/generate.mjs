import crypto from 'node:crypto';
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
    const fw = 16;
    const fh = 16;
    const totalH = fh * frames;
    const rgba = Buffer.alloc(fw * totalH * 4, 0);

    for (let f = 0; f < frames; f++) {
        const phase = (f / frames) * Math.PI * 2;
        const yBase = f * fh;
        for (let y = 1; y < fh - 1; y++) {
            const v = 1 - y / (fh - 1);
            const centerShift = Math.sin(phase + v * 4.2) * (1.8 + (1 - v) * 0.9);
            const cx = 7.5 + centerShift;
            const halfWidth = 0.85 + Math.sin(v * Math.PI) * 2.15;
            const verticalEnv = Math.sin(v * Math.PI);

            for (let x = 0; x < fw; x++) {
                const dist = Math.abs(x - cx);
                if (dist > halfWidth + 0.9) continue;
                const radial = Math.max(0, 1 - dist / (halfWidth + 0.75));
                const quantized = Math.floor(radial * 4) / 4;
                const alpha = Math.min(1, quantized * verticalEnv * 1.15);
                if (alpha < 0.14) continue;
                const coreBoost = dist < 0.75 && v > 0.22 && v < 0.78 ? 255 : 238;
                setPixel(rgba, fw, totalH, x, yBase + y, coreBoost, coreBoost, 255, alpha * 245);
            }
        }
    }
    return encodePng(fw, totalH, rgba);
}

function generateBodyVeins(direction) {
    const w = 32;
    const h = 32;
    const rgba = Buffer.alloc(w * h * 4, 0);

    for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
            const u = (x + direction * y * 0.75 + 64) % 16;
            const wave = Math.sin((y / h) * Math.PI * 2 * 2 + (direction > 0 ? 0.5 : 2.1)) * 2.1;
            const bandDist = Math.abs((u - 8) + wave);
            if (bandDist < 1.15) {
                const intensity = bandDist < 0.45 ? 215 : 125;
                setPixel(rgba, w, h, x, y, 250, 252, 255, intensity);
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
            if (dx <= 0.6 && dy <= 0.6) {
                alpha = 255;
            } else if ((dx <= 0.6 && dy <= 4.6) || (dy <= 0.6 && dx <= 4.6)) {
                const arm = 1 - Math.max(dx, dy) / 5.2;
                alpha = Math.round(arm * 235);
            } else if (dx <= 1.6 && dy <= 1.6) {
                alpha = 150;
            }
            if (alpha > 20) {
                setPixel(rgba, w, h, x, y, 255, 255, 255, alpha);
            }
        }
    }
    return encodePng(w, h, rgba);
}

function generateGroundHalo() {
    const w = 32;
    const h = 32;
    const rgba = Buffer.alloc(w * h * 4, 0);
    const cx = 15.5;
    const cy = 15.5;

    for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
            const dx = (x - cx) / 15.5;
            const dy = (y - cy) / 15.5;
            const r = Math.sqrt(dx * dx + dy * dy);
            if (r > 1.0) continue;
            const ring = Math.exp(-Math.pow((r - 0.58) / 0.24, 2));
            const center = Math.max(0, 1 - r * 1.15) * 0.45;
            const stepped = Math.floor((ring * 0.75 + center) * 6) / 6;
            if (stepped > 0.08) {
                setPixel(rgba, w, h, x, y, 255, 255, 255, stepped * 190);
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
    ['textures/entity/body_veins_a.png', generateBodyVeins(1)],
    ['textures/entity/body_veins_b.png', generateBodyVeins(-1)],
    ['textures/entity/star_mote.png', generateStarMote()],
    ['textures/entity/ground_halo.png', generateGroundHalo()],
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
