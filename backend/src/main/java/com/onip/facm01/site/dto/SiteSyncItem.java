package com.onip.facm01.site.dto;

import com.onip.facm01.site.AccesRoutier;
import com.onip.facm01.site.AvisGlobal;
import com.onip.facm01.site.ConnectiviteInternet;
import com.onip.facm01.site.EtatBatiment;
import com.onip.facm01.site.OptionOccupation;
import com.onip.facm01.site.ProprieteStatut;
import com.onip.facm01.site.StabiliteSnel;
import com.onip.facm01.site.TypeInfrastructure;
import com.onip.facm01.site.TypeSite;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// Rien d'autre que l'id/les dates n'est obligatoire : une fiche de repérage peut être synchronisée
// partiellement remplie et complétée plus tard (même principe que HouseholdSyncItem).
public record SiteSyncItem(
        @NotNull UUID id,
        String province,
        String ville,
        String commune,
        String quartierGroupement,
        String villageAvenueRue,
        String nomSite,
        String numeroTelephone,
        String serieTelephoneGps,
        Double latitude,
        Double longitude,
        Double altitude,
        Double gpsPrecision,
        TypeSite typeSite,
        String typeSiteAutre,
        ProprieteStatut proprieteStatut,
        OptionOccupation optionOccupation,
        Boolean disponibiliteImmediate,
        String delaiEstime,
        String proprietaireNomContact,
        TypeInfrastructure typeInfrastructure,
        String typeInfrastructureAutre,
        Double superficieBatimentM2,
        Double superficieTerrainM2,
        Integer nombrePieces,
        EtatBatiment etatBatiment,
        String etatBatimentObservations,
        AccesRoutier accesRoutier,
        Boolean raccordeSnel,
        StabiliteSnel stabiliteSnel,
        Boolean groupeElectrogeneSolaire,
        Boolean nonElectrifie,
        Boolean regideso,
        Boolean sanitaires,
        ConnectiviteInternet connectiviteInternet,
        String reseauxDisponibles,
        Boolean clotureSecurise,
        Boolean proximitePolice,
        Integer distancePoliceMetres,
        AvisGlobal avisGlobal,
        String travauxAmenagement,
        String motifDefavorable,
        String observationsParticulieres,
        String nomSignatureEnqueteur,
        LocalDate dateEvaluation,
        @NotNull Instant createdAt,
        @NotNull Instant updatedAt) {
}
