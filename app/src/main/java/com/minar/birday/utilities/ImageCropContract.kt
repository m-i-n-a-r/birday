package com.minar.birday.utilities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.IntentCompat
import com.minar.birday.activities.ImageCropActivity

/**
 * Launches [ImageCropActivity] with the given source [Uri] and returns the cropped image [Uri],
 * or null if the user cancelled.
 */
class ImageCropContract : ActivityResultContract<Uri, Uri?>() {

    override fun createIntent(context: Context, input: Uri): Intent =
        Intent(context, ImageCropActivity::class.java)
            .putExtra(ImageCropActivity.EXTRA_SOURCE_URI, input)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        return IntentCompat.getParcelableExtra(
            intent,
            ImageCropActivity.EXTRA_RESULT_URI,
            Uri::class.java,
        )
    }
}
