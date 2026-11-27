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

function generateMajestyOrbItem() {
    const w = 16;
    const h = 16;
    const rgba = Buffer.alloc(w * h * 4, 0);
    const grid = [
        '................',
        '......OOOO......',
        '....OOIIIIOO....',
        '...OIIWWWWPAO...',
        '..OIWWHHWWPAAO..',
        '..OIWWHWWPPAAO..',
        '.OIWWPPGGAAQQLD.',
        '.OIPPPGGGGQQLLD.',
        '.OPPLLGGQQQLLSD.',
        '.OAPLLLQQQLLSSD.',
        '..OALLLLQQSSBD..',
        '..ODLLLLSSBBDD..',
        '...ODSSSSBBDD...',
        '....DDSSBBDD....',
        '......DDDD......',
        '................'
    ];
    const palette = {
        O: [104, 110, 156, 255],
        D: [58, 48, 96, 255],
        I: [224, 244, 255, 255],
        W: [247, 250, 255, 255],
        H: [255, 255, 255, 255],
        P: [252, 214, 236, 255],
        G: [255, 241, 204, 255],
        A: [194, 242, 238, 255],
        Q: [184, 224, 248, 255],
        L: [198, 188, 244, 255],
        S: [144, 132, 194, 255],
        B: [196, 232, 250, 255]
    };
    for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
            const ch = grid[y][x];
            if (palette[ch]) {
                const [r, g, b, a] = palette[ch];
                setPixel(rgba, w, h, x, y, r, g, b, a);
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

function generateModIcon() {
    const w = 128;
    const h = 128;
    const rgba = Buffer.alloc(w * h * 4, 0);
    const palette = [
        [10, 5, 20, 255],
        [5, 12, 34, 255],
        [0, 110, 248, 255],
        [8, 244, 250, 255],
        [250, 6, 44, 255],
        [204, 196, 216, 255],
        [11, 18, 44, 255],
        [13, 32, 76, 255],
        [8, 66, 158, 255],
        [0, 176, 255, 255],
        [172, 252, 255, 255],
        [255, 255, 255, 255]
    ];
    const packedB64 = [
        'eNrVmb1uIzcQgAnKZ-mcaqMX4HEBJ1ClJYEUV6XQA7hxL9wBm0OqwxUWkEpwcyrvEQwLSILNG_jpwp8Z',
        'ckgud60EKbKFsfLuxxkO549cxv7jS0ox_9Jut_v5VTwXl_KtlrP8BG54MgCXcuSVqxp_LPhWmb_LY8GP',
        '4ivHd2stojKWZ48iFz-qOndI16zRhBJ4M454BS-RV8jrtefZQk3jDlDANyCV6w559n5LjF_yW5QhPS-c',
        '8fW7wG_OhC8t96flhVPZ8Gu3BLzVjefNAz5s62vP37-Y93TgvQWlboDfEgVG197K9zaTFvcWDLzTDhUY',
        'X_ttyjdGF6u-4712bDPFm9efFlvCS2HFE_k4gZrrb1JeyTbwbv44AcJzakL_lAPfKLeQ0f5hAoS_ofgZ',
        'XaaBCejIUxeIPP_rOXtGeeXuNOH5uSo_eAfXHjcu1DU5714anX_wTsKDGaiJt6X9vybiS96b7nONl8c4',
        's4SHC_hDhYdEsZnjVz4JiIzHrLAQke8orif5BWYlwks6gIljETNjzvMzYiS7ZbwgmTnn46xVkv-p_k6-',
        'KSKnICbwm7Bo3r815lzCt27-JrB4EIN8XHPgJRqgsL-Rz8NryEftaZHjsk15GV_hlKfiV7TepQ5g8oBA',
        'FTnNHyj-JeXbdP1N_EIFlSkfxNthloRPcWNB7adg4BPlz6Rw0MXL-bX2IWwm_5nwwXrfUj7HYwrK-G2l',
        'eZjg2ShvTC5Dl8HLCSDfCsprMc5LXWgA7cgi4VXotky_EjsWXk4AQ3iKV1O8rPPG26y-a1m3X-hFMNd4',
        'XoA03WC1Z7H5iEtPpsYSXk7w-hU8J4tFaoQzpm664Pukfyx4CDVNaoRbS_QBWvtyfkV5cTm_hE4r5tgX',
        'MsjreC8em02SjqSa5Yn6Mg9k2SKfFM9zykOiDIv3reR1UXwp3yb-xWkOQweQrOwQkJcpvyIVLPgu2cqE',
        'hIU8SFmrbIdCQog-K_muWP0iheqowKbC53jKxxUICWuOZzQFxcQwXMB3ee6C6b8QPvSZ-fRx9VMXctM_',
        'z_AulaYJFBTw6m8nee6ymR7nz8FFc16S2C3whE_qL_IhyGwyK6sX8G76q3FejjReBb8NZbrGt7M8G-Mx',
        'SMbFQwjTZgV5fH9tnNRudsZwHDz2x5FvKS-rPKvwpFWwRWKs8M_wOmlSmkv5xM9rl67zvKJyU3X_Iv92',
        's7jKswfN_-2_4xlpVSsjheAkzTLhQ6zrOZ6X_IJkunl-2Ob829Csr6uGjMnFGoCn9ncZxzcchKc_SG2w',
        'BrhJ-FVsGKp8LB_GALDdTvefPmUmHhN_6bgvsga4Kc4fNuWeU5H9E-GtAXjO45kD4VtZ8HK8_sX_Ed7k',
        'G-gLusBX6m_0SsorVuPz_iH-i-yZjcN7-32P_KLWv5AzD6lIw9AS3qyfFpX-iQRVWDLrMD4qPW_2HIvc',
        'AIGPPo28y9aEzwwoMv6cd0yWh_ZDFzxuIFLenllxCEMnHpz5Ah4TodZ4aGfdUHdzPDa0kZeX8fA0bgNE',
        '-GV-zPPwj7ajvSLwPij_OS9cGGl6Fq8LXkU-9NqeV8KFQdKcqiJ-SOEPrXbCN9O8jI2ETniNcUAb6JLn',
        'Vd7Mm-d8Gf_uQMXzKuUFK3hZ8u5USna00_exrNGRwrDfPYkR_ivyraTnb-7YCxYAjzrVGL_E5Yt9tg7H',
        'ZvZB6C4XnI3wuPzrjJdomNB_v5WTvMp4XBgzMOygjxXen_fXeHPr-VXKx28I_i10VB_LlIcTgBOb5iFS',
        'PN9K2uE6Pn4B2vkvKDAA5n485WqjNpEXbPmZ8g6HYzyddGpQ_CCTdaEMLkn_x954-bsRHlKZO7YM_VmS',
        'RSzPRvi1pE2xM0eVv_dfkSZ4Fw91_s4M8QpeZ4fAkb9jd8jrKs99c5XtsDMeOjCd2M_zzxKuUf5q55pI',
        '5O36L2IlVbf7_R5wc7so58-c_Ov-C7xl7u0P0Eb98ND3vxz81efyDX1_d2V_7QO_3_eHwxdQR8mD4XsL',
        'D8Pw8aM8HRPeXszL9yIePh39LYxm_nNtHv46DIe-T7-9GtmRZ6zvfxuGfm_vDG7uzfXH4fDpZKYyPH_4',
        'sN-z5PzUXm8AXz22ehh-v731ulgNHvpbY7Hrn34knzfKK4rH781PJsYf2d5K88Y-TX2_vr-_Y__P629d',
        's0PG'
    ].join('');
    const packed = zlib.inflateSync(Buffer.from(packedB64, 'base64url'));
    for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x += 2) {
            const byte = packed[(y * w + x) >> 1];
            const c0 = palette[(byte >> 4) & 0x0f];
            const c1 = palette[byte & 0x0f];
            setPixel(rgba, w, h, x, y, c0[0], c0[1], c0[2], c0[3]);
            setPixel(rgba, w, h, x + 1, y, c1[0], c1[1], c1[2], c1[3]);
        }
    }
    return encodePng(w, h, rgba);
}

const targets = [
    ['icon.png', generateModIcon()],
    ['textures/entity/aura_wisp.png', generateAuraWispStrip()],
    ['textures/entity/comet_ember.png', generateCometEmber()],
    ['textures/entity/filament_strip.png', generateFilamentStrip()],
    ['textures/entity/spark_dot.png', generateSparkDot()],
    ['textures/entity/star_mote.png', generateStarMote()],
    ['textures/entity/area_glow.png', generateAreaGlow()],
    ['textures/item/majesty_orb.png', generateMajestyOrbItem()],
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

for (const soundRel of ['sounds/majestic_presence.ogg', 'sounds/majestic_sparkle.ogg']) {
    const soundPath = path.join(assetsRoot, soundRel);
    if (checkOnly) {
        if (!fs.existsSync(soundPath) || fs.statSync(soundPath).size < 256) {
            console.error(`Missing or empty sound asset: ${soundRel}`);
            mismatches++;
        }
    }
}

if (checkOnly) {
    if (mismatches > 0) {
        process.exit(1);
    }
    console.log(`Verified ${targets.length} generated textures and 2 sound assets.`);
} else {
    console.log(`Generated ${targets.length} textures into ${assetsRoot}.`);
}
