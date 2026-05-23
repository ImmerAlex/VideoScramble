# Discret 11 — décalage horizontal pseudo-aléatoire

## Principe général

Discret 11 est un algorithme de chiffrement vidéo inspiré du système analogique Canal+ **Discret 11** (1984). Dans l'original analogique, le signal vidéo de chaque ligne était retardé de 0, 902 ou 1804 nanosecondes, ce qui décalait horizontalement l'image d'un nombre variable de pixels.

L'implémentation numérique transpose ce principe : **chaque ligne de la frame est décalée horizontalement** d'un nombre pseudo-aléatoire de pixels, avec wrap-around (les pixels qui sortent à droite réapparaissent à gauche).

L'algorithme est **symétrique** : le déchiffrement consiste à appliquer le décalage inverse, ce qui est équivalent à un décalage de signe opposé.

## Clé

La clé est une **graine entière** (`seed`) qui alimente un générateur pseudo-aléatoire `java.util.Random`. En pratique, dans l'interface graphique, la graine est dérivée du couple `(offset, step)` via `seed = offset × 128 + step`, ce qui donne `32 768` graines possibles — aligné sur l'espace de clés de Nagravision.

## Niveaux de décalage

L'algorithme utilise **3 niveaux de décalage** calqués sur l'original analogique :

| Niveau | Décalage | Équivalent analogique |
|---|---|---|
| 0 | 0 pixel (ligne inchangée) | 0 ns |
| 1 | `SHIFT_UNIT` pixels (par défaut 4) | 902 ns |
| 2 | `2 × SHIFT_UNIT` pixels (par défaut 8) | 1804 ns |

La constante `SHIFT_UNIT` est fixée à 4 pixels dans l'implémentation actuelle (`Discret11Algorithm.java:23`).

## Algorithme de décalage

### 1. Génération de la séquence de décalages

Pour chaque ligne `i` (de `0` à `height - 1`) :

```java
int level = rng.nextInt(3);       // 0, 1, ou 2
shifts[i] = level * SHIFT_UNIT;   // 0, 4, ou 8 pixels
```

La séquence est **déterministe** pour une graine donnée, car le `Random` est initialisé avec cette graine.

### 2. Application du décalage horizontal

Pour chaque ligne `i`, on décale ses pixels vers la droite de `shifts[i]` pixels, avec **wrap-around** :

```
Mode direct (chiffrement)  : dest[i, x] = source[i, (x - shifts[i]) mod w]
Mode inverse (déchiffrement) : dest[i, x] = source[i, (x + shifts[i]) mod w]
```

En pratique, l'opération est implémentée en deux copier-coller OpenCV :
- Les `width - cut` derniers pixels de la source sont copiés au début de la destination
- Les `cut` premiers pixels de la source sont copiés à la fin de la destination

Où `cut = (shifts[i] % width + width) % width` (normalisé dans `[0, width)`).

### 3. Préservation de l'information

Le wrap-around garantit que **toute l'information est préservée** : aucun pixel n'est perdu, le décalage est strictement réversible.

## Implémentation

### Code source

- **Classe** : `Discret11Algorithm` — `src/main/java/fr/aimmer/math/Discret11Algorithm.java`
- **Classe parente** : `AbstractFramePermutation` — `src/main/java/fr/aimmer/math/AbstractFramePermutation.java`

### Flux d'exécution

1. `prepareForResolution(width, height)` est appelée une fois — `computeRowShifts(height, seed)` génère le tableau `shifts[]` contenant le décalage de chaque ligne
2. Pour chaque frame, `transformFrame()` appelle `applyRowShifts(source, dest, shifts, inverse)`

```java
// Discret11Algorithm.java:47-49
protected void prepareForResolution(int width, int height)
{
    shifts = computeRowShifts(height, seed);
}
```

### Génération de la séquence de shifts

```java
// Discret11Algorithm.java:71-81
static int[] computeRowShifts(int height, int seed)
{
    int[] shifts = new int[height];
    Random rng = new Random(seed);

    for (int i = 0; i < height; i++) {
        int level = rng.nextInt(SHIFT_LEVELS);  // 3 niveaux
        shifts[i] = level * SHIFT_UNIT;         // ×4 pixels
    }
    return shifts;
}
```

### Application des décalages par ligne

```java
// Discret11Algorithm.java:92-109
private static void applyRowShifts(Mat source, Mat dest, int[] shifts, boolean inverse)
{
    source.copyTo(dest);  // couvre les lignes à shift nul

    int width = source.cols();
    for (int i = 0; i < shifts.length; i++) {
        int s = inverse ? -shifts[i] : shifts[i];
        int cut = ((s % width) + width) % width;  // normalisé dans [0, width)
        if (cut == 0) continue;

        // Queue ramenée à gauche : dest[0, cut) ← source[w-cut, w)
        source.row(i).colRange(width - cut, width).copyTo(dest.row(i).colRange(0, cut));
        // Tête poussée à droite : dest[cut, w) ← source[0, w-cut)
        source.row(i).colRange(0, width - cut).copyTo(dest.row(i).colRange(cut, width));
    }
}
```

## Fichier de sortie

- Chiffrement : `<outputDir>/generated/crypted/encrypted_d11_<nom>.mp4`
- Déchiffrement : `<outputDir>/generated/decrypted/decrypted_d11_<nom>.mp4`

## Différences avec le Discret 11 analogique original

| Aspect | Original (1984) | Implémentation |
|---|---|---|
| Domaine | Signal analogique (temps) | Image numérique (pixels) |
| Décalage | Retard temporel en nanosecondes | Décalage spatial en pixels |
| Séquence | Variable frame paire/impaire | Identique pour toutes les frames |
| Niveaux | 3 (0, 902 ns, 1804 ns) | 3 (0, 4 px, 8 px) |

## Note : attaque par force brute

L'implémentation actuelle de l'attaque brute force (`NagravisionBruteForce`) ne couvre **pas** Discret 11. Elle parcourt uniquement l'espace de clés de Nagravision (permutation de lignes). Une vidéo chiffrée avec Discret 11 ne peut pas être restaurée par les attaques disponibles dans l'UI de déchiffrement — c'est un point pédagogique souligné dans l'interface.
