package com.onip.facm01.bootstrap;

import com.onip.facm01.zone.ZoneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Au démarrage : les zones sans code, ou avec un code d'un ancien format, reçoivent un code
// lettre de province + numéro (A1, A2, B1...), dans l'ordre de création. Sans effet ensuite.
@Component
public class ZoneCodeBackfill implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ZoneCodeBackfill.class);

    private final ZoneService zoneService;

    public ZoneCodeBackfill(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @Override
    public void run(String... args) {
        int updated = zoneService.backfillCodes();
        if (updated > 0) {
            log.info("Code attribué à {} zone(s) existante(s)", updated);
        }
    }
}
