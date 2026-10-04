const fs = require('node:fs');
const zlib = require('node:zlib');
const path = require('node:path');

// CRC32 calculation for PNG chunks
const crcTable = [];
for (let n = 0; n < 256; n++) {
  let c = n;
  for (let k = 0; k < 8; k++) {
    if (c & 1) c = 0xedb88320 ^ (c >>> 1);
    else c = c >>> 1;
  }
  crcTable[n] = c;
}

function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) {
    c = crcTable[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  }
  return (c ^ 0xffffffff) >>> 0;
}

function makeChunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length, 0);
  const typeBuf = Buffer.from(type, 'ascii');
  const body = Buffer.concat([typeBuf, data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(body), 0);
  return Buffer.concat([len, body, crc]);
}

function createPng(width, height, pixelFn) {
  const sig = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr.writeUInt8(8, 8); // 8-bit
  ihdr.writeUInt8(6, 9); // RGBA
  ihdr.writeUInt8(0, 10);
  ihdr.writeUInt8(0, 11);
  ihdr.writeUInt8(0, 12);
  const ihdrChunk = makeChunk('IHDR', ihdr);

  const rowSize = 1 + width * 4;
  const raw = Buffer.alloc(height * rowSize);

  for (let y = 0; y < height; y++) {
    const rowOffset = y * rowSize;
    raw[rowOffset] = 0; // Filter: None
    for (let x = 0; x < width; x++) {
      const [r, g, b, a] = pixelFn(x, y, width, height);
      const pxOffset = rowOffset + 1 + x * 4;
      raw[pxOffset] = r;
      raw[pxOffset + 1] = g;
      raw[pxOffset + 2] = b;
      raw[pxOffset + 3] = a;
    }
  }

  const compressed = zlib.deflateSync(raw, { level: 9 });
  const idatChunk = makeChunk('IDAT', compressed);
  const iendChunk = makeChunk('IEND', Buffer.alloc(0));

  return Buffer.concat([sig, ihdrChunk, idatChunk, iendChunk]);
}

// 1. Athlete Icon (Neon Lime #c8ff00 + Dumbbell)
function renderAthleteIcon(x, y, w, h) {
  const nx = (x / w) * 2 - 1;
  const ny = (y / h) * 2 - 1;
  const dist = Math.sqrt(nx * nx + ny * ny);

  let r = 20, g = 20, b = 20, a = 255;

  if (dist > 0.72 && dist < 0.82) return [200, 255, 0, 255]; // Lime border
  if (dist <= 0.72) { r = 24; g = 24; b = 24; }

  // Dumbbell bar
  if (Math.abs(ny) <= 0.06 && Math.abs(nx) <= 0.42) return [200, 255, 0, 255];

  // Weights Left
  if (nx >= -0.42 && nx <= -0.34 && Math.abs(ny) <= 0.32) return [200, 255, 0, 255];
  if (nx >= -0.32 && nx <= -0.26 && Math.abs(ny) <= 0.24) return [200, 255, 0, 255];

  // Weights Right
  if (nx >= 0.26 && nx <= 0.32 && Math.abs(ny) <= 0.24) return [200, 255, 0, 255];
  if (nx >= 0.34 && nx <= 0.42 && Math.abs(ny) <= 0.32) return [200, 255, 0, 255];

  // Grip Knurling
  if (Math.abs(nx) <= 0.12 && Math.abs(ny) <= 0.08) return [230, 255, 80, 255];

  return [r, g, b, a];
}

// 2. Trainer Icon (Emerald Green #10b981 + Coach Clipboard/Whistle)
function renderTrainerIcon(x, y, w, h) {
  const nx = (x / w) * 2 - 1;
  const ny = (y / h) * 2 - 1;
  const dist = Math.sqrt(nx * nx + ny * ny);

  let r = 16, g = 24, b = 20, a = 255;

  if (dist > 0.72 && dist < 0.82) return [16, 185, 129, 255]; // Emerald border
  if (dist <= 0.72) { r = 20; g = 30; b = 25; }

  // Clipboard Outline: x in [-0.32, 0.32], y in [-0.38, 0.42]
  if (Math.abs(nx) <= 0.32 && ny >= -0.38 && ny <= 0.42) {
    // Border of board
    if (Math.abs(nx) >= 0.26 || ny >= 0.36 || (ny <= -0.32 && Math.abs(nx) >= 0.15)) {
      return [16, 185, 129, 255];
    }
    // Top Clip: x in [-0.15, 0.15], y in [-0.44, -0.30]
    if (Math.abs(nx) <= 0.15 && ny >= -0.44 && ny <= -0.30) {
      return [52, 211, 153, 255];
    }
    // 3 Plan Lines on board
    if ((Math.abs(ny - (-0.12)) <= 0.03 || Math.abs(ny - (0.05)) <= 0.03 || Math.abs(ny - (0.22)) <= 0.03) && Math.abs(nx) <= 0.20) {
      return [52, 211, 153, 255];
    }
    // Checkmark on first line: x in [-0.22, -0.16]
    if (nx >= -0.24 && nx <= -0.16 && Math.abs(ny - (-0.12)) <= 0.04) {
      return [200, 255, 0, 255];
    }
  }

  return [r, g, b, a];
}

const pubDir = path.join(__dirname, 'web', 'src', 'public');

// Write Athlete Icons
fs.writeFileSync(path.join(pubDir, 'icon-192.png'), createPng(192, 192, renderAthleteIcon));
fs.writeFileSync(path.join(pubDir, 'icon-512.png'), createPng(512, 512, renderAthleteIcon));
fs.writeFileSync(path.join(pubDir, 'icon-athlete-192.png'), createPng(192, 192, renderAthleteIcon));
fs.writeFileSync(path.join(pubDir, 'icon-athlete-512.png'), createPng(512, 512, renderAthleteIcon));

// Write Trainer Icons
fs.writeFileSync(path.join(pubDir, 'icon-trainer-192.png'), createPng(192, 192, renderTrainerIcon));
fs.writeFileSync(path.join(pubDir, 'icon-trainer-512.png'), createPng(512, 512, renderTrainerIcon));

// Matching SVGs
const athleteSvg = `<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">
  <rect width="512" height="512" rx="112" fill="#141414"/>
  <circle cx="256" cy="256" r="196" fill="#1c1c1c" stroke="#c8ff00" stroke-width="18"/>
  <rect x="140" y="242" width="232" height="28" rx="8" fill="#c8ff00"/>
  <rect x="148" y="196" width="22" height="120" rx="8" fill="#c8ff00"/>
  <rect x="122" y="174" width="20" height="164" rx="8" fill="#c8ff00"/>
  <rect x="342" y="196" width="22" height="120" rx="8" fill="#c8ff00"/>
  <rect x="370" y="174" width="20" height="164" rx="8" fill="#c8ff00"/>
  <rect x="226" y="238" width="60" height="36" rx="6" fill="#f0ff75"/>
</svg>`;

const trainerSvg = `<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">
  <rect width="512" height="512" rx="112" fill="#0f1f17"/>
  <circle cx="256" cy="256" r="196" fill="#142920" stroke="#10b981" stroke-width="18"/>
  <!-- Clipboard -->
  <rect x="160" y="150" width="192" height="230" rx="16" fill="none" stroke="#10b981" stroke-width="16"/>
  <!-- Clip -->
  <rect x="216" y="130" width="80" height="36" rx="8" fill="#34d399"/>
  <!-- Lines -->
  <line x1="200" y1="220" x2="310" y2="220" stroke="#34d399" stroke-width="12" stroke-linecap="round"/>
  <line x1="200" y1="270" x2="310" y2="270" stroke="#34d399" stroke-width="12" stroke-linecap="round"/>
  <line x1="200" y1="320" x2="280" y2="320" stroke="#34d399" stroke-width="12" stroke-linecap="round"/>
</svg>`;

fs.writeFileSync(path.join(pubDir, 'icon.svg'), athleteSvg, 'utf-8');
fs.writeFileSync(path.join(pubDir, 'icon-athlete.svg'), athleteSvg, 'utf-8');
fs.writeFileSync(path.join(pubDir, 'icon-trainer.svg'), trainerSvg, 'utf-8');

console.log('Successfully generated Athlete & Trainer distinct icons!');
