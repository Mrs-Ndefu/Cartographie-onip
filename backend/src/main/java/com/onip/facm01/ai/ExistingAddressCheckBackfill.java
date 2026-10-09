package com.onip.facm01.ai;

import com.onip.facm01.household.HouseholdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

// Au démarrage : les ménages "complet" enregistrés avant l'ajout de la vérification d'adresse
// (cf. AddressAiCheckService) sont rattrapés — leur adresse n'a jamais été jugée. Sans effet une
// fois tous les ménages existants passés une première fois (même principe que ProvinceBackfill).
// Chaque vérification se fait en tâche de fond (AddressAiCheckService.onAddressCheckRequested
// est @Async) : ce démarrage n'attend donc pas la fin du rattrapage pour continuer.
@Component
public class ExistingAddressCheckBackfill implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ExistingAddressCheckBackfill.class);

    private final HouseholdService householdService;
    private final AddressAiCheckService addressAiCheckService;

    public ExistingAddressCheckBackfill(HouseholdService householdService, AddressAiCheckService addressAiCheckService) {
        this.householdService = householdService;
        this.addressAiCheckService = addressAiCheckService;
    }

    @Override
    public void run(String... args) {
        List<UUID> ids = householdService.householdIdsPendingAddressCheck();
        if (ids.isEmpty()) {
            return;
        }
        log.info("Vérification d'adresse programmée pour {} ménage(s) existant(s) jamais vérifié(s)", ids.size());
        ids.forEach(id -> addressAiCheckService.onAddressCheckRequested(new AddressCheckRequestedEvent(id)));
    }
}
