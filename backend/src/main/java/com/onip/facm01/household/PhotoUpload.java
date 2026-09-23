package com.onip.facm01.household;

/** Un fichier photo reçu, avant persistance — découplé de MultipartFile pour garder HouseholdService indépendant de la couche web. */
public record PhotoUpload(byte[] bytes, String contentType) {
}
