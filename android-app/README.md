# Onip Dashboard (Android natif)

Application Android native (Kotlin + Jetpack Compose) pour les administrateurs du backend
FACM01 (`../backend`). Elle ne duplique pas la saisie terrain — celle-ci reste dans l'app React
(`../src`), utilisée par les agents. Cette app est l'équivalent mobile du dashboard admin
(`/dashboard` en Thymeleaf) : consultation des ménages enregistrés, statistiques, gestion des
comptes agents. Accès réservé aux comptes `ADMIN`.

## Prérequis

- Android SDK (déjà présent sur cette machine : `C:\Users\DELL\Desktop\AndroidSKD`, référencé
  par `local.properties`, qui n'est pas versionné — à recréer sur une autre machine avec
  `sdk.dir=<chemin vers le SDK>`).
- JDK 17.

## Configuration du backend à contacter

L'URL du backend n'est pas codée en dur : elle se saisit sur l'écran de connexion (ex.
`http://192.168.1.10:8080`), puis est mémorisée de façon chiffrée sur l'appareil
(EncryptedSharedPreferences) pour les connexions suivantes.

Le trafic HTTP en clair est autorisé (`network_security_config.xml`) car le backend tourne en
HTTP simple sur le réseau local — pas de certificat HTTPS en usage interne. Le téléphone doit
être sur le même réseau (Wi-Fi local) que le serveur backend, ou le serveur doit être joignable
depuis l'extérieur (IP publique / VPN) si l'app est utilisée hors du réseau local.

## Build

```bash
./gradlew assembleDebug      # APK de debug, non signé pour la release
./gradlew assembleRelease    # APK signé, installable directement sur un téléphone
```

Les APK générés se trouvent dans `app/build/outputs/apk/debug/` et
`app/build/outputs/apk/release/`.

## Signature de l'APK release

`keystore.properties` (non versionné) pointe vers `keystore/carto-onip-release.jks` (non
versionné non plus). Les deux sont générés localement — **à sauvegarder ailleurs** : sans eux,
impossible de publier une mise à jour signée avec la même clé (il faudrait redésinstaller l'app
sur chaque téléphone pour réinstaller une version signée différemment).

Pour recréer un keystore sur une autre machine :

```bash
keytool -genkeypair -v -keystore keystore/carto-onip-release.jks -alias carto-onip \
  -keyalg RSA -keysize 2048 -validity 10000
```

puis renseigner `keystore.properties` avec le chemin, l'alias et les mots de passe choisis.

## Installer sur le téléphone

- Par câble USB (débogage USB activé) : `adb install -r app/build/outputs/apk/release/app-release.apk`
- Ou transférer le fichier `.apk` sur le téléphone (Bluetooth, câble, lien) et l'installer
  directement (autoriser "sources inconnues" pour l'app utilisée).

## Structure

- `data/model` — classes Kotlin miroir des DTO backend (`HouseholdDto`, `AgentDto`, etc.).
- `data/network` — Retrofit + OkHttp, JWT attaché automatiquement aux requêtes une fois connecté.
- `data/SessionManager.kt` — session chiffrée sur l'appareil (URL backend, token, identité).
- `ui/` — écrans Compose : login, tableau de bord, ménages (liste + détail), agents.
