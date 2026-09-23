package com.onip.cartoonip.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object PhotoFiles {

    // Une photo de fiche par jeton unique (galerie jusqu'à MAX_HOUSEHOLD_PHOTOS) — un jeton (pas
    // un simple index 0..3) pour que retirer une photo du milieu de la galerie ne fasse jamais
    // correspondre le mauvais fichier à la mauvaise position ; sans lui, chaque nouvelle prise
    // écraserait la précédente au lieu de s'ajouter.
    fun fileFor(context: Context, householdId: String, token: String): File {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        return File(dir, "${householdId}_$token.jpg")
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
