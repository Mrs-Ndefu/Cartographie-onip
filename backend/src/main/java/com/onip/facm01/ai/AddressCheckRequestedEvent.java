package com.onip.facm01.ai;

import java.util.UUID;

// Publié par HouseholdService juste après l'enregistrement d'un ménage "complet" (synchro
// terrain ou modification depuis le tableau de bord). Traité après le commit de la transaction
// (cf. AddressAiCheckService), pour que le thread qui appelle l'IA retrouve bien le ménage en
// base et ne bloque jamais la requête d'origine (synchro mobile notamment).
public record AddressCheckRequestedEvent(UUID householdId) {
}
