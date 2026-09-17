package com.onip.facm01.household.dto;

import com.onip.facm01.household.Sexe;

public record PersonDto(String nom, String postnom, String prenom, String dateNaissance, Sexe sexe) {
}
