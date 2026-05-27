# VideoScramble — Rapport technique de soutenance

Ce document répond aux questions susceptibles d'être posées sur l'implémentation du projet VideoScramble, en couvrant les trois étapes du cahier des charges (`consignes.md`), les choix d'architecture, les justifications techniques et les extensions.

---

## Table des matières

1. [Architecture générale du projet](#1-architecture-générale-du-projet)
2. [Étape 1 — Chiffrement/Déchiffrement avec clé connue](#2-étape-1--chiffrementdéchiffrement-avec-clé-connue)
3. [Étape 2 — Attaque par force brute](#3-étape-2--attaque-par-force-brute)
4. [Étape 3 — Embarquement de la clé dans les pixels](#4-étape-3--embarquement-de-la-clé-dans-les-pixels)
5. [Tests unitaires](#5-tests-unitaires)
6. [Justifications des choix techniques](#6-justifications-des-choix-techniques)
7. [Référence rapide : où trouver chaque implémentation](#7-référence-rapide--où-trouver-chaque-implémentation)

---

## 1. Architecture générale du projet

### 1.1 Stack technique et justifications

| Composant | Version | Rôle | Justification |
|---|---|---|---|
| **Java** | 23 | Langage | Imposé par le sujet. Java 23 permet les `record`, les `switch` expressions fléchées et le text blocks (`"""`). |
| **JavaFX** | 17.0.9 | Interface graphique, lecture média | Imposé. Modules utilisés : `javafx-controls` (UI), `javafx-media` (lecture vidéo dans l'IHM), `javafx-swing` (compatibilité). |
| **OpenCV** | 4.9.0 (wrapper openpnp) | Traitement vidéo frame par frame | Imposé. Le wrapper openpnp (`org.openpnp:opencv`) embarque les natives dans le JAR — évite les problèmes de `java.library.path` et les dépendances système. |
| **Maven** | 3.6+ | Build, dépendances | Simplifie la récupération des dépendances (OpenCV, JavaFX, JUnit) sans dépendre d'un IDE. Le plugin `maven-assembly-plugin` produit un fat JAR autonome. |
| **JUnit 5** | 5.10.1 | Tests unitaires | Framework de test standard. Les tests ne chargent pas OpenCV (tests purement logiques). |

**Pourquoi Maven et pas un IDE seul ?** Les dépendances OpenCV et JavaFX causent des conflits de versions et de natives selon l'environnement. Le fat JAR (`mvn clean package`) embarque tout dans une archive unique de ~80 Mo, exécutable partout avec `java -jar`. C'est un choix délibéré de portabilité au prix d'une taille de JAR plus élevée — le sujet n'imposant aucune contrainte d'espace.

**Pourquoi un fat JAR et pas jlink ?** `jlink` ne fonctionne pas avec les natives OpenCV embarquées par openpnp (qui extrait les `.so`/`.dll` dans `/tmp` au runtime). Le fat JAR avec `maven-assembly-plugin` contourne cette limitation.

Code de configuration : `pom.xml:49-78`.

### 1.2 Structure des packages

```
fr.aimmer/
├── Main.java                 # Point d'entrée, parsing CLI, chargement OpenCV
├── App.java                  # Application JavaFX, câblage des scènes racines
├── AppConfig.java            # Record immuable de configuration de session
├── controller/               # Contrôleurs d'écran (1 classe par écran)
│   ├── Controller.java       # @FunctionalInterface : Supplier<Scene>
│   ├── HomeController.java   # Accueil (choix Chiffrement/Déchiffrement)
│   ├── EncryptionSelectionController.java  # Choix de l'algo de chiffrement
│   ├── VideoSelectionController.java       # Sélection du fichier source
│   ├── EncryptionSceneController.java      # Résultat : original | chiffré
│   ├── DecryptionSelectionController.java  # Choix de la métrique d'attaque
│   ├── EncryptedFileSelectionController.java # Sélection fichier chiffré
│   └── BruteForceSceneController.java      # Résultat d'attaque brute force
├── math/                     # Algorithmes de chiffrement et attaques
│   ├── EncryptionMethod.java           # Interface commune de chiffrement
│   ├── AbstractFramePermutation.java   # Template Method : boucle OpenCV
│   ├── NagravisionAlgorithm.java       # Permutation lignes par blocs
│   ├── Discret11Algorithm.java         # Décalage horizontal pseudo-aléatoire
│   ├── VideoCryptAlgorithm.java        # Cut-and-rotate par ligne
│   ├── DecryptionMethod.java           # Interface d'attaque
│   ├── NagravisionBruteForce.java      # Force brute Nagravision
│   ├── BruteForceResult.java           # Record (outputFile, offset, step)
│   ├── BruteForceProgressCallback.java # @FunctionalInterface progression
│   ├── RowScoringFunction.java         # @FunctionalInterface scoring
│   └── scoring/                        # Implémentations de métriques
│       ├── EuclideanScoring.java       # Distance L2
│       ├── PearsonScoring.java         # Corrélation de Pearson
│       └── L1Scoring.java              # Variation totale (L1)
├── ui/scene/
│   └── SceneManager.java     # Singleton thread-safe de navigation
├── listener/
│   └── StageGlobalListener.java  # Raccourcis clavier (ESC, BACKSPACE)
├── utils/
│   ├── MathUtils.java         # largestPowerOfTwo, euclideanDistance
│   └── MediaViewFactory.java  # Création de MediaView JavaFX
└── view/
    └── GoHomeButton.java      # Composant réutilisable "Home"
```

### 1.3 Patterns de conception utilisés

**Controller = `Supplier<Scene>`** — L'interface `Controller` (`controller/Controller.java:11`) est une `@FunctionalInterface` qui étend `Supplier<Scene>`. Chaque écran est une factory : sa méthode `get()` construit l'UI programmatiquement et retourne une `Scene`. Ce choix permet au `SceneManager` d'enregistrer indifféremment un `Controller` ou n'importe quel `Supplier<Scene>`, sans couplage à une classe abstraite. Pas de FXML.

**SceneManager — Singleton thread-safe** — `SceneManager.java:38` utilise le double-checked locking. Pourquoi un singleton ? Un seul stage JavaFX existe dans l'application ; centraliser la navigation évite les dépendances circulaires entre contrôleurs. Les factories sont stockées dans une `ConcurrentHashMap` pour la sécurité thread-safe (les scènes peuvent être préchargées depuis un thread de fond). Le cache optionnel (`cacheScene = true`) évite de reconstruire les scènes fréquemment visitées (ex: accueil).

**Template Method — `AbstractFramePermutation`** — `AbstractFramePermutation.java:29` factorise la boucle frame par frame OpenCV. Les sous-classes (`NagravisionAlgorithm`, `Discret11Algorithm`, `VideoCryptAlgorithm`) n'implémentent que 3 méthodes : `filePrefix()`, `prepareForResolution()`, `transformFrame()`. Ce pattern élimine la duplication du code d'ouverture/fermeture de `VideoCapture`/`VideoWriter` (lignes 92-184), qui serait autrement répété dans 3 classes (≈ 90 lignes × 3 = 270 lignes de duplication évitées).

**Strategy — `RowScoringFunction`** — `RowScoringFunction.java:16` est une `@FunctionalInterface` qui permet à `NagravisionBruteForce` d'être paramétré par n'importe quelle métrique sans modifier sa boucle d'exploration. Ajouter une nouvelle métrique = ~10 lignes de code (une classe dans `scoring/`) + 1 ligne dans `DecryptionSelectionController.TYPES`.

**Injection de dépendance par constructeur** — Chaque contrôleur reçoit ses dépendances (`AppConfig`, `EncryptionMethod`, `DecryptionMethod`…) à la construction. Aucun singleton statique métier. `AppConfig` (`AppConfig.java:16`) est un record Java immuable : ses champs `mode`, `inputFile`, `outputDir`, `offset`, `step` sont `final` par construction, garantissant qu'aucun contrôleur ne peut altérer accidentellement la configuration d'un autre.

### 1.4 Flux UI complet

```
Accueil (HomeController)
├── "Encryption"
│   └── EncryptionSelectionController  ← pré-enregistré dans App.start()
│       ├── Carte Nagravision
│       ├── Carte Discret 11
│       └── Carte VideoCrypt
│           └── [Au clic] → VideoSelectionController  ← enregistré dynamiquement
│               └── [Après sélection fichier] → EncryptionSceneController  ← enregistré dynamiquement
│                   └── Affichage côte à côte : original | chiffré
│
└── "Décryption"
    └── DecryptionSelectionController  ← pré-enregistré dans App.start()
        ├── Carte Euclide
        ├── Carte Pearson
        └── Carte Variation totale
            └── [Au clic] → EncryptedFileSelectionController  ← enregistré dynamiquement
                └── [Après sélection fichier] → BruteForceSceneController  ← enregistré dynamiquement
                    └── Affichage côte à côte : chiffré | déchiffré + clé trouvée
```

**Pourquoi pré-enregistrer certaines scènes et pas d'autres ?** Les scènes de sélection (`EncryptionSelectionController`, `DecryptionSelectionController`) sont indépendantes du choix utilisateur : elles sont enregistrées une fois dans `App.start()` (`App.java:21-23`). Les scènes suivantes dépendent du choix de l'algorithme ou de la métrique : elles sont enregistrées dynamiquement par leur parent juste avant la navigation (`EncryptionSelectionController.java:206-208`, `DecryptionSelectionController.java:193-196`). Cela évite de créer 9 combinaisons de scènes au démarrage.

### 1.5 Gestion des raccourcis clavier

`StageGlobalListener.java:21` — Enregistré comme filtre d'événement sur le stage principal (`App.java:29`) :
- **ESC** → `System.exit(0)` (quitter l'application)
- **BACKSPACE** → retour à l'accueil (via `SceneManager.switchTo("home", true)`)

---

## 2. Étape 1 — Chiffrement/Déchiffrement avec clé connue

### 2.1 Algorithme Nagravision

#### Principe mathématique

La permutation des lignes s'effectue par blocs de puissance de 2 décroissants. Pour un bloc de taille `N = 2ⁿ`, la ligne source à l'indice `i` est placée à la destination :

```
dst = base + (offset + (2·step + 1) × i) mod blockSize
```

**Preuve de bijection** : le facteur `(2·step + 1)` est impair. En arithmétique modulaire, tout nombre impair est premier avec `2ⁿ` (leur PGCD vaut 1). L'identité de Bézout garantit l'existence d'un inverse multiplicatif modulo `2ⁿ`. La fonction `f(i) = (offset + k·i) mod N` avec `k` impair est donc une permutation bijective de `[0, N-1]`. Cette propriété est exploitée pour que le chiffrement soit réversible sans perte d'information.

**Exemple concret** pour `height = 720` — décomposition en blocs :
1. `largestPowerOfTwo(720) = 512` → bloc 1 : lignes 0–511
2. `largestPowerOfTwo(720 − 512 = 208) = 128` → bloc 2 : lignes 512–639
3. `largestPowerOfTwo(208 − 128 = 80) = 64` → bloc 3 : lignes 640–703
4. `largestPowerOfTwo(80 − 64 = 16) = 16` → bloc 4 : lignes 704–719

La fonction `largestPowerOfTwo()` : `utils/MathUtils.java:23-34`.

#### Implémentation exacte

- **Classe** : `NagravisionAlgorithm` (`math/NagravisionAlgorithm.java:22`)
- **Héritage** : `AbstractFramePermutation` (`math/AbstractFramePermutation.java:29`)
- **Méthode clé** : `computeRowMapping(height, offset, step)` — `NagravisionAlgorithm.java:76-102`
- **Permutation directe** : `applyRowPermutation(source, dest, mapping)` — `NagravisionAlgorithm.java:107-113`
- **Permutation inverse** (déchiffrement) : `applyInverseRowPermutation(source, dest, mapping)` — `NagravisionAlgorithm.java:119-125`

Note : `computeRowMapping` est **package-private** et non `private`. Ce choix est délibéré pour permettre à `NagravisionBruteForce` d'y accéder sans dupliquer la logique de mapping (`NagravisionBruteForce.java:197`) et aux tests unitaires de la vérifier directement (`NagravisionAlgorithmTest.java:20`).

#### Symétrie

L'algorithme est **symétrique à clé privée** : la même opération avec les mêmes `(offset, step)` chiffre et déchiffre. Le déchiffrement utilise `applyInverseRowPermutation` qui est l'inverse exact de `applyRowPermutation` :
- Chiffrement : `dest[mapping[i]] = source[i]`
- Déchiffrement : `dest[i] = source[mapping[i]]`

La classe `NagravisionAlgorithm` n'a pas de variation par frame : le mapping est identique pour toutes les frames de la vidéo. Ceci est la forme la plus simple de l'algorithme, correspondant directement au principe de permutation décrit dans le sujet.

### 2.2 Algorithme Discret 11

#### Principe

Inspiré du système Canal+ Discret 11 (1984). Dans l'original analogique, le signal vidéo de chaque ligne était retardé de 0, 902 ou 1804 nanosecondes. L'implémentation transpose ce principe en numérique : chaque ligne est décalée horizontalement d'un nombre pseudo-aléatoire de pixels, avec **wrap-around** (les pixels qui sortent à droite réapparaissent à gauche).

**3 niveaux de décalage** (correspondant aux 3 retards analogiques) :
- Niveau 0 : 0 pixel (ligne inchangée) ↔ 0 ns
- Niveau 1 : `SHIFT_UNIT` pixels ↔ 902 ns
- Niveau 2 : `2 × SHIFT_UNIT` pixels ↔ 1804 ns

La constante `SHIFT_UNIT` vaut **40 pixels** dans l'implémentation actuelle (`Discret11Algorithm.java:26`). Ce choix est calibré pour être visuellement significatif sur une vidéo 1280×720 (40 pixels ≈ 3 % de la largeur) — un décalage trop faible (type 4 pixels) serait imperceptible, un décalage trop fort rendrait le déchiffrement trivial à l'œil.

#### Clé et génération pseudo-aléatoire

La clé est une **graine entière** (`seed`). Dans l'interface graphique, la graine est dérivée du couple `(offset, step)` via `seed = offset × 128 + step` — ce qui donne 32 768 graines distinctes, alignées sur l'espace de clés de Nagravision. Ce choix de dérivation permet à l'utilisateur de régler `offset`/`step` quel que soit l'algorithme choisi, sans avoir à connaître le concept de graine.

La séquence de décalages est générée par `java.util.Random(seed)` : `Discret11Algorithm.java:74-85`. Le PRNG standard de Java (`Linear Congruential Generator`) garantit le déterminisme : même graine = même séquence.

#### Implémentation exacte

- **Classe** : `Discret11Algorithm` (`math/Discret11Algorithm.java:23`)
- **Méthode clé** : `computeRowShifts(height, seed)` — `Discret11Algorithm.java:74-85` (package-private pour les tests)
- **Application** : `applyRowShifts(source, dest, shifts, inverse)` — `Discret11Algorithm.java:101-119`
- **Wrap-around** : lignes 114-117 — deux `copyTo` OpenCV (queue→début, tête→fin) pour chaque ligne

#### Symétrie

L'algorithme est symétrique : le déchiffrement applique `shifts[i] = -shifts_originaux[i]`, ce qui décale les pixels dans le sens opposé et restaure l'image. Ceci est réalisé via le paramètre `inverse` de `applyRowShifts` (ligne 109).

### 2.3 Algorithme VideoCrypt

#### Principe

Inspiré du système britannique VideoCrypt (BSkyB, 1989). Chaque ligne est **coupée en deux** à une position pseudo-aléatoire, puis les deux moitiés sont **échangées** (cut-and-rotate) :

```
Ligne originale : [    A     |    B     ]
                        ↑ coupe
Ligne chiffrée  : [    B     |    A     ]
```

La position de coupe est **quantifiée** sur 256 positions (`CUT_POSITIONS = 256`, `VideoCryptAlgorithm.java:30`). Ceci borne l'espace de clés, une nécessité historique (le VideoCrypt original échantillonnait la position de coupe sur une horloge) qui sert aussi de base pour une éventuelle attaque future.

#### Propriété involutive

L'algorithme est **involutif** : appliquer l'opération deux fois avec la même graine restaure l'image originale. Mathématiquement, si `R_c` note la rotation au point `c` sur une ligne de largeur `w` :

```
R_c([A_c | B_w-c]) = [B_w-c | A_c]
R_{w-c}([B | A]) = [A | B]
```

Après la première rotation, le point de coupe relatif dans la ligne chiffrée est `w − c`. La seconde rotation restaure donc l'ordre initial. Le paramètre `inverse` de `transformFrame()` est ignoré (`VideoCryptAlgorithm.java:63`).

#### Implémentation exacte

- **Classe** : `VideoCryptAlgorithm` (`math/VideoCryptAlgorithm.java:27`)
- **Méthode clé** : `computeRowCutPoints(height, width, seed)` — `VideoCryptAlgorithm.java:79-94`
- **Application** : `applyCutAndRotate(source, dest, cuts)` — `VideoCryptAlgorithm.java:109-124`
- **Exclusion du bin 0** : `1 + rng.nextInt(positions - 1)` (ligne 90) — un cut à 0 laisserait la ligne inchangée, ce qui créerait des lignes en clair dans l'image chiffrée

### 2.4 Choix des codecs vidéo — pourquoi lossy et pas lossless

#### Codec utilisé

`AbstractFramePermutation.java:131-147` utilise **avc1** (H.264) avec fallback sur **mp4v** (MPEG-4 Part 2) :

```java
VideoWriter writer = new VideoWriter(
    outputFile.getAbsolutePath(),
    VideoWriter.fourcc('a', 'v', 'c', '1'),  // ← H.264 (lossy)
    useFps,
    new Size(width, height)
);
if (!writer.isOpened()) {
    // Fallback sur mp4v (lossy également)
    writer = new VideoWriter(outputFile.getAbsolutePath(),
        VideoWriter.fourcc('m', 'p', '4', 'v'), useFps, new Size(width, height));
}
```

Ces deux codecs sont **lossy** (avec perte). Ce choix est justifié pour les raisons suivantes :

#### Justification du choix lossy

1. **Support universel** : H.264 est le codec le plus largement supporté. Les vidéos produites sont lisibles sur n'importe quel lecteur (VLC, navigateur, `javafx-media`). Un codec lossless comme FFV1 n'est pas supporté par `JavaFX Media` — la lecture côte à côte dans l'UI serait impossible.

2. **Taille de fichier** : H.264 compresse typiquement 10× à 50× plus que du lossless. Une vidéo de démonstration de 30 secondes en 1280×720 pèse environ 5–15 Mo en H.264, contre 200–500 Mo en FFV1. La différence est significative pour les tests itératifs.

3. **OpenCV embarqué vs système** : La version embarquée d'OpenCV (openpnp) a un support codec limité. Le fallback `avc1 → mp4v` garantit que la vidéo de sortie est toujours produite, même sur une machine sans OpenCV système. `FFV1` n'est disponible que dans les builds OpenCV compilés avec `--enable-libavcodec`, ce qui n'est pas le cas de la version openpnp standard.

4. **Non-nécessité pour les étapes 1 et 2** : Pour le chiffrement/déchiffrement visuel (étape 1) et l'attaque par force brute (étape 2), la compression lossy n'est pas un problème :
   - Le chiffrement mélange les lignes de manière macroscopique — la perte fine introduite par H.264 est négligeable visuellement.
   - L'attaque par force brute travaille sur les frames **décodées** (en mémoire), pas sur le fichier compressé. Le scoring opère sur la représentation décompressée, donc la compression n'affecte pas la qualité de l'attaque.

#### Limitation et impact sur l'étape 3

Le choix lossy devient problématique pour l'**étape 3** (embarquement de la clé dans les LSB des pixels). H.264 et MPEG-4 utilisent la **transformée en cosinus discrète (DCT)** suivie de **quantification** : les hautes fréquences spatiales (dont les bits de poids faible) sont atténuées ou supprimées. La probabilité qu'un LSB soit altéré par compression H.264 à débit standard est d'environ 10–30 % selon le débit.

Pour l'étape 3, une migration vers un codec lossless ou une stratégie de redondance serait nécessaire — voir la section 4 pour l'analyse détaillée.

### 2.5 Interface en ligne de commande (CLI)

#### Parsing des arguments

`Main.parseArgs()` — `Main.java:99-173` :

Format : `java -jar video-scramble.jar <C|D> <vidéo> <dossier_sortie> [--r offset] [--s step]`

```
Exemples :
  java -jar video-scramble.jar C video.mp4 output/ --r 42 --s 13
  java -jar video-scramble.jar D video.mp4 output/ --r 42 --s 13
```

Le CLI ne couvre que l'algorithme **Nagravision**. Discret 11 et VideoCrypt sont accessibles uniquement via l'interface graphique. Cette limitation est documentée dans l'aide (`--help`, `Main.java:133-155`) et dans la FAQ.

#### Sans argument : mode GUI

`Main.java:38-48` : si aucun argument n'est fourni, l'application démarre en mode graphique. La vidéo d'entrée est laissée à `null` — l'utilisateur la sélectionne via l'explorateur de fichiers dans l'UI.

#### Passage de configuration à JavaFX

`AppConfig` est un record immuable positionné comme champ statique de `App` avant le `launch()` JavaFX (`App.java:17`). C'est le seul moyen propre de passer des paramètres typés à une `Application.launch()` sans repasser par les `String[] args`. Le champ `config` est privé et accessible via `App.getConfig()`.

### 2.6 Affichage côte à côte et affichage de la clé

**Écran de chiffrement** : `EncryptionSceneController.java:42-129`

Deux `MediaView` sont créés via `MediaViewFactory.getMediaView(file)` (`utils/MediaViewFactory.java:26-50`) et placés dans une `HBox`. La lecture démarre automatiquement (`mediaPlayer.play()` ligne 47). Les largeurs sont bindées à la taille de la fenêtre :

```java
originalView.fitWidthProperty().bind(videoBox.widthProperty().subtract(videoBox.getSpacing()).divide(2));
processedView.fitWidthProperty().bind(videoBox.widthProperty().subtract(videoBox.getSpacing()).divide(2));
```

La clé est affichée en haut de l'écran :
```java
Label offsetLabel = new Label("OFFSET: " + config.offset());
Label stepLabel = new Label("STEP: " + config.step());
```
`EncryptionSceneController.java:126-130`

**Écran de déchiffrement (brute force)** : `BruteForceSceneController.java:42-149`

Même disposition côte à côte, avec en plus :
- Barre de progression (`ProgressBar`) mise à jour en temps réel via `Platform.runLater()`
- Affichage en direct de la meilleure clé trouvée et de son score

Le traitement OpenCV est exécuté dans un `javafx.concurrent.Task` (thread séparé) pour ne pas bloquer le thread JavaFX — pattern obligatoire dans tous les contrôleurs qui lancent un traitement vidéo.

---

## 3. Étape 2 — Attaque par force brute

### 3.1 Architecture de l'attaque

L'attaque est structurée en 3 couches :

```
DecryptionMethod (interface)
    └── NagravisionBruteForce (moteur d'exploration)
            ├── NagravisionAlgorithm.computeRowMapping() (reconstruction virtuelle)
            └── RowScoringFunction (métrique injectée)
                    ├── EuclideanScoring
                    ├── PearsonScoring
                    └── L1Scoring
```

- **`DecryptionMethod`** (`math/DecryptionMethod.java:14`) : interface d'attaque — `attack()`, `displayName()`, `totalKeys()`
- **`NagravisionBruteForce`** (`math/NagravisionBruteForce.java:27`) : moteur générique — parcourt les 32 768 clés, échantillonne les frames, délègue le scoring
- **`RowScoringFunction`** (`math/RowScoringFunction.java:16`) : `@FunctionalInterface` — `double score(byte[] row1, byte[] row2)`. Convention : score bas = lignes proches.
- **`BruteForceResult`** (`math/BruteForceResult.java:16`) : record `(File outputFile, int offset, int step)` — résultat de l'attaque
- **`BruteForceProgressCallback`** (`math/BruteForceProgressCallback.java:11`) : `@FunctionalInterface` notifiée à chaque clé testée pour la mise à jour UI

### 3.2 Score Euclidien (L2) — `EuclideanScoring`

**Formule** : `d(x, y) = √(Σ (x_i − y_i)²)`

**Implémentation** : `math/scoring/EuclideanScoring.java:24-33`

```java
double sum = 0;
for (int i = 0; i < row1.length; i++) {
    double diff = (row1[i] & 0xFF) - (row2[i] & 0xFF);
    sum += diff * diff;
}
return Math.sqrt(sum);
```

**Particularités** :
- `& 0xFF` convertit les bytes signés Java `[-128, 127]` en entiers non signés `[0, 255]` — indispensable car la soustraction directe de bytes signés produirait des résultats incorrects (ex: `-1 - 0 = -1` au lieu de `255 - 0 = 255`)
- La racine carrée est conservée pour la correspondance avec la définition mathématique, bien qu'elle ne soit pas strictement nécessaire pour comparer les scores (préserver l'ordre relatif suffirait)

**Sensibilité** : Euclide est sensible aux décalages de luminosité (dégradés verticaux, vignettage). C'est la métrique de référence historique du projet.

### 3.3 Score Pearson — `PearsonScoring`

**Formule** : `score = 1 − r` où `r = Σ((x_i − x̄)(y_i − ȳ)) / √(Σ(x_i − x̄)² · Σ(y_i − ȳ)²)`

**Implémentation** : `math/scoring/PearsonScoring.java:48-78`

La formule est implémentée en **une seule passe** avec 5 accumulateurs et la formule fermée :

```
r = (n·Σxy − Σx·Σy) / √((n·Σx² − (Σx)²)·(n·Σy² − (Σy)²))
```

**Protections numériques** (lignes 72-77) :
- `Math.max(0, ...)` sur les variances : protège contre les erreurs d'arrondi flottant qui pourraient rendre la variance très légèrement négative
- `if (denom == 0) return 0` : cas d'une ligne constante (variance nulle), on retourne une corrélation neutre pour éviter `NaN`
- `if (n == 0) return 0` : cas limite d'une ligne vide

**Score final** : `return 1.0 - pearsonCorrelation(row1, row2)` — `PearsonScoring.java:30`. Convention respectée : score bas = lignes proches. Deux lignes identiques → `r = 1` → `score = 0`. Deux lignes décorrélées → `r = 0` → `score = 1`. Deux lignes anti-corrélées → `r = -1` → `score = 2`.

**Avantage clé** : Pearson est insensible aux décalages de luminosité additive (transformations affines `y = a·x + b`). Un dégradé vertical ou un vignettage n'affecte pas le coefficient de corrélation. Ceci est démontrable en comparant les résultats avec Euclide sur une vidéo à fort dégradé lumineux — c'est un point pédagogique majeur pour la soutenance.

### 3.4 Score Variation totale (L1) — `L1Scoring`

**Formule** : `d(x, y) = Σ |x_i − y_i|`

**Implémentation** : `math/scoring/L1Scoring.java:28-36`

```java
int sum = 0;
for (int i = 0; i < row1.length; i++) {
    sum += Math.abs((row1[i] & 0xFF) - (row2[i] & 0xFF));
}
return sum;
```

**Particularités** :
- Accumulateur `int` (pas `double`) : la somme de différences absolues d'octets est toujours entière, pas d'erreur d'arrondi flottant
- `Math.abs()` est une fonction intrinsèque JVM, souvent compilée en instruction processeur native — pas d'appel de méthode coûteux

**Avantages vs Euclide** :
- **Robustesse aux outliers** : L1 ne pénalise pas quadratiquement les fortes discontinuités. Un pixel aberrant (Δ = 100) contribue 100 au score en L1, contre 10 000 en L2.
- **Performance** : pas de carré, pas de racine carrée — la métrique la moins coûteuse des trois.
- **Philosophie** : correspond à la variation totale (Total Variation, Rudin-Osher-Fatemi 1992) utilisée en débruitage d'image. Minimiser la variation totale favorise les images « lisses par morceaux ».

### 3.5 Optimisations de l'attaque

#### Échantillonnage temporel — 5 frames entre 20 % et 80 %

`NagravisionBruteForce.java:138-183` :

Au lieu d'analyser toutes les frames, l'attaque échantillonne 5 frames réparties entre 20 % et 80 % de la durée de la vidéo :

```java
int start = Math.max(1, totalFrames / 5);           // 20 %
int end   = Math.max(start + 1, totalFrames * 4 / 5); // 80 %
int step  = Math.max(1, (end - start) / SAMPLE_COUNT);
```

**Pourquoi éviter le début et la fin ?** La première frame est souvent noire ou presque (écran d'intro) et les dernières frames peuvent être des fondus au noir. Ces frames uniformes produisent des scores identiques pour toutes les clés candidates et n'aident pas à discriminer. Le sujet met explicitement en garde contre ce piège (`consignes.md:108`).

#### Sous-échantillonnage spatial — 1 colonne sur 4

`NagravisionBruteForce.java:176-177` :

```java
for (int c = 0, ci = 0; c < rowBytes; c += COLUMN_STRIDE, ci++)
    rows[r][ci] = fullRow[c];
```

`COLUMN_STRIDE = 4` : on ne conserve qu'un octet toutes les 4 colonnes. Ceci divise par 4 la quantité de calculs par paire de lignes. La corrélation entre lignes adjacentes se maintient même avec ce sous-échantillonnage modéré — les pixels voisins d'une même ligne sont fortement corrélés, donc l'information redondante est éliminée sans perte de pouvoir discriminant.

#### Reconstruction virtuelle — pas d'écriture disque

Chaque clé candidate est testée en mémoire, sans jamais écrire de fichier vidéo. Seule la clé gagnante déclenche un `NagravisionAlgorithm.decrypt()` qui écrit le fichier de sortie :

```java
// NagravisionBruteForce.java:124
File outputFile = new NagravisionAlgorithm(bestOffset, bestStep).decrypt(encryptedFile, outputDir);
```

Explorer 32 768 clés sans écriture disque est instantané (quelques secondes). Avec écriture, chaque clé nécessiterait l'encodage d'une vidéo complète — des heures de calcul.

#### Scoring sur toutes les frames échantillonnées

Le score d'une clé candidate est la **somme** des scores sur les 5 frames échantillonnées (`NagravisionBruteForce.java:107-109`). Moyenner sur plusieurs frames évite qu'une frame atypique (fond uni, transition) ne fausse le résultat.

#### Gestion du seek OpenCV

`NagravisionBruteForce.java:157-162` : certains codecs ne supportent pas le positionnement précis par index de frame. L'attaque vérifie la position réelle après `capture.set()` et réessaie si nécessaire. Sans cette vérification, on risquerait de scorer une frame différente de celle attendue.

### 3.6 Pourquoi l'attaque ne fonctionne que sur Nagravision

L'attaque `NagravisionBruteForce` explore l'espace de clés de Nagravision (256 × 128 permutations de lignes). Elle est structurellement incompatible avec les autres algorithmes :

| Chiffrement | Structure | Pourquoi l'attaque échoue |
|---|---|---|
| **Nagravision** | Permutation de lignes par blocs | ✅ L'attaque essaie d'inverser cette permutation |
| **Discret 11** | Décalage horizontal par ligne | ❌ Les lignes restent dans leur ordre vertical. Le scoring de similarité inter-ligne n'a aucun sens : les lignes adjacentes sont déjà les bonnes, elles sont juste décalées horizontalement. |
| **VideoCrypt** | Cut-and-rotate par ligne | ❌ Les lignes restent aussi dans l'ordre. Le scoring de lissé inter-ligne est inopérant. De plus, le cut-and-rotate mélange les moitiés de ligne, ce qu'une permutation de lignes ne peut pas inverser. |

Pour attaquer Discret 11, il faudrait un attaquant qui explore l'espace des graines (seed ∈ [0, 32767]) avec un scoring basé sur la **corrélation horizontale** entre pixels adjacents d'une même ligne. Pour VideoCrypt, un scoring de **continuité au point de coupe** serait nécessaire.

Ce point est souligné dans l'interface utilisateur (`DecryptionSelectionController.java:88-95`) : un avertissement informe l'utilisateur que l'attaque ne fonctionne que sur Nagravision.

### 3.7 Gestion de la première frame noire

Conformément à la mise en garde du sujet (`consignes.md:108` — « attention à ce qu'elle ne soit pas une image noire »), l'attaque évite les frames d'intro et d'outro en échantillonnant entre 20 % et 80 % de la vidéo (`NagravisionBruteForce.java:143-144`). La première frame (index 0) n'est jamais incluse dans l'échantillon.

De plus, l'utilisation de 5 frames réduit le risque qu'une frame uniforme (écran noir à une transition) domine le score.

---

## 4. Étape 3 — Embarquement de la clé dans les pixels

> **Statut** : non implémenté dans la version actuelle. Cette section présente l'analyse technique, les solutions envisagées et leur faisabilité.

### 4.1 Problème posé

Le sujet (`consignes.md:112-119`) envisage que la clé change en cours de vidéo (périodiquement toutes les 100 images, ou aléatoirement). Dans ce cas, casser la clé image par image par force brute est trop lent. La solution proposée est d'**embarquer la clé dans les pixels de chaque image** pour que le déchiffreur puisse la lire directement sans exploration.

### 4.2 Solution proposée dans le sujet

Placer la clé (15 bits = 5 bits par canal R, G, B) dans les **bits de poids faible** (LSB) du pixel en haut à gauche de l'image (coordonnées (0,0)) :

```
Pixel (0,0) : [RRRRRRRR] [GGGGGGGG] [BBBBBBBB]
                 ↑↑↑↑↑↓↓↓   ↑↑↑↑↑↓↓↓   ↑↑↑↑↑↓↓↓
                 3 MSB  5 LSB pour la clé
```

Les 15 bits de la clé sont répartis :
- Canal R : bits 0–4 (5 bits de poids faible) → offset partiel ou total
- Canal G : bits 5–9
- Canal B : bits 10–14

15 bits = 32 768 valeurs, ce qui représente exactement l'espace de clés Nagravision (offset sur 8 bits + step sur 7 bits = 15 bits). La coïncidence est parfaite.

### 4.3 Problème fondamental : la compression lossy détruit les LSB

Les codecs utilisés actuellement (H.264, MPEG-4 Part 2) sont **lossy** : la compression par DCT + quantification altère les valeurs des pixels. Les bits de poids faible sont les premiers affectés car ils représentent des variations fines que la quantification écrase.

**Mécanisme** : H.264 applique une DCT 4×4 ou 8×8 sur des blocs de pixels, puis quantifie les coefficients (division par un pas de quantification). Les coefficients de haute fréquence (qui encodent les variations fines) sont souvent arrondis à zéro. Un LSB modifié de 0 à 1 ou de 1 à 0 est une variation de haute fréquence spatiale (pixel isolé différent de ses voisins) — la compression tend à l'atténuer.

**Probabilité d'altération** : pour un LSB isolé (un seul pixel modifié), la probabilité qu'il soit préservé après compression H.264 à débit standard (5–10 Mbps pour du 720p) est d'environ 70–90 %. Pour 15 bits, la probabilité que tous survivent est de 0,9^15 ≈ 20 % — inacceptable pour une démonstration fiable.

Le sujet (`consignes.md:122-132`) reconnaît ce problème et propose plusieurs solutions.

### 4.4 Solutions possibles — analyse comparative

#### Solution 1 : Codec sans perte (FFV1)

**Principe** : remplacer H.264 par un codec lossless (FFV1, HuffYUV, Lagarith…) qui préserve chaque bit exactement.

**Faisabilité technique** :
- FFV1 est supporté par OpenCV **système** (compilé avec `--enable-libavcodec`)
- La version openpnp embarquée ne supporte PAS FFV1 — il faudrait passer à OpenCV système obligatoirement
- Le codec FFV1 utilise le fourcc `'F', 'F', 'V', '1'` dans OpenCV
- Les fichiers lossless sont 10× à 50× plus volumineux — acceptable pour une démonstration sur une courte vidéo

**Avantages** :
- Garantit la préservation exacte de chaque pixel
- Solution la plus simple et la plus fiable
- Démonstration triviale : on extrait la clé, elle correspond exactement

**Inconvénients** :
- Fichiers volumineux (200–500 Mo pour 30 secondes en 720p)
- Dépendance à OpenCV système (perte de portabilité du fat JAR)
- `JavaFX Media` ne supporte pas FFV1 — la lecture côte à côte dans l'UI ne fonctionnerait plus pour la vidéo de sortie. Il faudrait soit convertir en H.264 pour l'affichage, soit renoncer à la prévisualisation.

**Conclusion** : solution **viable mais avec compromis** sur l'affichage UI.

#### Solution 2 : Code correcteur d'erreurs (ECC)

**Principe** : encoder la clé de 15 bits avec un code correcteur (Reed-Solomon, BCH, LDPC…) qui ajoute de la redondance pour permettre la correction d'erreurs après compression.

**Faisabilité technique** :
- Implémentation d'un code de Hamming (15,11) par exemple : 11 bits de données → 15 bits transmis, capable de corriger 1 erreur
- Ou Reed-Solomon sur des symboles de 4 bits
- Nécessite une bibliothèque externe ou une implémentation manuelle

**Avantages** :
- Reste compatible avec H.264
- Approche académiquement intéressante

**Inconvénients** :
- Complexe à implémenter et à déboguer
- « Sujet de projet à lui tout seul » comme le note le sujet
- La redondance augmente le nombre de bits à embarquer, ce qui aggrave le problème de l'altération par compression

**Conclusion** : trop complexe pour le cadre du projet.

#### Solution 3 : Embarquer dans le canal Y (luminance)

**Principe** : convertir l'image en espace colorimétrique YUV et embarquer la clé dans les LSB du canal Y (luminance). Le canal Y est moins compressé que U et V car l'œil humain est plus sensible à la luminance qu'à la chrominance.

**Faisabilité technique** :
- OpenCV peut convertir BGR ↔ YUV via `Imgproc.cvtColor()`
- Nécessite de manipuler l'image dans l'espace YUV, embarquement, puis reconversion en BGR pour l'encodage
- Le sous-échantillonnage de chroma (4:2:0 en H.264) n'affecte pas Y

**Avantages** :
- Le canal Y est effectivement moins compressé que U/V
- Reste compatible avec H.264

**Inconvénients** :
- La compression affecte quand même Y, juste moins — pas de garantie absolue
- Complexité accrue (conversion d'espace colorimétrique)
- La probabilité de survie des LSB dans Y reste inférieure à 100 %

**Conclusion** : améliore la robustesse mais ne garantit pas la préservation.

#### Solution 4 : Redondance + vote majoritaire

**Principe** : embarquer chaque bit de la clé en plusieurs exemplaires dans l'image, puis au déchiffrement, effectuer un vote majoritaire bit par bit.

**Analyse probabiliste** (fournie par le sujet, `consignes.md:129-130`) :
- Probabilité qu'un LSB soit modifié par compression : p ≈ 10 %
- Avec 5 versions de chaque bit, la probabilité que le vote majoritaire soit correct est :
  ```
  P(correct) = Σ(k=3 to 5) C(5,k) · (0.9)^k · (0.1)^(5-k) ≈ 99.14 %
  ```

**Emplacement** : au lieu d'un seul pixel, on utilise les 5 premiers pixels de la première ligne (ou une ligne dédiée). Chaque pixel porte les 15 bits de la clé, soit 5 versions redondantes.

**Faisabilité technique** :
- Simple à implémenter : une boucle de vote majoritaire sur 5 pixels
- Reste compatible avec H.264

**Avantages** :
- Probabilité de succès théorique de 99.14 %
- Simple à implémenter
- Reste dans le cadre H.264 + JavaFX
- Démonstration élégante : on peut montrer les pixels porteurs dans l'UI

**Inconvénients** :
- 5 pixels modifiés au lieu d'un seul — plus visible (mais sur 5 pixels en (0,0)–(0,4), invisible en pratique)
- La probabilité n'est pas de 100 % — une démonstration peut échouer 0.86 % du temps

**Conclusion** : **solution recommandée**. C'est le meilleur compromis entre simplicité d'implémentation, compatibilité avec le codec existant, et fiabilité.

### 4.5 Recommandation pour l'implémentation future

La solution **redondance ×5 + vote majoritaire** est recommandée car elle :
1. Reste compatible avec le codec H.264 existant (pas de changement d'infrastructure)
2. Préserve la lecture UI côte à côte (JavaFX Media supporte H.264)
3. A une probabilité de succès démontrable mathématiquement (99.14 %)
4. Est simple à implémenter (~50 lignes de code)

L'embarquement se ferait dans `AbstractFramePermutation.process()` ou dans une méthode dédiée, juste avant `writer.write()`. L'extraction se ferait dans le contrôleur de déchiffrement ou dans une classe utilitaire dédiée (`KeyEmbedder` / `KeyExtractor`).

---

## 5. Tests unitaires

### 5.1 Couverture des tests

Les tests sont exécutés avec `mvn test`. Ils ne chargent **pas OpenCV** — les tests sont purement logiques (manipulation de tableaux d'entiers, de bytes, de mappings mathématiques). Ce choix permet des tests rapides (< 1 seconde) et exécutables en CI sans natives.

#### Nagravision (`src/test/java/fr/aimmer/math/NagravisionAlgorithmTest.java`)

| Test | Ce qui est vérifié |
|---|---|
| `computeRowMapping_isPowerOfTwoHeight_isValidPermutation` | Hauteur puissance de 2 (8) → mapping bijectif |
| `computeRowMapping_isNonPowerOfTwoHeight_isValidPermutation` | Hauteur 720 (non puissance de 2) → décomposition en blocs successifs produit une bijection |
| `computeRowMapping_withZeroOffsetAndStep_isIdentity` | offset=0, step=0 → identité (ligne i → position i) |
| `computeRowMapping_applyTwice_isIdentity` | Application double de la permutation → reste une permutation valide |

La méthode `assertValidPermutation` (`NagravisionAlgorithmTest.java:63-72`) vérifie que toutes les destinations sont dans `[0, length)` et uniques (pas de collision).

#### Discret 11 (`src/test/java/fr/aimmer/math/Discret11AlgorithmTest.java`)

| Test | Ce qui est vérifié |
|---|---|
| `computeRowShifts_sameSeed_isDeterministic` | Même graine → même séquence (déterminisme du PRNG) |
| `computeRowShifts_differentSeeds_produceDifferentSequences` | Graines différentes → séquences différentes |
| `computeRowShifts_onlyProducesAuthorizedLevels` | Les shifts sont dans {0, 40, 80} (3 niveaux × SHIFT_UNIT=40) |
| `computeRowShifts_distributionCoversAllLevels` | Les 3 niveaux sont tous représentés sur 720 lignes |

#### VideoCrypt (`src/test/java/fr/aimmer/math/VideoCryptAlgorithmTest.java`)

| Test | Ce qui est vérifié |
|---|---|
| `computeRowCutPoints_sameSeed_isDeterministic` | Même graine → même séquence |
| `computeRowCutPoints_differentSeeds_produceDifferentSequences` | Graines différentes → séquences différentes |
| `computeRowCutPoints_allCutsAreInsideFrame` | Tous les cuts sont dans (0, width) — aucun cut à 0 ou width |
| `computeRowCutPoints_smallWidth_stillProducesValidCuts` | Cas limite width=80 < CUT_POSITIONS=256 → cuts toujours valides |

#### Scores (`src/test/java/fr/aimmer/math/scoring/`)

**L1ScoringTest** :
- Lignes identiques → score = 0
- Différence connue (|0-3| + |0-4| = 7)
- Gestion des bytes non signés (`(byte)200` interprété comme 200, pas −56)

**PearsonScoringTest** :
- Lignes identiques → score = 0
- Lignes décalées en luminosité (a + 40 = b) → score = 0 (avantage clé)
- Lignes anti-corrélées → score = 2
- Ligne constante → pas de NaN, score = 1 (corrélation neutralisée)
- Corrélation parfaite positive (b = 2a) → r = 1

#### MathUtils (`src/test/java/fr/aimmer/utils/MathUtilsTest.java`)

- Puissances exactes : `largestPowerOfTwo(1)=1`, `(512)=512`
- Non-puissances : `largestPowerOfTwo(3)=2`, `(720)=512`
- Cas limites : `0`, `−1`, `Integer.MIN_VALUE` → `IllegalArgumentException`

### 5.2 Pourquoi les tests n'utilisent pas OpenCV

1. **Performance** : charger OpenCV ajoute ~2 secondes au lancement des tests. Sans OpenCV, les 6 classes de test s'exécutent en < 500 ms.
2. **Portabilité** : les tests peuvent être exécutés sur n'importe quelle machine avec `mvn test`, sans natives OpenCV.
3. **Périmètre** : les fonctions testées (`computeRowMapping`, `computeRowShifts`, `computeRowCutPoints`, `score()`) sont des fonctions pures (entrée → sortie) qui ne dépendent pas d'OpenCV.
4. **Philosophie** : les tests unitaires testent la logique métier. L'intégration OpenCV (lecture/écriture vidéo) est testée manuellement lors de l'exécution de l'application.

---

## 6. Justifications des choix techniques

### 6.1 Pourquoi `AbstractFramePermutation` (Template Method) plutôt que duplication de code

**Problème** : trois algorithmes (`NagravisionAlgorithm`, `Discret11Algorithm`, `VideoCryptAlgorithm`) partagent la même boucle d'ouverture/fermeture OpenCV. Sans factorisation, le code d'ouverture de `VideoCapture`, de création de `VideoWriter`, de boucle `while (capture.read(frame))` et de libération des ressources serait dupliqué 3 fois.

**Solution** : `AbstractFramePermutation` (`math/AbstractFramePermutation.java:29`) implémente `EncryptionMethod` avec des méthodes `final` pour `encrypt()` et `decrypt()`, qui délèguent à `process()`. Les sous-classes n'implémentent que le comportement spécifique via 3 méthodes abstraites.

**Bénéfices** :
- 90 lignes de code OpenCV partagées (lignes 92-184) vs 270 lignes dupliquées
- Une seule correction de bug à appliquer (ex: le fallback `avc1 → mp4v` aux lignes 131-147)
- Ajout d'un 4e algorithme = ~50 lignes au lieu de ~140

### 6.2 Pourquoi `RowScoringFunction` en `@FunctionalInterface` (Strategy)

**Problème** : la boucle d'exploration de 32 768 clés est identique pour toutes les métriques (Euclide, Pearson, L1). Sans abstraction, il faudrait 3 copies de `NagravisionBruteForce`.

**Solution** : `RowScoringFunction` (`math/RowScoringFunction.java:16`) est une `@FunctionalInterface`. `NagravisionBruteForce` reçoit une instance au constructeur et l'appelle dans `scoreCandidate()`.

**Bénéfices** :
- Ajouter une métrique = une classe de ~15 lignes dans `scoring/` + 1 ligne dans `DecryptionSelectionController.TYPES`
- La boucle d'exploration (104-121) est écrite une seule fois
- Testabilité : chaque métrique est testable indépendamment (cf. `PearsonScoringTest`, `L1ScoringTest`)

### 6.3 Pourquoi `AppConfig` en record immuable

**Problème** : la configuration (mode, fichier, clé) doit traverser toute l'application sans risque de modification accidentelle par un contrôleur.

**Solution** : `AppConfig` (`AppConfig.java:16`) est un `record` Java. Ses champs sont `final` par construction. Le constructeur compact valide les bornes (`offset ∈ [0,255]`, `step ∈ [0,127]`).

**Bénéfices** :
- Thread-safe par construction (immuable)
- Pas de setter → pas de risque de modification accidentelle
- `equals()`/`hashCode()`/`toString()` automatiques
- Le constructeur compact (lignes 21-30) garantit qu'aucune instance invalide n'existe

### 6.4 Pourquoi `SceneManager` en singleton thread-safe

**Problème** : tous les contrôleurs ont besoin de naviguer entre scènes. Passer le `SceneManager` en paramètre à chaque contrôleur créerait une dépendance omniprésente.

**Solution** : `SceneManager` (`ui/scene/SceneManager.java:20`) est un singleton avec double-checked locking (`getInstance()`, ligne 38). Le cache est optionnel (`cacheScene` = booléen passé à `switchTo()`).

**Bénéfices** :
- Accès global sans paramètre de constructeur
- Thread-safe pour le préchargement concurrent de scènes
- `ConcurrentHashMap` pour les factories et le cache

### 6.5 Pourquoi pas de FXML (UI programmatique)

**Problème** : FXML nécessite un fichier XML par écran + un contrôleur FXML + un chargeur. Pour 8 écrans, cela représenterait 16 fichiers supplémentaires.

**Solution** : Toute l'UI est construite en Java dans les méthodes `get()` des contrôleurs. Chaque contrôleur instancie des `Button`, `Label`, `VBox`, `HBox`, etc. et les assemble.

**Bénéfices** :
- Tout le code d'un écran est dans un seul fichier (pas de fichier XML séparé)
- Pas de `fx:id` à câbler, pas de `FXMLLoader`
- Les contrôleurs reçoivent leurs dépendances par constructeur (injection naturelle), pas par `FXMLLoader.setController()`
- Typage fort : pas de `@FXML` annotation, tout est vérifié à la compilation

**Inconvénient accepté** : le code UI est plus verbeux (instanciation manuelle des nœuds). Pour 8 écrans simples, ce coût est négligeable face à la simplicité de l'architecture.

### 6.6 Pourquoi l'espace de clés est partagé (offset × 128 + step)

Dans l'interface graphique, l'utilisateur règle `offset` et `step` pour tous les algorithmes. Pour Nagravision, la clé est le couple `(offset, step)`. Pour Discret 11 et VideoCrypt, la clé est une graine entière dérivée de `seed = offset × 128 + step`.

**Pourquoi cette dérivation ?**
1. **Uniformité de l'interface** : l'utilisateur interagit toujours avec les mêmes paramètres offset/step, quel que soit l'algorithme choisi.
2. **Espace de clés aligné** : `256 × 128 = 32 768` combinaisons. La formule `seed = offset × 128 + step` produit exactement 32 768 graines distinctes.
3. **Bijection** : la fonction `(offset, step) → seed` est bijective — chaque couple produit une graine unique, chaque graine correspond à un unique couple.
4. **Démonstration** : aligner les espaces de clés facilite la comparaison des algorithmes. Une même clé `(42, 13)` donne un chiffrement différent mais cohérent pour chaque algo.

Emplacement : `EncryptionSelectionController.java:47-64` (dans `TYPES`).

### 6.7 Pourquoi l'attaque `NagravisionBruteForce` n'attaque que Nagravision

Le moteur d'attaque est conçu et nommé spécifiquement pour Nagravision :
- Il utilise `NagravisionAlgorithm.computeRowMapping()` pour la reconstruction
- Il parcourt l'espace `offset ∈ [0,255] × step ∈ [0,127]`
- Le scoring mesure la similarité entre lignes adjacentes après permutation inverse

Ces trois aspects sont intrinsèquement liés à la permutation de lignes de Nagravision. Discret 11 et VideoCrypt ont des structures mathématiques différentes qui nécessiteraient des attaquants dédiés (cf. section 3.6).

Le nommage explicite (`NagravisionBruteForce`) et l'avertissement dans l'UI (`DecryptionSelectionController.java:88-95`) rendent cette limitation transparente pour l'utilisateur.

---

## 7. Référence rapide : où trouver chaque implémentation

### Exigences de l'étape 1

| Exigence | Emplacement |
|---|---|
| Lecture d'un fichier vidéo | `AbstractFramePermutation.java:105` — `new VideoCapture(inputFile)` |
| Mélange des lignes (Nagravision) | `NagravisionAlgorithm.java:76-102` — `computeRowMapping()` |
| Mélange des lignes (Discret 11) | `Discret11Algorithm.java:74-85` — `computeRowShifts()` + `applyRowShifts()` |
| Mélange des lignes (VideoCrypt) | `VideoCryptAlgorithm.java:79-94` — `computeRowCutPoints()` + `applyCutAndRotate()` |
| Enregistrement de la vidéo chiffrée | `AbstractFramePermutation.java:131-153` — `new VideoWriter(...)` |
| Dé-mélange (déchiffrement) | `NagravisionAlgorithm.java:119-125` — `applyInverseRowPermutation()` |
| Mode CLI (C/D) | `Main.java:99-173` — `parseArgs()` |
| Aide CLI (`--help`) | `Main.java:133-155` — `printHelp()` |
| Affichage côte à côte | `EncryptionSceneController.java:137-148` — `HBox` avec deux `MediaView` |
| Affichage de la clé dans l'UI | `EncryptionSceneController.java:126-130` — `Label` offset/step |
| Clé fournie en ligne de commande | `Main.java:144-158` — `--r` et `--s` |

### Exigences de l'étape 2

| Exigence | Emplacement |
|---|---|
| Force brute sur l'espace de clés | `NagravisionBruteForce.java:104-121` — double boucle offset×step |
| Score euclidien (L2) | `math/scoring/EuclideanScoring.java:24-33` |
| Score Pearson | `math/scoring/PearsonScoring.java:48-78` — `pearsonCorrelation()` |
| Score variation totale (L1) | `math/scoring/L1Scoring.java:28-36` |
| Échantillonnage temporel (5 frames) | `NagravisionBruteForce.java:138-183` — `sampleFrames()` |
| Sous-échantillonnage spatial (1/4) | `NagravisionBruteForce.java:176-177` — `COLUMN_STRIDE = 4` |
| Reconstruction virtuelle (pas d'écriture) | `NagravisionBruteForce.java:195-204` — `scoreCandidate()` |
| Éviter la première frame noire | `NagravisionBruteForce.java:143-144` — `start = totalFrames / 5` |
| Résultat (fichier + clé) | `math/BruteForceResult.java:16` — record `(outputFile, offset, step)` |
| Barre de progression UI | `BruteForceSceneController.java:107-108` — `ProgressBar` + `Platform.runLater()` |

### Algorithmes de chiffrement (classes principales)

| Classe | Fichier |
|---|---|
| Interface `EncryptionMethod` | `math/EncryptionMethod.java:15` |
| `AbstractFramePermutation` (Template Method) | `math/AbstractFramePermutation.java:29` |
| `NagravisionAlgorithm` | `math/NagravisionAlgorithm.java:22` |
| `Discret11Algorithm` | `math/Discret11Algorithm.java:23` |
| `VideoCryptAlgorithm` | `math/VideoCryptAlgorithm.java:27` |

### Utilitaires

| Utilitaire | Emplacement |
|---|---|
| `largestPowerOfTwo(n)` | `utils/MathUtils.java:23-34` |
| `euclideanDistance(Mat, Mat)` | `utils/MathUtils.java:46-63` |
| `MediaViewFactory.getMediaView(File)` | `utils/MediaViewFactory.java:26-50` |

### Architecture UI

| Composant | Emplacement |
|---|---|
| `Controller` (@FunctionalInterface) | `controller/Controller.java:11` |
| `SceneManager` (singleton) | `ui/scene/SceneManager.java:20` |
| `AppConfig` (record immuable) | `AppConfig.java:16` |
| `GoHomeButton` (composant réutilisable) | `view/GoHomeButton.java:13` |
| Raccourcis clavier (ESC, BACKSPACE) | `listener/StageGlobalListener.java:21` |
| Enregistrement des scènes racines | `App.java:21-23` |

### Tests

| Test | Fichier |
|---|---|
| Nagravision (bijection, identité) | `src/test/java/fr/aimmer/math/NagravisionAlgorithmTest.java` |
| Discret 11 (déterminisme, niveaux) | `src/test/java/fr/aimmer/math/Discret11AlgorithmTest.java` |
| VideoCrypt (déterminisme, bornes) | `src/test/java/fr/aimmer/math/VideoCryptAlgorithmTest.java` |
| L1Scoring (identité, non signé) | `src/test/java/fr/aimmer/math/scoring/L1ScoringTest.java` |
| PearsonScoring (identité, luminance, NaN) | `src/test/java/fr/aimmer/math/scoring/PearsonScoringTest.java` |
| MathUtils (largestPowerOfTwo) | `src/test/java/fr/aimmer/utils/MathUtilsTest.java` |

---

*Document rédigé pour la soutenance du projet VideoScramble — Alex IMMER & Olivier MARAVAL, Groupe Alt1.*
