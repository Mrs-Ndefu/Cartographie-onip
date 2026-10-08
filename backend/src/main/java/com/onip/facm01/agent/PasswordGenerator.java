package com.onip.facm01.agent;

import java.security.SecureRandom;

// Mot de passe par défaut généré automatiquement à la création d'un compte depuis "Gérer les
// agents" (cf. AgentAdminController) : un mot de passe différent pour chaque personne, pas une
// valeur fixe partagée par tous les nouveaux comptes. Affiché une seule fois, dans le message de
// confirmation — il n'est pas stocké en clair (cf. AgentService.createAgent, qui le hache tout de
// suite), donc à communiquer à la personne concernée immédiatement.
public final class PasswordGenerator {

    // Sans 0/O, 1/I/l (ambigus à l'écran comme à l'oral).
    private static final String CHARS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int LENGTH = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
