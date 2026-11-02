package com.onip.facm01.site;

// Décision de l'ADMIN/SUPERVISEUR sur une fiche de repérage, depuis le tableau de bord —
// indépendante de l'avis global de l'équipe de terrain (section VII), qui reste celui saisi sur
// la tablette. Une fiche rejetée est aussi retirée (cf. SiteService.reject), comme un ménage.
public enum SiteValidation {
    EN_ATTENTE,
    VALIDE,
    REJETE;

    public String label() {
        return switch (this) {
            case EN_ATTENTE -> "En attente";
            case VALIDE -> "Validée";
            case REJETE -> "Rejetée";
        };
    }
}
