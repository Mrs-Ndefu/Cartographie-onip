package com.onip.cartoonip.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.onip.cartoonip.data.model.ReperagePoint
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stockage local des points de repérage — même principe que CaptureStore (un fichier JSON dans
 * le stockage privé de l'app, pas de base de données embarquée) : ces points ne sont qu'une aide
 * terrain pour l'agent, jamais envoyés au serveur.
 */
class ReperageStore(context: Context) {

    private val file = File(context.filesDir, "reperage.json")
    private val gson = Gson()
    private val listType = object : TypeToken<List<ReperagePoint>>() {}.type

    private val _points = MutableStateFlow(load())
    val points: StateFlow<List<ReperagePoint>> = _points.asStateFlow()

    @Synchronized
    private fun load(): List<ReperagePoint> {
        if (!file.exists()) return emptyList()
        return try {
            gson.fromJson<List<ReperagePoint>>(file.readText(), listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    private fun persist(list: List<ReperagePoint>) {
        file.writeText(gson.toJson(list))
        _points.value = list
    }

    @Synchronized
    fun add(point: ReperagePoint) {
        persist(listOf(point) + _points.value)
    }

    @Synchronized
    fun setVisite(id: String, visite: Boolean) {
        persist(_points.value.map { if (it.id == id) it.copy(visite = visite) else it })
    }

    @Synchronized
    fun updateNote(id: String, note: String) {
        persist(_points.value.map { if (it.id == id) it.copy(note = note) else it })
    }

    @Synchronized
    fun delete(id: String) {
        persist(_points.value.filterNot { it.id == id })
    }
}
