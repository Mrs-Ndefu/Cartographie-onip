package com.onip.cartoonip.data

import java.security.MessageDigest
import java.util.Locale

private const val CODE_LENGTH = 6
private const val CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

/**
 * Code déterministe dérivé du GPS + nom du chef de foyer + adresse (commune/quartier) : deux
 * foyers saisis au même endroit, pour le même chef et la même adresse, obtiennent exactement le
 * même code (utile pour repérer un doublon de saisie) — contrairement à un code purement
 * aléatoire. La position est arrondie à 4 décimales (~11 m) pour ne pas faire varier le code au
 * bruit naturel du capteur GPS entre deux relevés au même endroit.
 *
 * Un hash (SHA-256) de ces champs est réencodé sur l'alphabet majuscules/chiffres pour garder
 * exactement CODE_LENGTH caractères, précédé d'un préfixe géographique lisible
 * "Ville+Commune+Province" (ex. "KG01-" pour Gombe, Kinshasa) — cf. [codePrefix]. Le préfixe ne
 * fait pas partie du hash : seuls GPS/chef/commune/quartier déterminent la partie après le tiret.
 */
fun generateCodeMenage(
    latitude: Double,
    longitude: Double,
    chefNom: String,
    province: String,
    ville: String,
    commune: String,
    quartier: String,
): String {
    val input = buildString {
        append(String.format(Locale.US, "%.4f", latitude))
        append('|')
        append(String.format(Locale.US, "%.4f", longitude))
        append('|')
        append(chefNom.trim().uppercase())
        append('|')
        append(commune.trim().uppercase())
        append('|')
        append(quartier.trim().uppercase())
    }
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    val hash = (0 until CODE_LENGTH)
        .map { CODE_CHARS[(digest[it].toInt() and 0xFF) % CODE_CHARS.length] }
        .joinToString("")
    return "${codePrefix(province, ville, commune)}-$hash"
}

/**
 * Préfixe géographique du code ménage : 1ère lettre de la ville + 1ère lettre de la commune +
 * numéro de la province sur 2 chiffres (cf. [provinceCode] — 01 = Kinshasa, puis les 25 autres
 * par ordre alphabétique). "X" remplace une initiale manquante (en pratique jamais déclenché ici
 * puisque [generateCodeMenage] n'est appelée qu'une fois l'adresse renseignée, cf.
 * CaptureUiState.canGenerateCode) — gardé par cohérence avec l'équivalent web (idGenerator.ts),
 * où cette garde n'existe pas.
 */
private fun codePrefix(province: String, ville: String, commune: String): String {
    val villeInitial = ville.trim().firstOrNull()?.uppercaseChar() ?: 'X'
    val communeInitial = commune.trim().firstOrNull()?.uppercaseChar() ?: 'X'
    return "$villeInitial$communeInitial${provinceCode(province)}"
}
