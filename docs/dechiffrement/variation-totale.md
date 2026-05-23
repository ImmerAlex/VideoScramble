# Attaque Variation totale — force brute Nagravision (norme L1)

## Principe général

L'attaque Variation totale est une méthode de **déchiffrement par force brute** ciblant le chiffrement **Nagravision**. Elle parcourt les 32 768 clés `(offset, step)` et évalue chaque candidat avec la **norme L1** (distance de Manhattan, aussi appelée variation totale) entre lignes adjacentes de l'image reconstituée.

**Hypothèse fondamentale** : une image naturelle est « lisse » — les variations entre lignes adjacentes sont faibles. La clé correcte est celle qui **minimise** la somme des différences absolues entre lignes consécutives.

## Fonction de scoring

### Distance L1 (Manhattan / variation totale)

```
d(x, y) = Σ |xi - yi|
```

Où `x` et `y` sont deux lignes de pixels, et la somme est sur tous les pixels échantillonnés de la ligne.

**Score bas = variation totale faible = image lisse = image naturelle probable.**

### Formule de scoring globale

Pour une clé candidate, le score est la **somme des distances L1** entre chaque paire de lignes consécutives :

```
score_total(offset, step) = Σ_{i=0}^{h-2} Σ |lignes[mapping[i]][j] - lignes[mapping[i+1]][j]|
```

La clé retenue est celle qui **minimise** ce score total.

## Pourquoi L1 ?

### Robustesse aux outliers

Contrairement à Euclide (L2) qui **élève au carré** les différences, L1 les somme linéairement. Conséquence :

- **Euclide pénalise fortement** une seule grosse discontinuité (un pixel aberrant fait Δ² = 10000, qui écrase tout le reste)
- **L1 traite toutes les différences de manière égale** (un pixel aberrant fait |Δ| = 100, proportionnel à 100 pixels qui diffèrent de 1)

Sur une image bruitée ou avec des artefacts de compression, L1 est plus robuste car elle n'est pas dominée par les valeurs extrêmes.

### Philosophie « image lisse »

L1 correspond à la **variation totale** (Total Variation) utilisée en traitement d'image pour le débruitage (Rudin-Osher-Fatemi, 1992). Minimiser la variation totale favorise les images « lisses par morceaux » — exactement ce qu'on attend d'une image naturelle correctement reconstituée.

### Coût de calcul réduit

L1 est la métrique la **moins coûteuse** des trois :
- **Pas de carré** (`x * x`)
- **Pas de racine carrée** (`Math.sqrt`)
- Seulement `Math.abs()` et une addition

Pour l'exploration de 32 768 clés sur plusieurs frames, cette économie est significative.

## Implémentation

```java
// L1Scoring.java:19-27
public double score(byte[] row1, byte[] row2)
{
    int sum = 0;
    for (int i = 0; i < row1.length; i++) {
        sum += Math.abs((row1[i] & 0xFF) - (row2[i] & 0xFF));
    }
    return sum;
}
```

Points notables :
- Utilisation d'un accumulateur `int` (pas `double`) : la somme de différences absolues d'octets est toujours un entier, ce qui évite les erreurs d'arrondi flottant
- `& 0xFF` : conversion des octets signés Java `[-128, 127]` en entiers non-signés `[0, 255]`
- `Math.abs()` : fonction intrinsèque JVM, souvent compilée en instruction processeur native (pas d'appel de méthode coûteux)

## Intégration dans l'attaque

L1 s'intègre dans le même moteur `NagravisionBruteForce` que les autres métriques :

```java
// DecryptionSelectionController.java:34-36
new DecryptionType("Variation totale",
    "Force brute — somme des |Δ| (norme L1), favorise les images lisses",
    new NagravisionBruteForce("Variation totale", new L1Scoring()))
```

Le pattern est identique : `NagravisionBruteForce` est paramétré par une `RowScoringFunction`, et `L1Scoring` est une implémentation de 10 lignes de cette interface.

## Optimisations partagées

L1 bénéficie des mêmes optimisations que toutes les attaques Nagravision :

| Optimisation | Description |
|---|---|
| Échantillonnage temporel | 5 frames entre 20 % et 80 % de la vidéo |
| Sous-échantillonnage spatial | 1 colonne sur 4 (`COLUMN_STRIDE = 4`) |
| Reconstruction virtuelle | Pas d'écriture disque pendant l'exploration des clés |

Ces mécanismes sont détaillés dans la documentation de [l'attaque Euclide](euclide.md).

## Résultat

```java
public record BruteForceResult(File outputFile, int offset, int step) {}
```

Le résultat contient :
- `outputFile` : le fichier vidéo déchiffré
- `offset`, `step` : la clé Nagravision retrouvée

## Comparaison avec Euclide et Pearson

| Critère | Euclide (L2) | Pearson | Variation totale (L1) |
|---|---|---|---|
| Formule | `√(Σ Δ²)` | `1 − r` | `Σ |Δ|` |
| Sensibilité aux outliers | **Forte** (Δ² écrase le score) | **Faible** (moyenne normalisée) | **Modérée** (somme linéaire) |
| Sensibilité au dégradé | Forte | Nulle | Forte |
| Coût de calcul | Élevé (carré + sqrt) | Élevé (5 accumulateurs + division + sqrt) | **Faible** (abs + addition) |
| Interprétation | Distance géométrique | Corrélation statistique | Variation totale / régularité |
| Type accumulateur | `double` | `double` | **`int`** (pas d'erreur flottante) |

## Quand L1 est-elle la meilleure ?

- Vidéos **bruitées** ou avec des **artefacts de compression** (les outliers ne dominent pas le score)
- Vidéos avec des **transitions brutales** entre plans (L1 n'est pas écrasée par les fortes discontinuités)
- Quand la **performance** est critique (pas de carré, pas de racine)
- Images avec beaucoup de **zones uniformes** (la variation totale est naturellement basse)
