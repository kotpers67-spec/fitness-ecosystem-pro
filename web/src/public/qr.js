/**
 * Fitness Ecosystem Pro - Verified Vector SVG QR Code Renderer
 * Powered by standard ISO/IEC 18004 QRCode engine
 * Fully readable by Android camera, ZXing, and iOS camera.
 */
(function (global) {
  'use strict';

  function generateQrSvg(text, sizeOrOptions = {}) {
    const opts = typeof sizeOrOptions === 'number' ? { size: sizeOrOptions } : (sizeOrOptions || {});
    const size = opts.size || 200;
    const margin = opts.margin !== undefined ? opts.margin : 4;
    const cleanText = String(text || '');

    if (!cleanText) return '';

    if (global.QRCode && typeof global.QRCode.toString === 'function') {
      let output = '';
      global.QRCode.toString(cleanText, {
        type: 'svg',
        margin: margin,
        color: {
          dark: opts.color || '#000000',
          light: opts.background || '#ffffff'
        }
      }, function (err, svg) {
        if (!err && svg) {
          output = svg.replace('<svg ', `<svg width="${size}" height="${size}" aria-label="QR Code for ${cleanText}" `);
        }
      });
      if (output) return output;
    }

    return '';
  }

  const SwissQr = {
    generateQrSvg
  };

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = SwissQr;
  } else {
    global.SwissQr = SwissQr;
    global.generateQrSvg = generateQrSvg;
  }
})(typeof window !== 'undefined' ? window : globalThis);
