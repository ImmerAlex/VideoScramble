# VideoScramble — CLAUDE.md

Application JavaFX de chiffrement/déchiffrement vidéo inspirée des systèmes de brouillage analogique historiques (Canal+ Discret 11, Nagravision). Projet académique en cours de développement.

## Commandes essentielles

```bash
# Tests unitaires (pure Java, pas besoin d'OpenCV)
mvn test
```


```bash
# Compiler
mvn clean package

# Lancer (valeurs par défaut, vidéo embarquée)
make run
# ou
mvn clean javafx:run

# Lancer avec arguments personnalisés
java -jar target/video-scramble.jar <C|D> <input_video> <output_dir> --r <offset> --s <step>

# Aide CLI
java -jar target/video-scramble.jar --help
```

Arguments CLI :
- `C|D` : mode Chiffrement ou Déchiffrement
- `--r <0-255>` : offset (décalage)
- `--s <0-127>` : step (pas)

Sans arguments : l'application démarre en mode GUI avec la vidéo embarquée (`src/main/resources/video/Pencil_Candle_1280x720.mp4`), offset=42, step=13.

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
├── Main.java                    # Entrée CLI, état global statique
├── App.java                     # Entrée JavaFX, câblage des scènes
├── controller/
│   ├── Controller.java          # Interface fonctionnelle : Supplier<Scene>
│   ├── HomeController.java      # Écran d'accueil (menu)
│   ├── EncryptionSceneController.java  # Chiffrement + affichage côte-à-côte
│   ├── EuclideSceneController.java     # Déchiffrement Euclide (WIP)
│   └── PearsonSceneController.java     # Déchiffrement Pearson (TODO)
├── math/
│   ├── EncryptionAlgorithm.java # Algorithme Nagravision (permutation lignes)
│   └── DecryptionAlgorithm.java # Algorithmes de déchiffrement (WIP)
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

**Controller = Supplier\<Scene\>** — chaque écran implémente `Controller` (interface fonctionnelle qui étend `Supplier<Scene>`). Le controller construit son UI programmatiquement dans `get()` et retourne la `Scene`. Pas de FXML.

**SceneManager** — singleton double-checked locking, gère le routage entre scènes avec cache optionnel. Enregistrement via `sm.register(id, controller)`, navigation via `sm.switchTo(id)` ou `sm.switchTo(id, cacheScene)`.

**AppConfig (record immuable)** — toute la configuration de session est portée par `AppConfig`. `Main.main()` parse les args et construit un `AppConfig`, qui est injecté dans les controllers via leur constructeur. Ne jamais ajouter de champs mutables à `Main`. `Main.WIDTH` et `Main.HEIGHT` sont des constantes `final` pour la taille de la fenêtre JavaFX uniquement.

**Injection de dépendance via constructeur** — chaque controller reçoit son `AppConfig` à la construction dans `App.start()`. Accès dans le controller via `this.config.offset()`, `this.config.inputFile()`, etc.

**Traitement asynchrone** — tout traitement OpenCV lourd tourne dans un `javafx.concurrent.Task` (thread séparé) pour ne pas bloquer le thread JavaFX. Ce pattern est obligatoire dans tous les controllers qui lancent un traitement vidéo.

## Algorithmes

### Chiffrement/Déchiffrement Nagravision (`EncryptionAlgorithm`)

L'algorithme est symétrique. Deux méthodes publiques avec signatures explicites :
- `encrypt(inputFile, outputDir, offset, step)` → fichier `encrypted_<nom>`
- `decrypt(inputFile, outputDir, offset, step)` → fichier `decrypted_<nom>`

Les deux délèguent à `process()` qui fait le vrai travail.

`computeRowMapping` est package-private (accessible aux tests) — ne pas le rendre `private`.

### Chiffrement Nagravision (`EncryptionAlgorithm.encrypt`)

Permutation des lignes de chaque frame vidéo :
1. Décompose la hauteur en blocs de puissance de 2 (`largestPowerOfTwo`)
2. Pour chaque bloc : `dst = base + (offset + (2*step+1)*i) % blockSize`
3. Applique la permutation frame par frame via OpenCV `Mat.row(i).copyTo(...)`

Le même algorithme chiffre ET déchiffre (symétrique avec les bons paramètres).

### Déchiffrement Euclide (`DecryptionAlgorithm.euclideDecrypt(File, File)`) — WIP

Calcule la distance euclidienne entre lignes adjacentes de la première frame pour tenter de retrouver l'ordre original. Implémentation incomplète — lève `UnsupportedOperationException`.

TODO actif : comprendre pourquoi `capture.read(firstFrame)` retourne `false` sur la vidéo chiffrée.

### Déchiffrement Pearson — TODO

`PearsonSceneController` est vide. À implémenter.

## Conventions de code

**Langue** : français pour les messages utilisateur, commentaires et commits. L'anglais est toléré pour les noms de variables/méthodes idiomatiques Java.

**Commits** : conventional commits en français — `feat:`, `fix:`, `doc:`, `chore:`, `refactor:`.

**Formatage** : indentation par tabulations. Le style d'accolades n'est pas encore unifié dans le projet (Allman dans certains fichiers, K&R dans d'autres) — préférer le style Allman (accolade ouvrante sur nouvelle ligne) pour les nouvelles classes et méthodes, comme dans `Main.java`, `App.java`, `SceneManager.java`.

**Nommage** :
- `SCREAMING_SNAKE_CASE` pour les constantes/champs statiques de `Main`
- `camelCase` standard Java pour le reste
- IDs de scènes : `"home"`, `"scene:1"`, `"scene:euclide"`, `"scene:pearson"`

**UI** : tout construit programmatiquement en Java, pas de FXML. Les composants réutilisables vont dans `view/`.

**Tests unitaires** dans `src/test/java/fr/aimmer/` (JUnit 5). Lancés avec `mvn test`. Les tests ne nécessitent pas OpenCV — tester uniquement la logique pure (math, mapping). Ne pas mocker OpenCV.

## Points d'attention

- `OpenCV.loadLocally()` doit être appelé avant tout usage d'OpenCV (fait dans `Main.main`).
- Le `SceneManager` doit être initialisé avec un `Stage` avant tout `switchTo` sans stage explicite.
- `MediaViewFactory.getMediaView` lance la lecture automatiquement (`mediaPlayer.play()`).
- La vidéo de test embarquée est à `src/main/resources/video/Pencil_Candle_1280x720.mp4` (1280×720).
- Le fichier chiffré est nommé `encrypted_<nom>`, le déchiffré `decrypted_<nom>` — géré par `EncryptionAlgorithm`.
- `App.config` est un champ statique positionné avant `launch()` — c'est le seul moyen propre de passer des paramètres typés à une `Application` JavaFX sans repasser par les args String.


## Conventions git

- Ne te met pas en co auteur.
- Commit en francais sauf mot clef de la conventional commit.
- 