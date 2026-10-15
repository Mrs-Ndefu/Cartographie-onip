package com.onip.cartoonip.data.model

/**
 * Un point repéré sur le terrain, à visiter plus tard pour l'adressage complet — juste une
 * position et une note facultative (ex. "porte fermée", "revenir le matin"), pas de nom de chef
 * ni d'adresse saisie. Sert à la pré-reconnaissance d'une zone avant la tournée d'adressage
 * complète (cf. ReperageScreen) : purement local, jamais synchronisé au serveur (seul un foyer
 * adressé l'est, via CapturedHousehold).
 */
data class ReperagePoint(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val locationPrecision: Double?,
    val note: String = "",
    val visite: Boolean = false,
    val createdAt: String,
)
