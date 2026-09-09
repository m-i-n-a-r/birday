package com.minar.birday.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.canhub.cropper.CropImageView
import com.minar.birday.R
import com.minar.birday.databinding.ActivityImageCropBinding
import com.minar.birday.utilities.addInsetsByMargin
import com.minar.birday.utilities.addInsetsByPadding
import com.minar.birday.utilities.applyUserTheme
import java.io.File

/**
 * Hosts a [CropImageView] and returns the cropped image as a content Uri to the caller.
 * The source Uri is passed in via [EXTRA_SOURCE_URI] and the result Uri is returned via
 * [EXTRA_RESULT_URI] in the activity result intent.
 */
class ImageCropActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageCropBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityImageCropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default outcome: predictive back / system back simply finishes with CANCELED
        setResult(Activity.RESULT_CANCELED)

        // Insets: pad the crop view so the image isn't clipped, push the FABs in from the system bars
        binding.cropImageView.addInsetsByPadding(top = true, bottom = true, left = true, right = true)
        binding.imageCropCancelButton.addInsetsByMargin(bottom = true, left = true)
        binding.imageCropConfirmButton.addInsetsByMargin(bottom = true, right = true)

        binding.imageCropCancelButton.setOnClickListener { finish() }
        binding.imageCropConfirmButton.setOnClickListener { confirmCrop() }

        val sourceUri = intent.getParcelableExtraCompat<Uri>(EXTRA_SOURCE_URI)
        if (sourceUri == null) {
            finish()
            return
        }
        binding.cropImageView.setOnCropImageCompleteListener { _, result ->
            if (result.isSuccessful && result.uriContent != null) {
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_RESULT_URI, result.uriContent))
                finish()
            } else {
                Toast.makeText(this, R.string.crop_image_error, Toast.LENGTH_SHORT).show()
            }
        }
        binding.cropImageView.setImageUriAsync(sourceUri)
    }

    private fun applyTheme() {
        applyUserTheme(PreferenceManager.getDefaultSharedPreferences(this))
    }

    private fun confirmCrop() {
        // Cropper requires a content:// URI, so save into the app's cache and expose it via FileProvider.
        // Previous crops are dropped first: the caller only ever needs the latest one, and nothing
        // else would delete them once the bitmap has been read.
        val cropDir = File(cacheDir, "image_crop").apply { mkdirs() }
        cropDir.listFiles()?.forEach { it.delete() }
        val outFile = File(cropDir, "crop_${System.currentTimeMillis()}.jpg")
        val outUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", outFile)
        binding.cropImageView.croppedImageAsync(
            saveCompressFormat = Bitmap.CompressFormat.JPEG,
            saveCompressQuality = 90,
            reqWidth = MAX_SIZE_PX,
            reqHeight = MAX_SIZE_PX,
            options = CropImageView.RequestSizeOptions.RESIZE_INSIDE,
            customOutputUri = outUri,
        )
    }

    private inline fun <reified T> Intent.getParcelableExtraCompat(key: String): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            getParcelableExtra(key, T::class.java)
        else
            @Suppress("DEPRECATION") getParcelableExtra(key) as? T

    companion object {
        const val EXTRA_SOURCE_URI = "com.minar.birday.extra.SOURCE_URI"
        const val EXTRA_RESULT_URI = "com.minar.birday.extra.RESULT_URI"

        /** Output is bounded to this many pixels on the longest side (stored as a small JPEG). */
        private const val MAX_SIZE_PX = 450
    }
}
