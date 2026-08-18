# 📋 Feuille de Route & Tâches à Faire (BookStack Companion v1.2.0+)

## 🎯 Prochaines Étapes Immédiates (Release v1.1.0)
- [ ] Générer l'APK Release final (`./gradlew assembleRelease`)
- [ ] Tester l'installation et les performances sur appareil Android physique

---

## 🚀 Améliorations Futures Possibles (v1.2.0+)

### P1 — Expérience de Lecture (Page Reader & Navigation)
- [x] **Navigation Suivant / Précédent** : Barre d'action au bas du lecteur avec boutons Page Précédente / Suivante.
- [x] **Sommaire Interactif (Table des Matières)** : Extraction des balises `h1`, `h2`, `h3` et Modal Bottom Sheet avec saut d'ancre fluide.
- [x] **Indicateur de Progression de Lecture** : Barre de défilement (%) dynamique au défilement de la page.
- [ ] **Options Typographiques Avancées** : Choix de la police (Sans-Serif / Serif / Monospace) et de l'interlignage dans le lecteur.

### P1 — Gestion Offline & Médias
- [x] **Gestionnaire de Cache Détaillé** : Calcul dynamique et affichage de la taille du cache en Mo (`getCachedPagesTotalBytes`), suppression ciblée des pages d'un livre.
- [x] **Authentification des Images Coil** : Configuration d'un `ImageLoader` personnalisé dans `BookStackApplication` injectant l'en-tête `Authorization: Token $tokenId:$tokenSecret`.
- [x] **Détection Réseau Globale (`NetworkConnectivityObserver`)** : Composant `NetworkConnectivityObserver` écoutant l'état du réseau via `ConnectivityManager` en temps réel.

### P2 — Recherche & Découverte
- [ ] **Filtres de Recherche** : Ajouter des filtres par catégorie (Livres, Étagères, Chapitres, Pages) sur l'écran de recherche.
- [ ] **Historique de Recherche** : Sauvegarder les derniers termes recherchés sous forme de puces (chips) cliquables.

### P2 — Dashboard & Tableau de Bord
- [ ] **Reprise de Lecture Rapide ("Continuer la lecture")** : Ajouter une carte prioritaire en haut du Dashboard permettant de réouvrir la dernière page consultée en 1 clic.
- [ ] **Statistiques de Lecture** : Afficher des compteurs rapides (nombre de livres téléchargés, pages lues récentes).

### P3 — Fonctionnalités Avancées & Sécurité
- [ ] **Partage & Exportation** : Permettre d'exporter une page lue au format Markdown ou d'en copier le lien direct/HTML.
- [ ] **Deep Linking** : Gérer la réception des URLs de serveurs BookStack (`https://my-bookstack.com/books/...`) pour ouvrir l'application directement au bon endroit.
- [ ] **Verrouillage de l'Application** : Option de verrouillage par empreinte digitale / code PIN dans les paramètres.

---

## 📝 Configuration Technique du Projet
- **Version cible actuelle** : v1.1.0 (`versionCode = 2`)
- **JDK Requis** : JDK 21
- **Guide de build Linux** : Consulter `info_build.md`
