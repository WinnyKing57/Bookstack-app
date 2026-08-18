# WinBook-Stack : BookStack Companion

**WinBook-Stack** est un client Android natif modern pour accéder et consulter vos instances [BookStack](https://www.bookstackapp.com/) auto-hébergées.

Développé par **WinnyKing** ([winnyking.cloud](https://winnyking.cloud)).

---

## 📱 Fonctionnalités

- **Multi-serveurs sécurisé** : Configurez et passez d'un serveur BookStack à un autre instantanément. Les identifiants (`Token ID` et `Token Secret`) sont chiffrés localement via **Android Keystore** et **EncryptedSharedPreferences**.
- **Tableau de bord** : Accès rapide aux livres, étagères, recherches, favoris et pages récemment consultées.
- **Consultation des Livres & Étagères** : Exploration sous forme de grilles et listes avec recherche locale, tri, pull-to-refresh et vue arborescente (Livres ➔ Chapitres ➔ Pages).
- **Lecteur de Page Hybride (Compose + WebView)** : Rendu HTML élégant avec support des thèmes Clair/Sombre, réglage dynamique de la taille de police, citations, tableaux et blocs de code.
- **Mode Hors-ligne & Téléchargement complet** : Téléchargez un livre entier pour une consultation 100% hors-ligne. Un bandeau clair indique lorsque les données proviennent du cache local.
- **Favoris & Historique** : Mettez vos pages en favoris et retrouvez votre historique de consultation isolé pour chaque serveur.
- **Recherche Globale API** : Recherche instantanée dans les livres, chapitres et pages de l'instance.
- **Paramètres personnalisés** : Thème (Système, Clair, Sombre), taille de texte, gestionnaire de cache et synchronisation manuelle.

---

## 🛠️ Stack Technique

- **Langage** : Kotlin
- **Interface Utilisateur** : Jetpack Compose + Material 3
- **Architecture** : MVVM (Clean Architecture en couches UI / ViewModel / UseCases / Repository / Data)
- **Injection de Dépendances** : Hilt / Dagger
- **Réseau** : Retrofit 2 + OkHttp 4 + Kotlinx Serialization
- **Base de données Locale** : Room DB
- **Stockage Sécurisé** : AndroidX Security Crypto (Android Keystore)
- **Préférences** : Jetpack DataStore Preferences
- **Chargement d'Images** : Coil Compose
- **SDK Min** : Android 8.0 (API 26)
- **SDK Cible** : Android 14 (API 34)

---

## 🔑 Créer un Token d'API sur BookStack

Pour connecter l'application à votre instance BookStack :

1. Connectez-vous à votre instance BookStack.
2. Allez dans votre **Profil** ➔ **Edit Profile** ➔ section **API Tokens**.
3. Cliquez sur **Add API Token**.
4. Donnez un nom (ex: `WinBook-Stack App`).
5. Copiez soigneusement le **Token ID** et le **Token Secret** générés.

---

## 🚀 Compilation et Installation

### Ouvrir le projet dans Android Studio
1. Ouvrez **Android Studio** (version Jellyfish ou plus récente recommandée).
2. Sélectionnez **Open** et choisissez le dossier du projet.
3. Laissez Gradle synchroniser les dépendances.

### Exécuter les tests unitaires
Dans un terminal à la racine du projet :
```bash
./gradlew test
```

### Générer l'APK Debug
```bash
./gradlew assembleDebug
```
L'APK généré se trouvera dans :
`app/build/outputs/apk/debug/app-debug.apk`

### Générer l'APK Release
```bash
./gradlew assembleRelease
```

---

## 👨‍💻 Développeur & Crédits

- **Développeur** : WinnyKing
- **Site Web** : [winnyking.cloud](https://winnyking.cloud)
- **Projet** : WinBook-Stack (BookStack Companion)
