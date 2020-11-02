package com.minar.birday.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

// Given a bitmap, convert it to a byte array
fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
    val imgConverted: ByteArray = byteArrayOf()
    return try {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
        stream.toByteArray()
    } catch (e: Exception) {
        imgConverted
    }
}

// Given a byte array containing an image, return the corresponding bitmap
fun byteArrayToBitmap(byteImg: ByteArray): Bitmap {
    // If the string is empty, just return an empty white bitmap
    if (byteImg.isEmpty()) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val stream: InputStream = ByteArrayInputStream(byteImg)
    return BitmapFactory.decodeStream(stream)
}

