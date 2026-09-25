package com.onip.facm01.household.dto;

public record AddressDto(
        String province, String ville, String commune, String quartier, String rue, String numero, String immeuble, String etage) {
}
