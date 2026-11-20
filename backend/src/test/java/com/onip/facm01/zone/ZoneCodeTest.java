package com.onip.facm01.zone;

import com.onip.facm01.agent.AgentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Code des zones : numéro de la province (01 = Kinshasa) + tiret + numéro dans la province sur 4
// chiffres, le premier libre étant repris.
class ZoneCodeTest {

    private ZoneService serviceWith(String province, String... existingCodes) {
        ZoneRepository repository = mock(ZoneRepository.class);
        List<Zone> zones = java.util.Arrays.stream(existingCodes)
                .map(code -> new Zone(UUID.randomUUID(), code, province, "VILLE", List.of("COMMUNE")))
                .toList();
        when(repository.findByProvince(province)).thenReturn(zones);
        return new ZoneService(repository, mock(AgentRepository.class), mock(ZoneSiteService.class));
    }

    @Test
    void firstZoneOfAProvinceGetsNumberOne() {
        assertThat(serviceWith("KINSHASA").nextCode("KINSHASA")).isEqualTo("01-0001");
        assertThat(serviceWith("BAS-UELE").nextCode("BAS-UELE")).isEqualTo("02-0001");
    }

    @Test
    void numberingIsPerProvinceAndReusesFreedNumbers() {
        assertThat(serviceWith("KINSHASA", "01-0001", "01-0002").nextCode("KINSHASA")).isEqualTo("01-0003");
        assertThat(serviceWith("KINSHASA", "01-0001", "01-0003").nextCode("KINSHASA")).isEqualTo("01-0002");
        // Un ancien code (A1) ne compte pas : il sera renuméroté au démarrage.
        assertThat(serviceWith("KINSHASA", "A1").nextCode("KINSHASA")).isEqualTo("01-0001");
    }

    @Test
    void unknownProvinceHasNoCode() {
        assertThat(serviceWith("PAS UNE PROVINCE").nextCode("PAS UNE PROVINCE")).isNull();
    }
}
