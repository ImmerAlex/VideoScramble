# VideoCrypt — cut-and-rotate par ligne

## Principe général

VideoCrypt est un algorithme de chiffrement vidéo inspiré du système britannique **VideoCrypt** (utilisé par BSkyB à partir de 1989). Le principe est simple et visuellement spectaculaire : **chaque ligne de l'image est coupée en deux à une position pseudo-aléatoire, puis les deux moitiés sont échangées**.

L'algorithme est **involutif** : appliquer l'opération une seconde fois avec la même graine restaure l'image originale. Il n'y a pas de fonction inverse distincte — `chiffrer(chiffrer(image)) = image`.

## Clé

Comme pour Discret 11, la clé est une **graine entière** (`seed`) dérivée de `(offset, step)` via `seed = offset × 128 + step` dans l'interface graphique. Cette graine détermine la position de coupe de chaque ligne via un PRNG.

## Algorithme cut-and-rotate

### Principe visuel

```
Ligne originale : [    A     |    B     ]
                         ↑ cut
Ligne chiffrée  : [    B     |    A     ]
```

La partie droite de la ligne originale devient la partie gauche de la ligne chiffrée, et inversement.

### 1. Calcul du point de coupe par ligne

Pour chaque ligne, un point de coupe `c` est calculé :

1. La largeur est divisée en `min(256, width)` positions discrètes (bins)
2. `positions = min(CUT_POSITIONS, max(2, width))` — 256 dans le cas historique
3. `binWidth = max(1, width / positions)` — largeur de chaque bin en pixels
4. Un bin est tiré aléatoirement dans `[1, positions-1]` (le bin 0 est exclu car `cut = 0` laisserait la ligne inchangée)
5. `cut = min(bin × binWidth, width - 1)`

La **quantification** sur 256 positions est un choix délibéré : elle borne l'espace de clés, ce qui est utile pour la démonstration (et une éventuelle attaque). Dans le VideoCrypt historique, la quantification était aussi présente (horloge de découpage échantillonnée).

### 2. Application du cut-and-rotate

Pour chaque ligne `i` avec point de coupe `c` :

```java
// dest[0, w-c)  ← source[c, w)     partie droite → début
source.row(i).colRange(c, width).copyTo(dest.row(i).colRange(0, width - c));
// dest[w-c, w)  ← source[0, c)     partie gauche → fin
source.row(i).colRange(0, c).copyTo(dest.row(i).colRange(width - c, width));
```

### 3. Propriété involutive

L'opération est sa propre inverse : appliquer la même opération deux fois restaure la ligne. Mathématiquement, si on note `R_c` la rotation au point `c` :

```
R_c([A|c | B]) = [B | A]
R_c([B|c'| A]) avec c' = largeur - c → [A | B]
```

Ceci est assuré par construction : après la première rotation, le point de coupe relatif dans la ligne chiffrée est `largeur - coupe_originale`. La seconde rotation restaure donc l'ordre initial.

Le paramètre `inverse` de `transformFrame()` est **ignoré** dans cette implémentation (voir `VideoCryptAlgorithm.java:59`).

## Implémentation

### Code source

- **Classe** : `VideoCryptAlgorithm` — `src/main/java/fr/aimmer/math/VideoCryptAlgorithm.java`
- **Classe parente** : `AbstractFramePermutation` — `src/main/java/fr/aimmer/math/AbstractFramePermutation.java`

### Flux d'exécution

1. `prepareForResolution(width, height)` est appelée une fois — `computeRowCutPoints(height, width, seed)` génère le tableau `cuts[]`
2. Pour chaque frame, `transformFrame()` appelle `applyCutAndRotate()` — le paramètre `inverse` est ignoré car l'opération est involutive

```java
// VideoCryptAlgorithm.java:50-54
protected void prepareForResolution(int width, int height)
{
    cuts = computeRowCutPoints(height, width, seed);
}
```

```java
// VideoCryptAlgorithm.java:56-61
protected void transformFrame(Mat source, Mat dest, boolean inverse)
{
    // L'opération est sa propre inverse : aucun traitement spécial en mode inverse.
    applyCutAndRotate(source, dest, cuts);
}
```

### Calcul des points de coupe

```java
// VideoCryptAlgorithm.java:76-91
static int[] computeRowCutPoints(int height, int width, int seed)
{
    int[] cuts = new int[height];
    Random rng = new Random(seed);

    int positions = Math.min(CUT_POSITIONS, Math.max(2, width));
    int binWidth = Math.max(1, width / positions);
    for (int i = 0; i < height; i++) {
        int bin = 1 + rng.nextInt(positions - 1);  // bin ∈ [1, positions-1]
        cuts[i] = Math.min(bin * binWidth, width - 1);
    }
    return cuts;
}
```

### Application du cut-and-rotate

```java
// VideoCryptAlgorithm.java:102-118
private static void applyCutAndRotate(Mat source, Mat dest, int[] cuts)
{
    source.copyTo(dest);

    int width = source.cols();
    for (int i = 0; i < cuts.length; i++) {
        int c = cuts[i];
        if (c <= 0 || c >= width) continue;

        // dest[0, w-c)  ← source[c, w)
        source.row(i).colRange(c, width).copyTo(dest.row(i).colRange(0, width - c));
        // dest[w-c, w)  ← source[0, c)
        source.row(i).colRange(0, c).copyTo(dest.row(i).colRange(width - c, width));
    }
}
```

## Fichier de sortie

- Chiffrement : `<outputDir>/generated/crypted/encrypted_vc_<nom>.mp4`
- Déchiffrement : `<outputDir>/generated/decrypted/decrypted_vc_<nom>.mp4`

## Différences avec le VideoCrypt original (BSkyB, 1989)

| Aspect | Original (1989) | Implémentation |
|---|---|---|
| Principe | Cut-and-rotate analogique sur signal vidéo | Cut-and-rotate numérique sur pixels |
| Quantification | 256 positions (horloge) | 256 positions (paramétrable via `CUT_POSITIONS`) |
| Séquence | Variable selon les frames | Fixe pour toutes les frames (même `cuts[]`) |
| Involutivité | Oui | Oui, par construction |

## Note : attaque par force brute

Comme Discret 11, VideoCrypt n'est **pas** couvert par les attaques de déchiffrement disponibles (`NagravisionBruteForce`). Celles-ci cherchent à inverser une permutation de lignes de type Nagravision, ce qui est structurellement différent du cut-and-rotate. Une vidéo chiffrée en VideoCrypt nécessiterait un attaquant dédié (exploration de l'espace des graines avec un scoring adapté au cut-and-rotate).
