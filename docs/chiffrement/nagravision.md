# Nagravision — permutation de lignes par blocs

## Principe général

Nagravision est un algorithme de chiffrement vidéo symétrique inspiré du système analogique Nagravision Syster (utilisé par Canal+ en France dans les années 90). Son principe est simple : **permuter les lignes de chaque frame vidéo** selon une règle déterministe, rendant l'image illisible sans la clé.

L'algorithme est **symétrique** : la même opération avec les mêmes paramètres chiffre puis déchiffre. Il n'y a pas de fonction inverse distincte.

## Clé

La clé est un couple `(offset, step)` avec :

| Paramètre | Domaine | Description |
|---|---|---|
| `offset` | 0 à 255 | Décalage initial dans chaque bloc |
| `step` | 0 à 127 | Incrément entre lignes consécutives |

Taille de l'espace de clés : `256 × 128 = 32 768` combinaisons possibles.

## Algorithme de permutation

L'algorithme opère en deux étapes :

### 1. Décomposition de la hauteur en blocs de puissance de 2

La hauteur de l'image est décomposée en blocs dont la taille est la plus grande puissance de 2 possible. Par exemple, pour une hauteur de 720 pixels :

- `largestPowerOfTwo(720) = 512` → premier bloc de 512 lignes
- `largestPowerOfTwo(720 - 512 = 208) = 128` → deuxième bloc de 128 lignes
- `largestPowerOfTwo(208 - 128 = 80) = 64` → troisième bloc de 64 lignes
- `largestPowerOfTwo(80 - 64 = 16) = 16` → quatrième bloc de 16 lignes

La fonction `largestPowerOfTwo(n)` retourne la plus grande puissance de 2 inférieure ou égale à `n`.

### 2. Permutation des lignes dans chaque bloc

Pour chaque bloc de `N` lignes (où `N` est une puissance de 2), la ligne source à la position `i` est placée à la position destination :

```
dst = base + (offset + (2*step + 1) × i) mod blockSize
```

Le facteur `(2*step + 1)` est forcément **impair**, ce qui garantit que la permutation est bijective : un nombre impair est toujours inversible modulo une puissance de 2.

**Propriété clé** : en arithmétique modulaire, `(2k+1)` est toujours premier avec `2^n`. Donc la multiplication par `(2*step + 1)` suivie du modulo `2^n` est une permutation bijective des entiers `[0, 2^n - 1]`. C'est mathématiquement prouvé — l'identité de Bézout garantit l'existence d'un inverse multiplicatif modulo `2^n`.

## Implémentation

### Code source

- **Classe** : `NagravisionAlgorithm` — `src/main/java/fr/aimmer/math/NagravisionAlgorithm.java`
- **Classe parente** : `AbstractFramePermutation` — `src/main/java/fr/aimmer/math/AbstractFramePermutation.java` (factorise l'ouverture/fermeture OpenCV et la boucle frame par frame)
- **Utilitaire** : `MathUtils.largestPowerOfTwo()` — `src/main/java/fr/aimmer/utils/MathUtils.java:14`

### Variation par frame

Pour éviter un pattern de brouillage statique (identique d'une frame à l'autre),
l'offset effectif varie à chaque frame :

```
effectiveOffset = (offset + frameIndex) & 0xFF
```

Ainsi, la permutation des lignes est **différente pour chaque image** de la
vidéo. Le compteur `frameIndex` est réinitialisé à 0 par `prepareForResolution`
et incrémenté à chaque appel de `transformFrame`. Le modulo 256 (`& 0xFF`)
garantit que l'offset effectif reste dans `[0, 255]`.

Le déchiffrement reproduit exactement la même séquence puisque le `frameIndex`
démarre également à 0.

### Flux d'exécution

1. `AbstractFramePermutation.process()` ouvre la vidéo source via `VideoCapture`, détecte la résolution, crée un `VideoWriter` pour la sortie
2. `prepareForResolution(width, height)` est appelée une seule fois — elle initialise `frameIndex = 0`
3. Pour chaque frame, `transformFrame()` recalcule un mapping frais avec l'offset effectif de la frame :
   - **Chiffrement** (`inverse = false`) : `applyRowPermutation()` — `dest[mapping[i]] = source[i]`
   - **Déchiffrement** (`inverse = true`) : `applyInverseRowPermutation()` — `dest[i] = source[mapping[i]]`

```java
// NagravisionAlgorithm.java:48-51
protected void prepareForResolution(int width, int height)
{
    this.frameIndex = 0;
}
```

```java
// NagravisionAlgorithm.java:54-64
protected void transformFrame(Mat source, Mat dest, boolean inverse)
{
    int effectiveOffset = (offset + frameIndex) & 0xFF;
    int[] mapping = computeRowMapping(source.rows(), effectiveOffset, step);
    if (inverse)
        applyInverseRowPermutation(source, dest, mapping);
    else
        applyRowPermutation(source, dest, mapping);
    frameIndex++;
}
```

### Calcul du mapping de lignes

```java
// NagravisionAlgorithm.java:66-90
static int[] computeRowMapping(int height, int offset, int step)
{
    int[] mapping = new int[height];
    int base = 0;
    int remaining = height;
    int destIndex = 0;

    while (remaining > 1) {
        int blockSize = largestPowerOfTwo(remaining);

        for (int i = 0; i < blockSize; i++) {
            int dst = base + ((offset + (2 * step + 1) * i) % blockSize);
            mapping[destIndex++] = dst;
        }

        base += blockSize;
        remaining -= blockSize;
    }

    if (remaining == 1) {
        mapping[destIndex] = base;
    }

    return mapping;
}
```

### Permutation directe

```java
// NagravisionAlgorithm.java:93-99
private static void applyRowPermutation(Mat source, Mat dest, int[] mapping)
{
    source.copyTo(dest);
    for (int i = 0; i < mapping.length; i++) {
        source.row(i).copyTo(dest.row(mapping[i]));
    }
}
```

### Permutation inverse (utilisée pour le déchiffrement explicite)

```java
// NagravisionAlgorithm.java:101-108
private static void applyInverseRowPermutation(Mat source, Mat dest, int[] mapping)
{
    source.copyTo(dest);
    for (int i = 0; i < mapping.length; i++) {
        source.row(mapping[i]).copyTo(dest.row(i));
    }
}
```

## Fichier de sortie

- Chiffrement : `<dossier de la vidéo d'entrée>/encrypted_<nom>.mp4`
- Déchiffrement : `<dossier de la vidéo d'entrée>/decrypted_<nom>.mp4`

## Exemple visuel

Pour `offset = 42` et `step = 13` sur une image 1280×720 :
- Chaque frame est découpée en blocs de tailles 512, 128, 64, 16
- Dans chaque bloc, les lignes sont permutées selon la formule
- L'offset effectif varie à chaque frame (`(42 + frameIndex) & 0xFF`), rendant le brouillage dynamique
- L'image chiffrée est visuellement incompréhensible (lignes mélangées)
- L'application de la permutation avec la même clé restaure l'image originale
