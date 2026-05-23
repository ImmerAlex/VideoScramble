# Attaque Pearson — force brute Nagravision (corrélation de Pearson)

## Principe général

L'attaque Pearson est une méthode de **déchiffrement par force brute** ciblant le chiffrement **Nagravision**. Elle explore les 32 768 clés `(offset, step)` et évalue chaque candidat avec un score basé sur la **corrélation de Pearson** (coefficient `r`) entre lignes adjacentes de l'image reconstituée.

**Hypothèse fondamentale** : dans une image naturelle, les lignes adjacentes sont fortement corrélées (r proche de 1). Une permutation incorrecte produit des lignes décorrélées (r proche de 0). La clé correcte est celle qui **maximise** la corrélation entre lignes consécutives, donc qui **minimise** le score `1 - r`.

## Fonction de scoring

### Coefficient de corrélation de Pearson

```
r = Σ((xi - x̄)(yi - ȳ)) / √(Σ(xi - x̄)² · Σ(yi - ȳ)²)
```

Où `x̄` et `ȳ` sont les moyennes arithmétiques des valeurs des lignes `x` et `y`.

**Propriétés** :
- `r = 1` : signaux identiques (à un facteur d'échelle près)
- `r = 0` : signaux décorrélés (pas de relation linéaire)
- `r = -1` : signaux opposés (inversement proportionnels)

### Score = 1 − r

Conformément à la convention `RowScoringFunction` (**score bas = lignes proches**), le score retourné est `1 - r`. Ainsi :
- Deux lignes identiques → `r = 1` → `score = 0` (parfait)
- Deux lignes décorrélées → `r = 0` → `score = 1` (mauvais)

### Formule de scoring globale

Pour une clé candidate, le score est la **somme** des `(1 - r)` entre chaque paire de lignes consécutives :

```
score_total(offset, step) = Σ_{i=0}^{h-2} (1 - pearson(mapping[i], mapping[i+1]))
```

La clé retenue est celle qui **minimise** ce score total.

## Avantage clé de Pearson

Pearson est **insensible aux décalages de luminosité additive** (transformations affines de type `y = a × x + b`). Concrètement :

- Un dégradé vertical (ciel qui s'assombrit vers le haut) affecte Euclide mais pas Pearson
- Un vignettage (coins plus sombres) affecte Euclide mais pas Pearson
- Un changement global de luminosité entre deux frames n'affecte pas Pearson

C'est l'avantage **pédagogique majeur** de cette métrique, démontrable en comparant ses résultats avec ceux d'Euclide sur des vidéos à fort dégradé lumineux.

## Implémentation du coefficient de Pearson

La formule est implémentée **en une seule passe** avec 5 accumulateurs et la formule fermée :

```
r = (n·Σxy − Σx·Σy) / √((n·Σx² − (Σx)²)·(n·Σy² − (Σy)²))
```

```java
// PearsonScoring.java:34-64
static double pearsonCorrelation(byte[] row1, byte[] row2)
{
    int n = row1.length;
    if (n == 0) return 0;

    double sumX  = 0;
    double sumY  = 0;
    double sumXX = 0;
    double sumYY = 0;
    double sumXY = 0;

    for (int i = 0; i < n; i++) {
        int x = row1[i] & 0xFF;
        int y = row2[i] & 0xFF;
        sumX  += x;
        sumY  += y;
        sumXX += (double) x * x;
        sumYY += (double) y * y;
        sumXY += (double) x * y;
    }

    double numerator = n * sumXY - sumX * sumY;

    double varX = Math.max(0, n * sumXX - sumX * sumX);
    double varY = Math.max(0, n * sumYY - sumY * sumY);
    double denom = Math.sqrt(varX * varY);

    if (denom == 0) return 0;
    return numerator / denom;
}
```

### Protections numériques

1. **`Math.max(0, ...)` sur les variances** : protège contre les erreurs d'arrondi flottant qui pourraient rendre la variance très légèrement négative (impossible mathématiquement, mais possible en `double`)
2. **`if (denom == 0) return 0`** : évite la division par zéro quand une ligne est constante (variance nulle). Dans ce cas, on retourne `r = 0` (corrélation neutre), ce qui est une convention raisonnable : une ligne constante n'apporte pas d'information sur la corrélation
3. **`if (n == 0) return 0`** : cas limite d'une ligne vide (ne devrait pas se produire en pratique)

### Score final

```java
// PearsonScoring.java:18-21
public double score(byte[] row1, byte[] row2)
{
    return 1.0 - pearsonCorrelation(row1, row2);
}
```

## Intégration dans l'attaque

Pearson s'intègre dans le même moteur `NagravisionBruteForce` qu'Euclide, via l'injection de dépendance :

```java
// DecryptionSelectionController.java:31-33
new DecryptionType("Pearson",
    "Force brute — corrélation de Pearson (insensible aux décalages de luminosité)",
    new NagravisionBruteForce("Pearson", new PearsonScoring()))
```

L'architecture est identique à l'attaque Euclide — seules les 10 lignes de `PearsonScoring` changent. C'est le pattern **Strategy** appliqué aux métriques de scoring : `NagravisionBruteForce` est paramétré par une `RowScoringFunction`, et chaque métrique est une implémentation distincte de cette interface fonctionnelle.

## Optimisations partagées

Pearson bénéficie des mêmes optimisations que l'attaque Euclide :

| Optimisation | Description |
|---|---|
| Échantillonnage temporel | 5 frames entre 20 % et 80 % de la vidéo |
| Sous-échantillonnage spatial | 1 colonne sur 4 (`COLUMN_STRIDE = 4`) |
| Reconstruction virtuelle | Pas d'écriture disque pendant l'exploration des clés |

Voir la documentation de [l'attaque Euclide](euclide.md) pour le détail de ces mécanismes, qui sont implémentés dans `NagravisionBruteForce` et partagés par toutes les métriques.

## Résultat

```java
public record BruteForceResult(File outputFile, int offset, int step) {}
```

Le résultat contient :
- `outputFile` : le fichier vidéo déchiffré
- `offset`, `step` : la clé Nagravision retrouvée

## Comparaison avec Euclide

| Critère | Euclide (L2) | Pearson |
|---|---|---|
| Formule | `√(Σ Δ²)` | `1 − r` |
| Sensibilité au dégradé vertical | **Forte** (pénalise les variations naturelles) | **Nulle** (insensible aux translations additives) |
| Coût de calcul | `Σ Δ² + sqrt` (par paire) | `5 accumulateurs + division + sqrt` (par paire) |
| Robustesse | Standard | Meilleure sur vidéos à éclairage non uniforme |
| Interprétation | Distance géométrique (pixels) | Corrélation statistique (sans unité) |
