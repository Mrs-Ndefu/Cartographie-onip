package com.onip.facm01.household.dto;

import com.onip.facm01.household.Household;
import com.onip.facm01.household.HouseholdMember;
import com.onip.facm01.household.HouseholdStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record HouseholdDto(
        UUID id,
        String codeMenage,
        Integer nombreMembres,
        PersonDto chef,
        List<PersonDto> membres,
        AddressDto address,
        GeoLocationDto location,
        FormMetaDto meta,
        HouseholdStatus status,
        String agentUsername,
        Instant createdAt,
        Instant updatedAt,
        Instant syncedAt) {

    public static HouseholdDto from(Household household) {
        PersonDto chef = household.getMembers().stream()
                .filter(HouseholdMember::isChef)
                .findFirst()
                .map(HouseholdDto::toPersonDto)
                .orElse(null);

        List<PersonDto> membres = household.getMembers().stream()
                .filter(m -> !m.isChef())
                .map(HouseholdDto::toPersonDto)
                .toList();

        var address = household.getAddress();
        AddressDto addressDto = address == null ? null
                : new AddressDto(address.getVille(), address.getCommune(), address.getQuartier(),
                        address.getRue(), address.getNumero(), address.getImmeuble());

        var location = household.getLocation();
        GeoLocationDto locationDto = (location == null || !location.isPresent()) ? null
                : new GeoLocationDto(location.getLatitude(), location.getLongitude(),
                        location.getPrecision(), location.getSaisieManuelle());

        var meta = household.getMeta();
        FormMetaDto metaDto = meta == null ? null
                : new FormMetaDto(meta.getFaitA(), meta.getDateEncodage(), meta.getNombreFiches(),
                        meta.getAgentCarthographe(), meta.getRenseignant());

        return new HouseholdDto(
                household.getId(),
                household.getCodeMenage(),
                household.getNombreMembresDeclare(),
                chef,
                membres,
                addressDto,
                locationDto,
                metaDto,
                household.getStatus(),
                Optional.ofNullable(household.getAgent()).map(a -> a.getUsername()).orElse(null),
                household.getCreatedAt(),
                household.getUpdatedAt(),
                household.getSyncedAt());
    }

    private static PersonDto toPersonDto(HouseholdMember member) {
        return new PersonDto(member.getNom(), member.getPostnom(), member.getPrenom(),
                member.getDateNaissance(), member.getSexe(), member.getRelation());
    }
}
