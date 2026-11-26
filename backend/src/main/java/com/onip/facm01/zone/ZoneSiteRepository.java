package com.onip.facm01.zone;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ZoneSiteRepository extends JpaRepository<ZoneSite, UUID> {

    // zoneFilter : 'all' = tous, 'none' = non affectés, 'zone' = ceux de la zone :zoneId.
    @Query("""
            SELECT s FROM ZoneSite s LEFT JOIN s.zone z
            WHERE (:zoneFilter = 'all'
                   OR (:zoneFilter = 'none' AND z IS NULL)
                   OR (:zoneFilter = 'zone' AND z.id = :zoneId))
              AND (:search = ''
                   OR LOWER(s.nom) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(s.adresse, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(s.ville, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(s.territoire, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:commune = '' OR UPPER(COALESCE(s.commune, '')) = UPPER(:commune))
              AND (:quartier = '' OR UPPER(COALESCE(s.quartier, '')) = UPPER(:quartier))
            ORDER BY s.province, s.territoire, s.nom
            """)
    Page<ZoneSite> search(@Param("zoneFilter") String zoneFilter,
                          @Param("zoneId") UUID zoneId,
                          @Param("search") String search,
                          @Param("commune") String commune,
                          @Param("quartier") String quartier,
                          Pageable pageable);

    // Valeurs des filtres Commune / Quartier de la page Sites (le quartier restreint à la commune
    // choisie, "" = toutes).
    @Query("SELECT DISTINCT UPPER(s.commune) FROM ZoneSite s WHERE s.commune IS NOT NULL AND s.commune <> '' ORDER BY UPPER(s.commune)")
    List<String> distinctCommunes();

    @Query("""
            SELECT DISTINCT UPPER(s.quartier) FROM ZoneSite s
            WHERE s.quartier IS NOT NULL AND s.quartier <> ''
              AND (:commune = '' OR UPPER(COALESCE(s.commune, '')) = UPPER(:commune))
            ORDER BY UPPER(s.quartier)
            """)
    List<String> distinctQuartiers(@Param("commune") String commune);

    // [zoneId, nombre] pour chaque zone ayant au moins un site.
    @Query("SELECT s.zone.id, COUNT(s) FROM ZoneSite s WHERE s.zone IS NOT NULL GROUP BY s.zone.id")
    List<Object[]> countByZone();

    long countByZoneIsNull();

    List<ZoneSite> findByZoneIsNull();

    List<ZoneSite> findByZone_Id(UUID zoneId);
}
