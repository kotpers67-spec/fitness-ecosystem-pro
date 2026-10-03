package com.trainerapp.pro.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

object QrCodeScannerHelper {

    private const val MAX_SCAN_DIMENSION = 800

    fun decodeFromBitmap(bitmap: Bitmap): String? {
        var scaledBitmap: Bitmap? = null
        return try {
            val width = bitmap.width
            val height = bitmap.height

            val workingBitmap = if (width > MAX_SCAN_DIMENSION || height > MAX_SCAN_DIMENSION) {
                val scale = MAX_SCAN_DIMENSION.toFloat() / maxOf(width, height)
                val targetW = (width * scale).toInt().coerceAtLeast(1)
                val targetH = (height * scale).toInt().coerceAtLeast(1)
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                scaledBitmap
            } else {
                bitmap
            }

            val curW = workingBitmap.width
            val curH = workingBitmap.height
            val pixels = IntArray(curW * curH)
            workingBitmap.getPixels(pixels, 0, curW, 0, 0, curW, curH)

            val source = RGBLuminanceSource(curW, curH, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val hints = mapOf(
                com.google.zxing.DecodeHintType.TRY_HARDER to true,
                com.google.zxing.DecodeHintType.POSSIBLE_FORMATS to listOf(com.google.zxing.BarcodeFormat.QR_CODE)
            )
            MultiFormatReader().apply { setHints(hints) }.decode(binaryBitmap).text
        } catch (_: Throwable) {
            null
        } finally {
            if (scaledBitmap != null && scaledBitmap != bitmap && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
        }
    }

    fun decodeFromUri(context: Context, uri: Uri): String? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            var sampleSize = 1
            val maxSide = maxOf(options.outWidth, options.outHeight)
            while (maxSide / (sampleSize * 2) >= MAX_SCAN_DIMENSION) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            decodeFromBitmap(bitmap)
        } catch (_: Throwable) {
            null
        }
    }
}
