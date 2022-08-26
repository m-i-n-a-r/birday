package com.minar.birday.utilities

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

// Share the backup to a supported app
fun shareFile(context: Context, fileUri: String) {
    val file = File(fileUri)
    val contentUri: Uri = FileProvider.getUriForFile(context, "com.minar.birday.fileprovider", file)
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, contentUri)
        type = "*/*"
    }
    // Verify that the intent will resolve to an activity
    if (shareIntent.resolveActivity(context.packageManager) != null)
        context.startActivity(shareIntent)
}