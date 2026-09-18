package com.onip.cartoonip.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object PhotoFiles {

    fun fileFor(context: Context, householdId: String): File {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        return File(dir, "$householdId.jpg")
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
