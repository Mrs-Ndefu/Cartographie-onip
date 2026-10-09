package com.onip.facm01.ai;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// Détecteur de plausibilité d'adresse sans appel réseau ni clé API : utilisé par
// AddressAiCheckService quand aucune clé Anthropic n'est configurée (cf. AnthropicClient),
// c'est-à-dire par défaut. Volontairement simple et rapide (aucune dépendance externe, aucun
// coût) : il repère les cas flagrants de texte saisi au hasard (lettres répétées, texte clavier,
// absence totale de voyelle, mots de test), pas les adresses juste mal orthographiées,
// abrégées ou inhabituelles — ce qui reste le rôle d'une relecture humaine (Valider/Rejeter),
// pas de ce contrôle.
final class AddressPlausibilityHeuristic {

    private AddressPlausibilityHeuristic() {
    }

    // Suites de touches voisines tapées sans réfléchir (saisies de test ou bâclées) — les cas les
    // plus fréquents, en AZERTY comme en QWERTY.
    private static final List<String> KEYBOARD_MASH = List.of(
            "qsdqsd", "qwerty", "azerty", "asdfgh", "asdf", "zxcv", "qwert", "poiuy", "lkjhg", "mlkj");

    private static final List<String> PLACEHOLDER_WORDS = List.of(
            "test", "essai", "exemple", "bidon", "xxx", "aaa", "nimportequoi", "rien", "neant",
            "inconnu", "asd", "lorem", "nimporte");

    private static final Pattern REPEATED_CHAR = Pattern.compile("^(.)\\1*$");
    private static final Pattern REPEATED_PAIR = Pattern.compile("^(..)\\1+.?$");
    private static final Pattern HAS_VOWEL = Pattern.compile("[aeiouyAEIOUY]");

    // Numéro volontairement exclu : court et numérique par nature ("12", "4B"), ces règles n'ont
    // pas de sens pour lui.
    static boolean isImplausible(String quartier, String rue, String immeuble) {
        return isSuspicious(quartier) || isSuspicious(rue) || isSuspicious(immeuble);
    }

    private static boolean isSuspicious(String raw) {
        if (raw == null) {
            return false;
        }
        // Lettres accentuées conservées (noms congolais/français), le reste (chiffres,
        // ponctuation, espaces) retiré avant d'appliquer les règles ci-dessous.
        String normalized = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-zàâäéèêëîïôöùûüç]", "");
        if (normalized.length() < 3) {
            // Vide, ou trop court pour juger (une initiale ou un sigle n'est pas du charabia) :
            // pas le rôle de ce contrôle de forcer un champ à être rempli.
            return false;
        }
        if (REPEATED_CHAR.matcher(normalized).matches()) {
            return true;
        }
        if (REPEATED_PAIR.matcher(normalized).matches()) {
            return true;
        }
        if (normalized.length() >= 4 && !HAS_VOWEL.matcher(normalized).find()) {
            return true;
        }
        for (String mash : KEYBOARD_MASH) {
            if (normalized.contains(mash)) {
                return true;
            }
        }
        return PLACEHOLDER_WORDS.contains(normalized);
    }
}
