package com.onip.facm01.ai;

import com.onip.facm01.household.AddressEmbeddable;
import com.onip.facm01.household.Household;
import com.onip.facm01.household.HouseholdRepository;
import com.onip.facm01.household.HouseholdStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddressAiCheckServiceTest {

    private static Household householdWithAddress(String quartier, String rue) {
        Household household = new Household(UUID.randomUUID());
        household.setStatus(HouseholdStatus.COMPLET);
        household.setAddress(new AddressEmbeddable(
                "KINSHASA", "KINSHASA", "GOMBE", quartier, rue, "12", null, null, null));
        return household;
    }

    @Test
    void flagsHouseholdAsAVerifierWhenAiJudgesAddressImplausible() {
        HouseholdRepository repository = mock(HouseholdRepository.class);
        AnthropicClient client = mock(AnthropicClient.class);
        Household household = householdWithAddress("qsdqsd", "aaaaa");
        when(repository.findById(household.getId())).thenReturn(Optional.of(household));
        when(client.hasApiKey()).thenReturn(true);
        when(client.ask(any())).thenReturn("NON");

        AddressAiCheckService service = new AddressAiCheckService(repository, client, true);
        service.onAddressCheckRequested(new AddressCheckRequestedEvent(household.getId()));

        assertEquals(HouseholdStatus.A_VERIFIER, household.getStatus());
        assertNotNull(household.getAddressCheckedSignature());
    }

    @Test
    void leavesHouseholdCompletWhenAiJudgesAddressPlausible() {
        HouseholdRepository repository = mock(HouseholdRepository.class);
        AnthropicClient client = mock(AnthropicClient.class);
        Household household = householdWithAddress("MATONGE", "AVENUE DE LA PAIX");
        when(repository.findById(household.getId())).thenReturn(Optional.of(household));
        when(client.hasApiKey()).thenReturn(true);
        when(client.ask(any())).thenReturn("OUI");

        AddressAiCheckService service = new AddressAiCheckService(repository, client, true);
        service.onAddressCheckRequested(new AddressCheckRequestedEvent(household.getId()));

        assertEquals(HouseholdStatus.COMPLET, household.getStatus());
        assertNotNull(household.getAddressCheckedSignature());
    }

    @Test
    void doesNotCallAiAgainWhenAddressTextUnchangedSinceLastCheck() {
        HouseholdRepository repository = mock(HouseholdRepository.class);
        AnthropicClient client = mock(AnthropicClient.class);
        Household household = householdWithAddress("qsdqsd", "aaaaa");
        when(repository.findById(household.getId())).thenReturn(Optional.of(household));
        when(client.hasApiKey()).thenReturn(true);
        when(client.ask(any())).thenReturn("NON");

        AddressAiCheckService service = new AddressAiCheckService(repository, client, true);
        service.onAddressCheckRequested(new AddressCheckRequestedEvent(household.getId()));
        // Un ADMIN valide malgré l'alerte (cf. HouseholdService.validate) : le texte de l'adresse
        // n'a pas changé, une resynchronisation ne doit pas annuler cette décision.
        household.setStatus(HouseholdStatus.COMPLET);
        service.onAddressCheckRequested(new AddressCheckRequestedEvent(household.getId()));

        // Un seul appel IA au total (celui de la première vérification) : la deuxième synchro,
        // adresse inchangée, ne rappelle pas l'IA.
        verify(client, times(1)).ask(any());
        assertEquals(HouseholdStatus.COMPLET, household.getStatus());
    }

    @Test
    void doesNothingWithoutApiKey() {
        HouseholdRepository repository = mock(HouseholdRepository.class);
        AnthropicClient client = mock(AnthropicClient.class);
        when(client.hasApiKey()).thenReturn(false);

        AddressAiCheckService service = new AddressAiCheckService(repository, client, true);
        service.onAddressCheckRequested(new AddressCheckRequestedEvent(UUID.randomUUID()));

        verify(repository, never()).findById(any());
    }
}
