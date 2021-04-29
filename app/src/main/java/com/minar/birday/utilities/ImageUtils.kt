package com.minar.birday.utilities

import android.graphics.*
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
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

// Get the smallest dimension in a non-square image to crop and resize it
fun getBitmapSquareSize(bitmap: Bitmap): Int {
    return bitmap.width.coerceAtMost(bitmap.height)
}

// Transform a square bitmap in a circular bitmap, useful for notification
fun getCircularBitmap(bitmap: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(
        bitmap.width,
        bitmap.height,
        Bitmap.Config.ARGB_8888,
    )
    val canvas = Canvas(output)
    val color: Int = Color.GRAY
    val paint = Paint()
    val rect = Rect(0, 0, bitmap.width, bitmap.height)
    val rectF = RectF(rect)
    paint.isAntiAlias = true
    canvas.drawARGB(0, 0, 0, 0)
    paint.color = color
    canvas.drawOval(rectF, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap, rect, rect, paint)
    return output
}

// Loop an animated vector drawable indefinitely
fun ImageView.applyLoopingAnimatedVectorDrawable(@DrawableRes animatedVector: Int) {
    val animated = AnimatedVectorDrawableCompat.create(context, animatedVector)
    animated?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
        override fun onAnimationEnd(drawable: Drawable?) {
            this@applyLoopingAnimatedVectorDrawable.post { animated.start() }
        }
    })
    this.setImageDrawable(animated)
    animated?.start()
}
