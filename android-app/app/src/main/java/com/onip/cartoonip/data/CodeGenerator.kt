package com.onip.cartoonip.data

import java.security.MessageDigest
import java.util.Locale

private const val CODE_LENGTH = 18
private const val CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

/**
 * Code déterministe dérivé du GPS + nom du chef de ménage + adresse (commune/quartier) : deux
 * ménages saisis au même endroit, pour le même chef et la même adresse, obtiennent exactement le
 * même code (utile pour repérer un doublon de saisie) — contrairement à un code purement
 * aléatoire. La position est arrondie à 4 décimales (~11 m) pour ne pas faire varier le code au
 * bruit naturel du capteur GPS entre deux relevés au même endroit.
 *
 * Un hash (SHA-256) de ces champs est réencodé sur l'alphabet majuscules/chiffres pour garder
 * exactement CODE_LENGTH caractères (contrainte de la colonne code_menage côté serveur).
 */
fun generateCodeMenage(latitude: Double, longitude: Double, chefNom: String, commune: String, quartier: String): String {
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
    return (0 until CODE_LENGTH)
        .map { CODE_CHARS[(digest[it].toInt() and 0xFF) % CODE_CHARS.length] }
        .joinToString("")
}
