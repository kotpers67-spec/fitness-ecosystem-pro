/**
 * Swiss Clean UI - Standalone Pure JavaScript SVG QR Code Generator
 * Zero external dependencies. Zero CDN calls.
 * Renders crisp, scalable vector SVG for pairing PINs and URLs.
 */

(function (global) {
  'use strict';

  // Galois Field GF(256) tables
  const EXP = new Uint8Array(512);
  const LOG = new Uint8Array(256);
  let val = 1;
  for (let i = 0; i < 255; i++) {
    EXP[i] = val;
    EXP[i + 255] = val;
    LOG[val] = i;
    val <<= 1;
    if (val & 256) val ^= 0x11d;
  }

  function gmul(a, b) {
    if (a === 0 || b === 0) return 0;
    return EXP[LOG[a] + LOG[b]];
  }

  function rsGenPoly(degree) {
    let poly = [1];
    for (let i = 0; i < degree; i++) {
      const next = [1, EXP[i]];
      const res = new Uint8Array(poly.length + 1);
      for (let j = 0; j < poly.length; j++) {
        for (let k = 0; k < next.length; k++) {
          res[j + k] ^= gmul(poly[j], next[k]);
        }
      }
      poly = Array.from(res);
    }
    return poly;
  }

  function rsRemainder(data, degree) {
    const gen = rsGenPoly(degree);
    const msg = new Uint8Array(data.length + degree);
    msg.set(data, 0);
    for (let i = 0; i < data.length; i++) {
      const coef = msg[i];
      if (coef !== 0) {
        for (let j = 0; j < gen.length; j++) {
          msg[i + j] ^= gmul(gen[j], coef);
        }
      }
    }
    return Array.from(msg.slice(data.length));
  }

  // Version specs for Error Correction Level M
  const VERSION_SPECS = [
    null, // 0 index unused
    { version: 1, size: 21, totalCw: 26, ecCw: 10, blocks: [{ count: 1, dataCw: 16, ecCw: 10 }], align: [] },
    { version: 2, size: 25, totalCw: 44, ecCw: 16, blocks: [{ count: 1, dataCw: 28, ecCw: 16 }], align: [6, 18] },
    { version: 3, size: 29, totalCw: 70, ecCw: 26, blocks: [{ count: 1, dataCw: 44, ecCw: 26 }], align: [6, 22] },
    { version: 4, size: 33, totalCw: 100, ecCw: 36, blocks: [{ count: 2, dataCw: 32, ecCw: 18 }], align: [6, 26] },
    { version: 5, size: 37, totalCw: 134, ecCw: 48, blocks: [{ count: 2, dataCw: 43, ecCw: 24 }], align: [6, 30] },
    { version: 6, size: 41, totalCw: 172, ecCw: 64, blocks: [{ count: 4, dataCw: 27, ecCw: 16 }], align: [6, 34] }
  ];

  function encodeDataBytes(text, targetDataCw) {
    const utf8 = [];
    for (let i = 0; i < text.length; i++) {
      let code = text.charCodeAt(i);
      if (code < 0x80) {
        utf8.push(code);
      } else if (code < 0x800) {
        utf8.push(0xc0 | (code >> 6), 0x80 | (code & 0x3f));
      } else {
        utf8.push(0xe0 | (code >> 12), 0x80 | ((code >> 6) & 0x3f), 0x80 | (code & 0x3f));
      }
    }

    // 8-bit Byte Mode: indicator 0100 (4 bits) + length (8 bits for v1-9)
    const bits = [];
    function pushBits(val, len) {
      for (let i = len - 1; i >= 0; i--) {
        bits.push((val >> i) & 1);
      }
    }

    pushBits(0b0100, 4);
    pushBits(utf8.length, 8);
    for (const b of utf8) {
      pushBits(b, 8);
    }

    // Terminator (up to 4 zeroes)
    const capacityBits = targetDataCw * 8;
    const termLen = Math.min(4, capacityBits - bits.length);
    for (let i = 0; i < termLen; i++) bits.push(0);

    // Byte align
    while (bits.length % 8 !== 0) bits.push(0);

    // Bytes conversion
    const bytes = [];
    for (let i = 0; i < bits.length; i += 8) {
      let b = 0;
      for (let j = 0; j < 8; j++) {
        b = (b << 1) | bits[i + j];
      }
      bytes.push(b);
    }

    // Pad bytes: alternating 0xEC, 0x11
    const pad = [0xec, 0x11];
    let padIdx = 0;
    while (bytes.length < targetDataCw) {
      bytes.push(pad[padIdx % 2]);
      padIdx++;
    }

    return bytes;
  }

  function pickVersion(textLength) {
    for (let v = 1; v < VERSION_SPECS.length; v++) {
      const spec = VERSION_SPECS[v];
      let totalData = 0;
      for (const b of spec.blocks) totalData += b.count * b.dataCw;
      // 4 bits mode + 8 bits len + text bytes
      const neededBytes = textLength + 2;
      if (neededBytes <= totalData) {
        return spec;
      }
    }
    return VERSION_SPECS[6];
  }

  function generateMatrix(text) {
    const spec = pickVersion(text.length);
    let totalData = 0;
    for (const b of spec.blocks) totalData += b.count * b.dataCw;
    const dataBytes = encodeDataBytes(text, totalData);

    // Split into blocks and compute RS EC
    const blocksData = [];
    const blocksEc = [];
    let offset = 0;
    for (const b of spec.blocks) {
      for (let i = 0; i < b.count; i++) {
        const slice = dataBytes.slice(offset, offset + b.dataCw);
        offset += b.dataCw;
        blocksData.push(slice);
        blocksEc.push(rsRemainder(slice, b.ecCw));
      }
    }

    // Interleave data and EC codewords
    const finalCodewords = [];
    let maxDataLen = Math.max(...blocksData.map(b => b.length));
    for (let i = 0; i < maxDataLen; i++) {
      for (const b of blocksData) {
        if (i < b.length) finalCodewords.push(b[i]);
      }
    }
    let maxEcLen = Math.max(...blocksEc.map(b => b.length));
    for (let i = 0; i < maxEcLen; i++) {
      for (const b of blocksEc) {
        if (i < b.length) finalCodewords.push(b[i]);
      }
    }

    // Initialize matrix (null = unset, false = white, true = black)
    const size = spec.size;
    const matrix = Array.from({ length: size }, () => Array(size).fill(null));
    const isReserved = Array.from({ length: size }, () => Array(size).fill(false));

    function setModule(r, c, val, reserved = true) {
      if (r >= 0 && r < size && c >= 0 && c < size) {
        matrix[r][c] = Boolean(val);
        if (reserved) isReserved[r][c] = true;
      }
    }

    // 1. Finder patterns (7x7) + separators
    function placeFinder(r0, c0) {
      for (let r = -1; r <= 7; r++) {
        for (let c = -1; c <= 7; c++) {
          const row = r0 + r;
          const col = c0 + c;
          if (row < 0 || row >= size || col < 0 || col >= size) continue;
          if (r >= 0 && r <= 6 && c >= 0 && c <= 6) {
            const isBlack = (r === 0 || r === 6 || c === 0 || c === 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4));
            setModule(row, col, isBlack, true);
          } else {
            setModule(row, col, false, true); // separator
          }
        }
      }
    }

    placeFinder(0, 0);
    placeFinder(0, size - 7);
    placeFinder(size - 7, 0);

    // 2. Timing patterns
    for (let i = 8; i < size - 8; i++) {
      if (!isReserved[6][i]) setModule(6, i, i % 2 === 0, true);
      if (!isReserved[i][6]) setModule(i, 6, i % 2 === 0, true);
    }

    // 3. Alignment patterns (version >= 2)
    if (spec.align && spec.align.length > 0) {
      const coords = spec.align;
      for (const r of coords) {
        for (const c of coords) {
          if (isReserved[r][c]) continue;
          for (let dr = -2; dr <= 2; dr++) {
            for (let dc = -2; dc <= 2; dc++) {
              const isBlack = (Math.abs(dr) === 2 || Math.abs(dc) === 2 || (dr === 0 && dc === 0));
              setModule(r + dr, c + dc, isBlack, true);
            }
          }
        }
      }
    }

    // 4. Dark module
    setModule(4 * spec.version + 9, 8, true, true);

    // 5. Reserve format info areas
    for (let i = 0; i < 9; i++) {
      if (!isReserved[8][i]) isReserved[8][i] = true;
      if (!isReserved[i][8]) isReserved[i][8] = true;
    }
    for (let i = 0; i < 8; i++) {
      if (!isReserved[8][size - 1 - i]) isReserved[8][size - 1 - i] = true;
      if (!isReserved[size - 1 - i][8]) isReserved[size - 1 - i] = true;
    }

    // 6. Place Data Codewords (Zigzag pattern)
    let cwBits = [];
    for (const cw of finalCodewords) {
      for (let b = 7; b >= 0; b--) {
        cwBits.push((cw >> b) & 1);
      }
    }
    let bitIdx = 0;
    let upward = true;

    for (let rightCol = size - 1; rightCol > 0; rightCol -= 2) {
      if (rightCol === 6) rightCol--; // skip timing col
      const cols = [rightCol, rightCol - 1];
      const rows = [];
      for (let r = 0; r < size; r++) rows.push(r);
      if (upward) rows.reverse();

      for (const r of rows) {
        for (const c of cols) {
          if (!isReserved[r][c]) {
            const bit = bitIdx < cwBits.length ? cwBits[bitIdx] : 0;
            bitIdx++;
            setModule(r, c, bit === 1, false);
          }
        }
      }
      upward = !upward;
    }

    // 7. Mask pattern: Mask 0 ((row + col) % 2 === 0)
    // Error correction Level M format bits for mask 0: 101010000010010 (pre-computed with mask 0x5412)
    const formatBits = [1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0];

    // Apply Mask 0 to non-reserved data modules
    for (let r = 0; r < size; r++) {
      for (let c = 0; c < size; c++) {
        if (!isReserved[r][c]) {
          if ((r + c) % 2 === 0) {
            matrix[r][c] = !matrix[r][c];
          }
        }
      }
    }

    // 8. Place format bits
    // Top-left
    const tlCoords = [
      [8, 0], [8, 1], [8, 2], [8, 3], [8, 4], [8, 5], [8, 7], [8, 8],
      [7, 8], [5, 8], [4, 8], [3, 8], [2, 8], [1, 8], [0, 8]
    ];
    for (let i = 0; i < 15; i++) {
      const [r, c] = tlCoords[i];
      matrix[r][c] = formatBits[i] === 1;
    }

    // Bottom-left & Top-right
    const blTrCoords = [
      [size - 1, 8], [size - 2, 8], [size - 3, 8], [size - 4, 8], [size - 5, 8], [size - 6, 8], [size - 7, 8],
      [8, size - 8], [8, size - 7], [8, size - 6], [8, size - 5], [8, size - 4], [8, size - 3], [8, size - 2], [8, size - 1]
    ];
    for (let i = 0; i < 15; i++) {
      const [r, c] = blTrCoords[i];
      matrix[r][c] = formatBits[i] === 1;
    }

    return matrix;
  }

  function generateQrSvg(text, sizeOrOptions = {}) {
    const opts = typeof sizeOrOptions === 'number' ? { size: sizeOrOptions } : (sizeOrOptions || {});
    const {
      size = 200,
      color = '#000000',
      background = '#ffffff',
      margin = 2
    } = opts;

    const matrix = generateMatrix(String(text || ''));
    const moduleCount = matrix.length;
    const totalUnits = moduleCount + margin * 2;

    let pathD = '';
    for (let r = 0; r < moduleCount; r++) {
      for (let c = 0; c < moduleCount; c++) {
        if (matrix[r][c]) {
          const x = c + margin;
          const y = r + margin;
          pathD += `M${x},${y}h1v1h-1z `;
        }
      }
    }

    const bgRect = background !== 'transparent'
      ? `<rect width="${totalUnits}" height="${totalUnits}" fill="${background}"/>`
      : '';

    return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${totalUnits} ${totalUnits}" width="${size}" height="${size}" shape-rendering="crispEdges" aria-label="QR Code for ${text}">${bgRect}<path d="${pathD.trim()}" fill="${color}"/></svg>`;
  }

  // Export to global / module
  const SwissQr = {
    generateMatrix,
    generateQrSvg
  };

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = SwissQr;
  } else {
    global.SwissQr = SwissQr;
    global.generateQrSvg = generateQrSvg;
  }
})(typeof window !== 'undefined' ? window : globalThis);
