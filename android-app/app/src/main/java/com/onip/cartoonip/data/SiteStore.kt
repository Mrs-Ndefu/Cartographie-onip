package com.onip.cartoonip.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.onip.cartoonip.data.model.CapturedSite
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stockage local des fiches de repérage de site — même principe que CaptureStore (un fichier
 * JSON dans le stockage privé de l'app, pas de base de données embarquée).
 */
class SiteStore(context: Context) {

    private val file = File(context.filesDir, "sites.json")
    private val gson = Gson()
    private val listType = object : TypeToken<List<CapturedSite>>() {}.type

    private val _sites = MutableStateFlow(load())
    val sites: StateFlow<List<CapturedSite>> = _sites.asStateFlow()

    @Synchronized
    private fun load(): List<CapturedSite> {
        if (!file.exists()) return emptyList()
        return try {
            gson.fromJson<List<CapturedSite>>(file.readText(), listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    private fun persist(list: List<CapturedSite>) {
        file.writeText(gson.toJson(list))
        _sites.value = list
    }

    @Synchronized
    fun upsert(site: CapturedSite) {
        val current = _sites.value.toMutableList()
        val index = current.indexOfFirst { it.id == site.id }
        if (index >= 0) current[index] = site else current.add(0, site)
        persist(current)
    }

    @Synchronized
    fun delete(id: String) {
        persist(_sites.value.filterNot { it.id == id })
    }

    fun get(id: String): CapturedSite? = _sites.value.firstOrNull { it.id == id }
}
