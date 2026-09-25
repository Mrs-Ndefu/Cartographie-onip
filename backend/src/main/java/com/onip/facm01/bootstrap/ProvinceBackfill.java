package com.onip.facm01.bootstrap;

import com.onip.facm01.household.HouseholdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Au démarrage : les ménages enregistrés avant l'ajout du champ province reçoivent la province
// déduite de leur ville. Sans effet une fois tous les ménages complétés.
@Component
public class ProvinceBackfill implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProvinceBackfill.class);

    private final HouseholdService householdService;

    public ProvinceBackfill(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @Override
    public void run(String... args) {
        int updated = householdService.backfillProvinces();
        if (updated > 0) {
            log.info("Province déduite de la ville pour {} ménage(s) existant(s)", updated);
        }
    }
}
