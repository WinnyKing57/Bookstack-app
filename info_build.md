# Guide de Compilation et Build APK sur Linux — BookStack Companion

Ce document détaille la procédure complète pour compiler et générer les fichiers **APK** (Debug et Release) ou **AAB** (Android App Bundle) pour l'application Android **BookStack Companion** sous un environnement Linux.

---

## 1. Prérequis Système & Environnement

Pour compiler l'application sur Linux sans erreurs, assurez-vous d'avoir les éléments suivants installés et configurés :

### A. Java Development Kit (JDK 21)
L'application requiert Java 17 ou **JDK 21**.
Définissez la variable d'environnement `JAVA_HOME` pour pointer vers votre installation JDK 21 :
```bash
export JAVA_HOME=/path/to/your/jdk-21
```
*Alternativement, vous pouvez configurer `org.gradle.java.home` dans `gradle.properties` :*
```properties
org.gradle.java.home=/path/to/your/jdk-21
```

### B. Permissions d'Exécution sur Gradle Wrapper
Assurez-vous que le script `./gradlew` possède les droits d'exécution :
```bash
chmod +x gradlew
```

---

## 2. Emplacement du Projet

Naviguez vers le dossier racine du projet Android :
```bash
cd /path/to/Bookstack-app
```

---

## 3. Commandes de Compilation

### A. Nettoyage du projet
Avant tout build important ou changement de version, nettoyez les caches de build :
```bash
./gradlew clean
```

### B. Générer l'APK Debug (Pour tests rapides)
Pour générer une version de développement immédiatement installable sans signature de production :
```bash
./gradlew assembleDebug
```
📍 **Fichier généré :**
```text
app/build/outputs/apk/debug/app-debug.apk
```

### C. Générer l'APK Release (Version finale)
Pour compiler la version optimisée avec ProGuard / R8 :
```bash
./gradlew assembleRelease
```
📍 **Fichier généré :**
```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

### D. Générer le Bundle Release (.aab)
Pour générer un fichier AAB destiné à la publication sur Google Play :
```bash
./gradlew bundleRelease
```
📍 **Fichier généré :**
```text
app/build/outputs/bundle/release/app-release.aab
```

---

## 4. Signature de l'APK Release pour la Production

Par défaut, `./gradlew assembleRelease` génère un APK **non signé** (`app-release-unsigned.apk`). Pour pouvoir l'installer sur un appareil Android en mode production, suivez ces étapes sous Linux :

### Étape 1 : Créer une clé de signature (Keystore) *(Si non existante)*
```bash
keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias bookstack-companion-alias
```

### Étape 2 : Aligner l'APK avec `zipalign`
`zipalign` est inclus dans les Android SDK Build-Tools.
```bash
zipalign -v -p 4 app/build/outputs/apk/release/app-release-unsigned.apk app-release-aligned.apk
```

### Étape 3 : Signer l'APK avec `apksigner`
```bash
apksigner sign --ks release-key.jks --out app-release-v1.1.0-signed.apk app-release-aligned.apk
```

### Étape 4 : Vérifier la signature
```bash
apksigner verify app-release-v1.1.0-signed.apk
```

---

## 5. Installation de l'APK sur un Appareil Android (via ADB)

1. Connectez votre smartphone Android en USB avec le **Débogage USB** activé.
2. Vérifiez la détection du périphérique sous Linux :
   ```bash
   adb devices
   ```
3. Installez l'APK directement :
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 6. Résolution des Problèmes Fréquents (Troubleshooting)

| Erreur | Cause probable | Solution |
| :--- | :--- | :--- |
| `bash: ./gradlew: Permission denied` | Permissions manquantes sur `gradlew`. | Exécuter `chmod +x gradlew` |
| `jlink not found` ou `Java home supplied is invalid` | Le chemin du JDK dans `gradle.properties` est incorrect sur la machine hôte. | Mettre à jour `org.gradle.java.home` dans `gradle.properties` avec le bon chemin JDK 21. |
| `OutOfMemoryError` de Gradle | Mémoire insuffisante allouée au daemon Gradle. | Ajuster `org.gradle.jvmargs=-Xmx2048m` dans `gradle.properties` ou exécuter `./gradlew --stop`. |
