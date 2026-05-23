# Attaque Euclide — force brute Nagravision (distance L2)

## Principe général

L'attaque Euclide est une méthode de **déchiffrement par force brute** ciblant le chiffrement **Nagravision** (permutation de lignes). Elle parcourt l'intégralité de l'espace de clés — 32 768 combinaisons `(offset, step)` — et évalue chaque candidat avec la **distance euclidienne** (norme L2) entre lignes adjacentes de l'image reconstituée.

**Hypothèse fondamentale** : une image naturelle a des lignes adjacentes très similaires. Une permutation incorrecte produit des discontinuités entre lignes. La clé correcte est celle qui **minimise** la distance euclidienne entre lignes consécutives.

## Fonction de scoring

### Distance euclidienne L2

```
d(x, y) = √(Σ (xi - yi)²)
```

Où `x` et `y` sont deux lignes de pixels (sous-échantillonnées), et la somme est sur tous les canaux de couleur de tous les pixels échantillonnés de la ligne.

**Score bas = lignes très similaires = image naturelle probable.**

### Formule de scoring globale

Pour une clé candidate, le score est la **somme des distances euclidiennes** entre chaque paire de lignes consécutives après application du mapping Nagravision :

```
score_total(offset, step) = Σ_{i=0}^{h-2} d(lignes[mapping[i]], lignes[mapping[i+1]])
```

Où `mapping` est produit par `NagravisionAlgorithm.computeRowMapping(height, offset, step)`.

La clé retenue est celle qui **minimise** ce score total.

## Architecture de l'attaque

### Chaîne de classes

```
NagravisionBruteForce  (boucle d'exploration, échantillonnage)
  ├── RowScoringFunction   (métrique injectée)
  │     └── EuclideanScoring   (distance L2)
  └── NagravisionAlgorithm.computeRowMapping()  (reconstruction virtuelle)
```

- `NagravisionBruteForce` (`src/main/java/fr/aimmer/math/NagravisionBruteForce.java`) : moteur de l'attaque — parcourt les 32 768 clés, échantillonne les frames, évalue les candidats
- `EuclideanScoring` (`src/main/java/fr/aimmer/math/scoring/EuclideanScoring.java`) : implémentation de la distance L2
- `NagravisionAlgorithm.computeRowMapping()` (`src/main/java/fr/aimmer/math/NagravisionAlgorithm.java:58`) : reconstruit virtuellement l'ordre des lignes pour une clé donnée (sans écrire sur disque)

## Optimisations

### 1. Échantillonnage temporel (5 frames)

Au lieu d'analyser toutes les frames, l'attaque échantillonne **5 frames** réparties entre 20 % et 80 % de la durée de la vidéo. Ceci évite les frames d'intro/outro souvent uniformes (fond noir, titres) qui pourraient tromper le scoring.

```java
// NagravisionBruteForce.java:115-156
int start = Math.max(1, totalFrames / 5);           // 20 %
int end   = Math.max(start + 1, totalFrames * 4 / 5); // 80 %
int step  = Math.max(1, (end - start) / SAMPLE_COUNT);
```

### 2. Sous-échantillonnage spatial (1 colonne sur 4)

Pour chaque ligne, on ne conserve qu'un octet toutes les 4 colonnes (`COLUMN_STRIDE = 4`). Ceci divise par 4 la quantité de calculs par paire de lignes, sans perte significative de précision : la corrélation entre lignes adjacentes se maintient même avec un sous-échantillonnage modéré.

```java
// NagravisionBruteForce.java:148-151
for (int c = 0, ci = 0; c < rowBytes; c += COLUMN_STRIDE, ci++)
    rows[r][ci] = fullRow[c];
```

### 3. Reconstruction virtuelle

La clé candidate n'est **jamais écrite sur disque** pendant l'exploration. Le mapping de lignes est appliqué virtuellement en mémoire :

```java
// NagravisionBruteForce.java:163-172
private double scoreCandidate(byte[][] rows, int height, int offset, int step)
{
    int[] mapping = NagravisionAlgorithm.computeRowMapping(height, offset, step);
    double total = 0;
    for (int i = 0; i < height - 1; i++) {
        total += scoring.score(rows[mapping[i]], rows[mapping[i + 1]]);
    }
    return total;
}
```

Une fois la meilleure clé trouvée, un **seul** `NagravisionAlgorithm.decrypt()` est exécuté pour produire le fichier de sortie.

## Implémentation du scoring euclidien

```java
// EuclideanScoring.java:15-22
public double score(byte[] row1, byte[] row2)
{
    double sum = 0;
    for (int i = 0; i < row1.length; i++) {
        double diff = (row1[i] & 0xFF) - (row2[i] & 0xFF);
        sum += diff * diff;
    }
    return Math.sqrt(sum);
}
```

Points notables :
- Conversion `& 0xFF` : les octets Java sont signés (`[-128, 127]`), on les convertit en entiers non-signés `[0, 255]`
- La racine carrée (`Math.sqrt`) n'est pas strictement nécessaire pour comparer les scores (préserver l'ordre relatif suffirait), mais elle est conservée pour la lisibilité et la correspondance avec la définition mathématique

## Performances

| Paramètre | Valeur |
|---|---|
| Taille espace de clés | 32 768 |
| Frames échantillonnées | 5 |
| Sous-échantillonnage colonnes | 1/4 |
| Paires de lignes évaluées par frame | `height - 1` ≈ 719 |

Pour une vidéo 1280×720, chaque clé candidate évalue environ `5 × 719 × (1280×3/4) ≈ 3,5 millions` de différences de pixels. Le temps de calcul reste acceptable grâce à la simplicité des opérations (addition, soustraction, multiplication).

## Résultat

```java
public record BruteForceResult(File outputFile, int offset, int step) {}
```

Le résultat contient :
- `outputFile` : le fichier vidéo déchiffré
- `offset`, `step` : la clé retrouvée

## Caractéristique pédagogique

Euclide est la métrique **de référence historique** du projet. Elle est intuitive (distance géométrique) et fonctionne bien sur la plupart des vidéos. Cependant, elle est **sensible aux décalages de luminosité** : si la vidéo a un dégradé vertical ou du vignettage, Euclide pénalisera ces variations naturelles et pourrait être moins discriminante que Pearson.
