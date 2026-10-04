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

  // Build raw scanlines: each row = filter byte (0) + width * 4 bytes
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

// Draw a modern Fitness Pro Icon:
// Dark background #121212 with Lime Green #C8FF00 border & Dumbbell / Lightning / PRO Emblem
function renderIconPixel(x, y, w, h) {
  const nx = (x / w) * 2 - 1; // -1 to 1
  const ny = (y / h) * 2 - 1; // -1 to 1
  const dist = Math.sqrt(nx * nx + ny * ny);

  // Background: Rich Dark Charcoal #141414
  let r = 20, g = 20, b = 20, a = 255;

  // Outer Squircle / Circle Highlight Ring (Lime Green Accent)
  if (dist > 0.72 && dist < 0.82) {
    // Vibrant Lime #c8ff00
    return [200, 255, 0, 255];
  }

  // Inside circle background
  if (dist <= 0.72) {
    r = 24; g = 24; b = 24;
  }

  // Center Dumbbell / Barbell Graphic in Lime Green #c8ff00
  // Bar: y in [-0.06, 0.06], x in [-0.45, 0.45]
  if (Math.abs(ny) <= 0.06 && Math.abs(nx) <= 0.42) {
    return [200, 255, 0, 255]; // Bar
  }

  // Left Plates:
  // Outer plate: x in [-0.42, -0.34], y in [-0.32, 0.32]
  if (nx >= -0.42 && nx <= -0.34 && Math.abs(ny) <= 0.32) {
    return [200, 255, 0, 255];
  }
  // Inner plate: x in [-0.32, -0.26], y in [-0.24, 0.24]
  if (nx >= -0.32 && nx <= -0.26 && Math.abs(ny) <= 0.24) {
    return [200, 255, 0, 255];
  }

  // Right Plates:
  // Inner plate: x in [0.26, 0.32], y in [-0.24, 0.24]
  if (nx >= 0.26 && nx <= 0.32 && Math.abs(ny) <= 0.24) {
    return [200, 255, 0, 255];
  }
  // Outer plate: x in [0.34, 0.42], y in [-0.32, 0.32]
  if (nx >= 0.34 && nx <= 0.42 && Math.abs(ny) <= 0.32) {
    return [200, 255, 0, 255];
  }

  // Center Grip / Collar knurling accents:
  if (Math.abs(nx) <= 0.12 && Math.abs(ny) <= 0.08) {
    return [230, 255, 80, 255]; // Brighter center grip
  }

  return [r, g, b, a];
}

const pubDir = path.join(__dirname, 'web', 'src', 'public');
fs.writeFileSync(path.join(pubDir, 'icon-192.png'), createPng(192, 192, renderIconPixel));
fs.writeFileSync(path.join(pubDir, 'icon-512.png'), createPng(512, 512, renderIconPixel));

// Also generate a matching SVG icon
const svgContent = `<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">
  <rect width="512" height="512" rx="112" fill="#141414"/>
  <circle cx="256" cy="256" r="196" fill="#1c1c1c" stroke="#c8ff00" stroke-width="18"/>
  <!-- Dumbbell Bar -->
  <rect x="140" y="242" width="232" height="28" rx="8" fill="#c8ff00"/>
  <!-- Left Weights -->
  <rect x="148" y="196" width="22" height="120" rx="8" fill="#c8ff00"/>
  <rect x="122" y="174" width="20" height="164" rx="8" fill="#c8ff00"/>
  <!-- Right Weights -->
  <rect x="342" y="196" width="22" height="120" rx="8" fill="#c8ff00"/>
  <rect x="370" y="174" width="20" height="164" rx="8" fill="#c8ff00"/>
  <!-- Center Knurling -->
  <rect x="226" y="238" width="60" height="36" rx="6" fill="#f0ff75"/>
</svg>`;

fs.writeFileSync(path.join(pubDir, 'icon.svg'), svgContent, 'utf-8');
console.log('Successfully generated high-contrast PWA icons (192, 512, SVG)!');
