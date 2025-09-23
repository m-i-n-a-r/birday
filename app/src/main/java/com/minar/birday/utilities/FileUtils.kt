package com.minar.birday.utilities

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.AnyRes
import com.minar.birday.BuildConfig
import com.minar.birday.R

// Get the URI for a file in the raw folder
fun getResourceUri(@AnyRes resourceId: Int): Uri =
    Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(BuildConfig.APPLICATION_ID)
        .path(resourceId.toString())
        .build()

// Share a content Uri (e.g. an Uri from SAF)
fun shareUri(context: Context, uri: Uri) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_STREAM, uri)
        type = context.contentResolver.getType(uri) ?: "*/*"
    }
    if (shareIntent.resolveActivity(context.packageManager) != null)
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_event)))
}
