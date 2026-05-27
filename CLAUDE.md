# VideoScramble — CLAUDE.md

Application JavaFX de chiffrement/déchiffrement vidéo inspirée des systèmes de brouillage analogique historiques (Canal+ Discret 11, Nagravision, VideoCrypt). Projet académique en cours de développement.

Lien consigne : https://info.iut-bm.univ-fcomte.fr/staff/perrot/DUT-INFO/S5/PMMEDIA/cracKey/assignment.html

## Commandes essentielles

```bash
# Tests unitaires (pure Java, pas besoin d'OpenCV)
mvn test
```

```bash
# Compiler et lancer
mvn clean package
make run
# ou
mvn clean javafx:run
```

L'application démarre en mode GUI. L'utilisateur choisit la vidéo source, l'algorithme de chiffrement et la clé (offset/step) via l'interface graphique.

## Stack technique

| Outil | Version |
|---|---|
| Java | 23 |
| JavaFX | 17.0.9 (controls, swing, media) |
| OpenCV | 4.9.0 (wrapper openpnp) |
| Maven | 3.6+ |

Les dépendances OpenCV et JavaFX sont embarquées dans le fat JAR via `maven-assembly-plugin` pour éviter les problèmes de dépendances natives locales.

## Architecture

```
fr.aimmer/
├── Main.java                    # Entrée, constantes WIDTH/HEIGHT, chargement OpenCV
├── App.java                     # Entrée JavaFX, câblage des scènes racines
├── AppConfig.java               # Record immuable : mode, inputFile, outputDir, offset, step
├── controller/
│   ├── Controller.java                       # Interface fonctionnelle : Supplier<Scene>
│   ├── HomeController.java                   # Écran d'accueil (Chiffrement / Décryption)
│   ├── EncryptionSelectionController.java    # 3 cartes : Nagravision / Discret 11 / VideoCrypt
│   ├── VideoSelectionController.java         # Sélection du fichier source (paramétré par algo)
│   ├── EncryptionSceneController.java        # Résultat : original | chiffré, paramétré par EncryptionMethod
│   ├── DecryptionSelectionController.java    # 3 cartes : Euclide / Pearson / Variation totale
│   ├── EncryptedFileSelectionController.java # Sélection du fichier chiffré à attaquer
│   └── BruteForceSceneController.java        # Résultat d'attaque générique, paramétré par DecryptionMethod
├── math/
│   ├── EncryptionMethod.java        # Interface : displayName / encrypt / decrypt
│   ├── AbstractFramePermutation.java # Template method : factorise la boucle OpenCV
│   ├── NagravisionAlgorithm.java    # Permutation lignes par blocs de puissance de 2
│   ├── Discret11Algorithm.java      # Décalage horizontal pseudo-aléatoire (3 niveaux, wrap-around)
│   ├── VideoCryptAlgorithm.java     # Cut-and-rotate par ligne (involutif)
│   ├── DecryptionMethod.java        # Interface : displayName / attack / totalKeys
│   ├── NagravisionBruteForce.java   # Brute force 256×128, paramétré par RowScoringFunction
│   ├── RowScoringFunction.java      # @FunctionalInterface : double score(byte[], byte[])
│   ├── BruteForceResult.java        # Record : outputFile, offset, step
│   └── scoring/
│       ├── EuclideanScoring.java    # L2 (sqrt(Σ Δ²))
│       ├── PearsonScoring.java      # 1 − corrélation de Pearson
│       └── L1Scoring.java           # Variation totale (Σ |Δ|)
├── ui/scene/
│   └── SceneManager.java        # Singleton thread-safe, routage de scènes
├── listener/
│   └── StageGlobalListener.java # ESC = quitter, BACKSPACE = accueil
├── utils/
│   ├── MathUtils.java           # largestPowerOfTwo, euclideanDistance
│   └── MediaViewFactory.java    # Création MediaView JavaFX
└── view/
    └── GoHomeButton.java        # Composant bouton réutilisable
```

### Patterns clés

**Controller = `Supplier<Scene>`** — chaque écran implémente `Controller` (interface fonctionnelle qui étend `Supplier<Scene>`). Le controller construit son UI programmatiquement dans `get()` et retourne la `Scene`. Pas de FXML.

**SceneManager** — singleton double-checked locking, gère le routage entre scènes avec cache optionnel. Enregistrement via `sm.register(id, controller)`, navigation via `sm.switchTo(id)` ou `sm.switchTo(id, cacheScene)`. Les scènes paramétrées (qui dépendent d'un choix utilisateur) sont enregistrées **dynamiquement** par leur parent, pas dans `App.start()`.

**AppConfig (record immuable)** — toute la configuration de session est portée par `AppConfig`. L'instance est construite dans `Main.main()` avec les valeurs par défaut puis injectée dans les controllers via leur constructeur. L'utilisateur peut ajuster les paramètres via l'UI. Ne jamais ajouter de champs mutables à `Main`. `Main.WIDTH` et `Main.HEIGHT` sont des constantes `final` pour la taille de la fenêtre JavaFX uniquement.

**Injection de dépendance via constructeur** — chaque controller reçoit ses dépendances (`AppConfig`, `EncryptionMethod`, `DecryptionMethod`…) à la construction. Aucun singleton statique métier.

**Traitement asynchrone** — tout traitement OpenCV lourd tourne dans un `javafx.concurrent.Task` (thread séparé) pour ne pas bloquer le thread JavaFX. Ce pattern est obligatoire dans tous les controllers qui lancent un traitement vidéo.

**`EncryptionMethod` + `AbstractFramePermutation`** — la classe abstraite factorise l'ouverture/fermeture d'OpenCV et la boucle frame par frame. Les sous-classes implémentent 3 méthodes : `filePrefix(inverse)`, `prepareForResolution(w, h)` (initialise le compteur `frameIndex` pour la variation par frame), `transformFrame(src, dst, inverse)` (recalcule le mapping/shifts/cuts à chaque frame avec `frameIndex`). La clé est portée par l'état de l'instance (passée au constructeur), pas par les signatures de méthodes.

**`DecryptionMethod` + `RowScoringFunction`** — `NagravisionBruteForce(displayName, scoring)` parcourt l'espace de clés Nagravision (256×128) et délègue la mesure de similarité de lignes à une `RowScoringFunction` (interface fonctionnelle `double score(byte[], byte[])`). Ajouter une nouvelle métrique = une classe de quelques lignes dans `math/scoring/` + une ligne dans `DecryptionSelectionController.TYPES`.

**Brute force = Nagravision uniquement** — `NagravisionBruteForce` n'attaque QUE Nagravision (espace de clés et inverse spécifiques). Une vidéo chiffrée par Discret 11 ou VideoCrypt ne peut pas être restaurée par ces attaques — c'est un point pédagogique souligné dans l'UI (note sous le titre de `DecryptionSelectionController`).

## Algorithmes de chiffrement

### Nagravision (`NagravisionAlgorithm`)

Permutation des lignes de chaque frame par blocs de puissance de 2 :
1. Décompose la hauteur en blocs (`MathUtils.largestPowerOfTwo`)
2. Pour chaque bloc : `dst = base + (offset + (2*step+1)*i) % blockSize`
3. Applique la permutation via `Mat.row(i).copyTo(...)`

Symétrique : le même algorithme chiffre et déchiffre avec les mêmes paramètres. Clé = `(offset ∈ [0,255], step ∈ [0,127])`.

`computeRowMapping` est **package-private** (accessible aux tests et à `NagravisionBruteForce`) — ne pas le rendre `private`.

### Discret 11 (`Discret11Algorithm`)

Décalage horizontal par ligne, **3 niveaux possibles** (0, +4, +8 pixels), wrap-around en bord d'image. Inspiré du système Canal+ historique (0 / 902 ns / 1804 ns dans l'analogique).

- Clé = `seed` (int). Le PRNG `java.util.Random(seed)` génère la séquence de niveaux par ligne.
- L'inverse est l'opération directe avec le shift de signe inversé.

### VideoCrypt (`VideoCryptAlgorithm`)

Cut-and-rotate par ligne : chaque ligne est coupée à un point pseudo-aléatoire, les deux moitiés sont échangées. Quantifié sur 256 positions pour borner l'espace de clés.

- Clé = `seed` (int). Le PRNG génère un bin par ligne dans `[1, min(256, width)-1]` (cut=0 exclu pour éviter une ligne inchangée).
- **Involutif** : appliquer l'algorithme deux fois avec la même graine restaure la vidéo. Le paramètre `inverse` de `transformFrame` est ignoré.

### Dérivation de la graine en GUI

Pour Discret 11 et VideoCrypt, le seed est dérivé de `(offset, step)` : `seed = offset * 128 + step`. Cela donne 32 768 graines distinctes, alignées sur l'espace de clés de Nagravision — l'utilisateur règle toujours `offset`/`step` quel que soit l'algo. Câblage dans `EncryptionSelectionController.TYPES`.

## Attaques de déchiffrement

### `NagravisionBruteForce(displayName, scoring)`

Parcourt les 256×128 = 32 768 clés Nagravision, reconstitue virtuellement chaque frame candidate (sans écriture disque), et calcule la somme des `scoring.score(rows[mapping[i]], rows[mapping[i+1]])`. La clé minimisant le score est retenue.

Optimisations :
- 5 frames échantillonnées entre 20% et 80% de la vidéo (évite intros/outros uniformes)
- Sous-échantillonnage des colonnes (`COLUMN_STRIDE=4`) — précision inchangée, 4× plus rapide

### Scorings disponibles (`math/scoring/`)

| Classe | Formule | Caractéristique pédagogique |
|---|---|---|
| `EuclideanScoring` | `sqrt(Σ Δ²)` | Référence historique du projet |
| `PearsonScoring` | `1 − r` où r est la corrélation de Pearson | **Insensible au décalage de luminosité** (avantage clé démontrable en oral) |
| `L1Scoring` | `Σ |Δ|` (variation totale) | Robuste aux outliers, "cherche l'image la plus lisse" |

Convention `RowScoringFunction` : **score bas = lignes proches**. Les implémentations "plus haut = mieux" (Pearson) doivent retourner `1 − r`.

## Flux UI

```
Accueil
├── Chiffrement → EncryptionSelectionController (Nagravision / Discret 11 / VideoCrypt)
│                  → VideoSelectionController
│                  → EncryptionSceneController (original | chiffré)
└── Décryption  → DecryptionSelectionController (Euclide / Pearson / Variation totale)
                  → EncryptedFileSelectionController
                  → BruteForceSceneController (chiffré | déchiffré + clé trouvée)
```

Les scènes `scene:encryption:video-selection`, `scene:encryption`, `scene:decryption:file-selection` et `scene:decryption:result` sont enregistrées **dynamiquement** par leur parent (elles dépendent du choix utilisateur). Seules `scene:encryption:selection` et `scene:decryption:selection` sont pré-enregistrées dans `App.start()`.

## Comment ajouter…

**…un nouvel algorithme de chiffrement** :
1. Étendre `AbstractFramePermutation`, implémenter `displayName`, `filePrefix`, `prepareForResolution`, `transformFrame`
2. Ajouter une entrée dans `EncryptionSelectionController.TYPES` (1 ligne)

**…une nouvelle métrique de déchiffrement** :
1. Créer une classe dans `math/scoring/` implémentant `RowScoringFunction`
2. Ajouter une entrée dans `DecryptionSelectionController.TYPES` (1 ligne avec `new NagravisionBruteForce(label, new MaMetriqueScoring())`)

## Conventions de code

**Langue** : français pour les messages utilisateur, commentaires et commits. L'anglais est toléré pour les noms de variables/méthodes idiomatiques Java.

**Commits** : conventional commits en français — `feat:`, `fix:`, `doc:`, `chore:`, `refactor:`.

**Formatage** :
- Fichiers du package `math/` : **indentation à 4 espaces** (historique). Matcher cette convention en ajoutant un fichier dans `math/`.
- Fichiers du package `controller/` : **indentation par tabulations**. Matcher en ajoutant un controller.
- Style d'accolades : **Allman** pour classes/méthodes (accolade ouvrante sur nouvelle ligne), **K&R** pour le contrôle de flux (`if`, `for`, `while`).

**Nommage** :
- `SCREAMING_SNAKE_CASE` pour les constantes statiques
- `camelCase` standard Java pour le reste
- IDs de scènes : `"home"`, `"scene:encryption:selection"`, `"scene:encryption:video-selection"`, `"scene:encryption"`, `"scene:decryption:selection"`, `"scene:decryption:file-selection"`, `"scene:decryption:result"`

**UI** : tout construit programmatiquement en Java, pas de FXML. Les composants réutilisables vont dans `view/`.

**Tests unitaires** dans `src/test/java/fr/aimmer/` (JUnit 5). Lancés avec `mvn test`. Les tests **ne nécessitent pas OpenCV** — tester uniquement la logique pure (math, mapping, scoring sur `byte[]`). Ne pas mocker OpenCV.

## Points d'attention

- `OpenCV.loadLocally()` doit être appelé avant tout usage d'OpenCV (fait dans `Main.main`).
- Le `SceneManager` doit être initialisé avec un `Stage` avant tout `switchTo` sans stage explicite.
- `MediaViewFactory.getMediaView` lance la lecture automatiquement (`mediaPlayer.play()`).
- Les fichiers générés sont écrits dans le même dossier que la vidéo d'entrée (`input.getParentFile()`), avec le préfixe correspondant au mode de chiffrement. Les préfixes (`encrypted_`, `encrypted_d11_`, `encrypted_vc_`) sont gérés par chaque algo via `filePrefix()`.
- `App.config` est un champ statique positionné avant `launch()` — c'est le seul moyen propre de passer des paramètres typés à une `Application` JavaFX sans repasser par les args String.
- `NagravisionBruteForce` est **sans état mutable** : une instance par scoring est partagée par toutes les sessions de décryption (cf. `DecryptionSelectionController.TYPES`).
- `AbstractFramePermutation.process()` appelle `prepareForResolution(w, h)` une seule fois par vidéo : la sous-classe y initialise son état (frameIndex, dimensions…). Le mapping/shifts/cuts sont recalculés à chaque frame dans `transformFrame()` pour garantir un brouillage dynamique.

## Conventions git

- Ne te met pas en co auteur.
- Commit en français sauf mot clef de la conventional commit.
