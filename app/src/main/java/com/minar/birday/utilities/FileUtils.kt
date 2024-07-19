package com.minar.birday.utilities

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.AnyRes
import androidx.core.content.FileProvider
import com.minar.birday.BuildConfig
import java.io.File

// Share the backup to a supported app
fun shareFile(context: Context, fileUri: String) {
    val file = File(fileUri)
    val contentUri: Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, contentUri)
        type = "*/*"
    }
    // Verify that the intent will resolve to an activity
    if (shareIntent.resolveActivity(context.packageManager) != null)
        context.startActivity(shareIntent)
}

// Get the URI for a file in the raw folder
fun getResourceUri(@AnyRes resourceId: Int): Uri =
    Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(BuildConfig.APPLICATION_ID)
        .path(resourceId.toString())
        .build()