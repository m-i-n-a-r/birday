package com.minar.birday.utilities

import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.minar.birday.R
import com.minar.birday.model.EventCode
import com.minar.birday.model.EventResult
import java.io.ByteArrayOutputStream
import androidx.core.graphics.createBitmap


// Given a bitmap, convert it to a byte array
fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
    val imgConverted: ByteArray = byteArrayOf()
    return try {
        bitmap.toByteArray()
    } catch (_: Exception) {
        imgConverted
    }
}

// Given a byte array containing an image, return the corresponding bitmap
fun byteArrayToBitmap(byteImg: ByteArray): Bitmap {
    // If the string is empty, just return an empty white bitmap
    if (byteImg.isEmpty()) return createBitmap(1, 1)
    return BitmapFactory.decodeByteArray(byteImg, 0, byteImg.size)
}

// Generate a circular bitmap with initials (first + last name), used as placeholder
fun getInitialBitmap(name: String, surname: String?, size: Int): Bitmap {
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)

    // Pick a consistent color based on the full name
    val fullName = "$name${surname.orEmpty()}"
    val colors = intArrayOf(
        0xFF1E88E5.toInt(), 0xFF43A047.toInt(), 0xFFE53935.toInt(),
        0xFF8E24AA.toInt(), 0xFFFB8C00.toInt(), 0xFF00ACC1.toInt(),
        0xFF3949AB.toInt(), 0xFFD81B60.toInt(), 0xFF6D4C41.toInt(),
    )
    val bgColor = colors[fullName.hashCode().and(0x7FFFFFFF) % colors.size]

    val bgPaint = Paint().apply {
        isAntiAlias = true
        color = bgColor
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

    // Build initials: first letter of name + first letter of surname
    val initials = buildString {
        append(name.first().uppercaseChar())
        if (!surname.isNullOrBlank()) append(surname.first().uppercaseChar())
    }

    val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textSize = if (initials.length > 1) size * 0.38f else size * 0.5f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    val textBounds = Rect()
    textPaint.getTextBounds(initials, 0, initials.length, textBounds)
    canvas.drawText(initials, size / 2f, size / 2f + textBounds.height() / 2f, textPaint)

    return bitmap
}

// Get the smallest dimension in a non-square image to crop and resize it
fun getBitmapSquareSize(bitmap: Bitmap): Int {
    return bitmap.width.coerceAtMost(bitmap.height)
}

// Transform a square bitmap in a circular bitmap, useful for notification
fun getCircularBitmap(bitmap: Bitmap): Bitmap {
    val output = createBitmap(bitmap.width, bitmap.height)
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

// Return true if the event has an image, else false
fun setEventImageOrPlaceholder(event: EventResult, eventImage: ImageView): Boolean {
    if (event.image != null && event.image.isNotEmpty()) {
        // The click is not implemented atm
        eventImage.setImageBitmap(byteArrayToBitmap(event.image))
        return true
    } else {
        eventImage.setImageDrawable(
            ContextCompat.getDrawable(
                eventImage.context,
                // Set the image depending on the event type
                when (event.type) {
                    EventCode.BIRTHDAY.name -> R.drawable.placeholder_birthday_image
                    EventCode.ANNIVERSARY.name -> R.drawable.placeholder_anniversary_image
                    EventCode.DEATH.name -> R.drawable.placeholder_death_image
                    EventCode.NAME_DAY.name -> R.drawable.placeholder_name_day_image
                    else -> R.drawable.placeholder_other_image
                }
            )
        )
        return false
    }
}

// Loop an animated vector drawable indefinitely
fun ImageView.applyLoopingAnimatedVectorDrawable(
    @DrawableRes animatedVector: Int,
    endDelay: Long = 0,
    disableLooping: Boolean = false
) {
    val animated = AnimatedVectorDrawableCompat.create(context, animatedVector)
    // Ability to disable the loop, for a future option
    if (!disableLooping) {
        animated?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                Handler(Looper.getMainLooper()).postDelayed({
                    this@applyLoopingAnimatedVectorDrawable.post { animated.start() }
                }, endDelay)
            }
        })
    }
    this.setImageDrawable(animated)
    animated?.start()
}

// Extension function to convert bitmap to byte array
fun Bitmap.toByteArray(): ByteArray {
    ByteArrayOutputStream().apply {
        compress(Bitmap.CompressFormat.JPEG, 100, this)
        return toByteArray()
    }
}
