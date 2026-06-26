package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QrCodeGenerator {
    fun generateQrCode(content: String, size: Int = 512, fgColor: Int = Color.BLACK, bgColor: Int = Color.WHITE): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) fgColor else bgColor)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
