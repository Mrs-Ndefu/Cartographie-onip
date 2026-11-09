package com.onip.facm01.zone;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneSiteMatchTest {

    private final Zone matadi = new Zone(UUID.randomUUID(), "K1", "KONGO-CENTRAL", "MATADI", List.of("MVUZI", "NZANZA"));
    private final Zone gombe = new Zone(UUID.randomUUID(), "A1", "KINSHASA", "KINSHASA", List.of("GOMBE", "KINSHASA", "LINGWALA"));
    private final Zone limete = new Zone(UUID.randomUUID(), "A2", "KINSHASA", "KINSHASA", List.of("LIMETE"));
    private final List<Zone> zones = List.of(matadi, gombe, limete);

    private static ZoneSite site(String adresse, String province, String ville, String territoire) {
        return new ZoneSite(UUID.randomUUID(), "Site", adresse, province, ville, territoire);
    }

    @Test
    void matchesProvinceAndTerritoireIgnoringCaseAndAccents() {
        assertThat(ZoneSiteService.matchZone(site(null, "Kongo-Central", "Matadi", "matadi"), zones)).isEqualTo(matadi);
    }

    @Test
    void fallsBackToVilleWhenTerritoireDoesNotMatch() {
        assertThat(ZoneSiteService.matchZone(site(null, "KONGO-CENTRAL", "MATADI", "SEKE-BANZA"), zones)).isEqualTo(matadi);
    }

    @Test
    void picksKinshasaZoneByCommuneWhenSeveralZonesShareTheCity() {
        assertThat(ZoneSiteService.matchZone(site("12, av. des Huileries, Limete", "Kinshasa", "Kinshasa", "Kinshasa"), zones))
                .isEqualTo(limete);
        assertThat(ZoneSiteService.matchZone(site(null, "Kinshasa", "Kinshasa", "Gombe"), zones)).isEqualTo(gombe);
        // La commune KINSHASA (homonyme de la ville) ne doit pas faire hésiter avec A1.
        assertThat(ZoneSiteService.matchZone(site("Av. X, Limete, Kinshasa", "Kinshasa", "Kinshasa", "Kinshasa"), zones))
                .isEqualTo(limete);
    }

    @Test
    void staysUnassignedWhenAmbiguousOrUnknown() {
        assertThat(ZoneSiteService.matchZone(site("Av. 1", "Kinshasa", "Kinshasa", "Kinshasa"), zones)).isNull();
        assertThat(ZoneSiteService.matchZone(site(null, "Nord-Kivu", "Goma", "Goma"), zones)).isNull();
        assertThat(ZoneSiteService.matchZone(site(null, null, null, null), zones)).isNull();
    }

    @Test
    void deducesCommuneOfSiteWithinItsZone() {
        assertThat(ZoneSiteService.communeOf(site("Q. Mvuzi", "Kongo-Central", "Matadi", "Matadi"), matadi)).isEqualTo("MVUZI");
        assertThat(ZoneSiteService.communeOf(site("Av. du Port", "Kongo-Central", "Matadi", "Matadi"), matadi)).isNull();
        assertThat(ZoneSiteService.communeOf(site("Av. de la Paix", "Kinshasa", "Kinshasa", "Gombe"), gombe)).isEqualTo("GOMBE");
        assertThat(ZoneSiteService.communeOf(site("Av. Kabinda, commune de Kinshasa", "Kinshasa", "Kinshasa", "Kinshasa"), gombe))
                .isEqualTo("KINSHASA");
        assertThat(ZoneSiteService.communeOf(site("Av. X", "Kinshasa", "Kinshasa", "Limete"), limete)).isEqualTo("LIMETE");
    }
}
