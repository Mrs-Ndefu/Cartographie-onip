package com.onip.cartoonip.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.onip.cartoonip.data.model.CapturedHousehold
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stockage local des ménages capturés — un simple fichier JSON dans le stockage privé de l'app
 * (pas de base de données embarquée : le volume attendu par agent et par jour est modeste, et ça
 * évite d'ajouter Room/KSP au projet). Les photos elles-mêmes sont des fichiers séparés
 * (voir PhotoStore) ; ce fichier ne référence que leur chemin.
 */
class CaptureStore(context: Context) {

    private val file = File(context.filesDir, "captures.json")
    private val gson = Gson()
    private val listType = object : TypeToken<List<CapturedHousehold>>() {}.type

    private val _households = MutableStateFlow(load())
    val households: StateFlow<List<CapturedHousehold>> = _households.asStateFlow()

    @Synchronized
    private fun load(): List<CapturedHousehold> {
        if (!file.exists()) return emptyList()
        return try {
            val raw = gson.fromJson<List<CapturedHousehold>>(file.readText(), listType) ?: emptyList()
            raw.map(::sanitize)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Gson ne respecte pas les valeurs par défaut de Kotlin à la désérialisation (il alloue
    // l'objet sans passer par le constructeur) : un enregistrement écrit AVANT l'ajout d'un
    // nouveau champ (ex. "membres") le charge à null malgré le type non-nullable déclaré, ce qui
    // fait planter le premier `.isEmpty()`/`.isNotEmpty()` venu. On répare ça une fois ici plutôt
    // que de semer des `?: emptyList()` partout où le champ est lu.
    @Suppress("SENSELESS_COMPARISON")
    private fun sanitize(h: CapturedHousehold): CapturedHousehold {
        var fixed = h
        if (fixed.membres == null) fixed = fixed.copy(membres = emptyList())
        if (fixed.photoPaths == null) fixed = fixed.copy(photoPaths = emptyList())
        if (fixed.etage == null) fixed = fixed.copy(etage = "")
        return fixed
    }

    @Synchronized
    private fun persist(list: List<CapturedHousehold>) {
        file.writeText(gson.toJson(list))
        _households.value = list
    }

    @Synchronized
    fun upsert(household: CapturedHousehold) {
        val current = _households.value.toMutableList()
        val index = current.indexOfFirst { it.id == household.id }
        if (index >= 0) current[index] = household else current.add(0, household)
        persist(current)
    }

    @Synchronized
    fun delete(id: String) {
        persist(_households.value.filterNot { it.id == id })
    }

    fun get(id: String): CapturedHousehold? = _households.value.firstOrNull { it.id == id }
}
