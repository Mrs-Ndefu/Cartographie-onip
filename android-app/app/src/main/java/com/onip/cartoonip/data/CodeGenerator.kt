package com.onip.cartoonip.data

import kotlin.random.Random

private const val CODE_LENGTH = 18
private const val CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

/** Même algorithme que l'app React (utils/idGenerator.ts) — code aléatoire, majuscules/chiffres. */
fun generateCodeMenage(): String =
    (1..CODE_LENGTH).map { CODE_CHARS[Random.nextInt(CODE_CHARS.length)] }.joinToString("")
