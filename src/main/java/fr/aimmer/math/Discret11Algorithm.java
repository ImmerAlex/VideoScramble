package fr.aimmer.math;

import org.opencv.core.Mat;

import java.util.Random;

/**
 * Chiffrement inspiré du système Discret 11 (Canal+, 1984).
 * <p>
 * Principe : chaque ligne de l'image est décalée horizontalement de l'un
 * de N valeurs possibles (0, 902 ns ou 1804 ns dans l'original analogique).
 * Ici on transpose en pixels : décalage de 0, +SHIFT_UNIT ou +2*SHIFT_UNIT pixels.
 * Le choix par ligne est piloté par une séquence pseudo-aléatoire dérivée
 * d'une graine (la clé).
 * <p>
 * Algorithme symétrique : chiffrer puis déchiffrer avec la même graine restaure
 * l'image originale.
 */
public class Discret11Algorithm extends AbstractFramePermutation
{
    // Pas du décalage horizontal (en pixels). 3 niveaux possibles : 0, +UNIT, +2*UNIT.
    // TODO : ajuster cette valeur en fonction de la résolution cible.
    private static final int SHIFT_UNIT   = 4;
    private static final int SHIFT_LEVELS = 3;

    private final int seed;
    private int[] shifts;

    public Discret11Algorithm(int seed)
    {
        this.seed = seed;
    }

    @Override
    public String displayName()
    {
        return "Discret 11";
    }

    @Override
    protected String filePrefix(boolean inverse)
    {
        return inverse ? "decrypted_d11_" : "encrypted_d11_";
    }

    @Override
    protected void prepareForResolution(int width, int height)
    {
        shifts = computeRowShifts(height, seed);
    }

    @Override
    protected void transformFrame(Mat source, Mat dest, boolean inverse)
    {
        applyRowShifts(source, dest, shifts, inverse);
    }

    /**
     * Calcule, pour chaque ligne, le décalage horizontal (en pixels) à appliquer.
     * Valeurs possibles : 0, SHIFT_UNIT, 2*SHIFT_UNIT (3 niveaux comme l'original).
     * <p>
     * La séquence doit être déterministe pour une même graine.
     *
     * TODO :
     *   - Décider si la séquence est globale (1 par ligne, identique pour toutes les
     *     frames) ou variable dans le temps (1 par (frame, ligne)).
     *     L'original Discret 11 utilisait une séquence variable mais qui se répétait
     *     à chaque trame paire/impaire — à arbitrer pour la présentation.
     */
    // package-private pour les tests unitaires
    static int[] computeRowShifts(int height, int seed)
    {
        int[] shifts = new int[height];
        Random rng = new Random(seed);

        for (int i = 0; i < height; i++) {
            int level = rng.nextInt(SHIFT_LEVELS);
            shifts[i] = level * SHIFT_UNIT;
        }
        return shifts;
    }

    /**
     * Applique (ou inverse) le décalage horizontal sur chaque ligne, avec
     * wrap-around (les pixels qui sortent à droite réapparaissent à gauche,
     * et inversement). Préserve toute l'information : l'algorithme reste
     * strictement réversible.
     * <p>
     * En mode direct  : dest[i, x] = source[i, (x - shifts[i]) mod w]   (pixels glissent vers la droite)
     * En mode inverse : dest[i, x] = source[i, (x + shifts[i]) mod w]
     */
    private static void applyRowShifts(Mat source, Mat dest, int[] shifts, boolean inverse)
    {
        // copyTo : alloue dest si nécessaire et couvre les lignes à shift nul.
        source.copyTo(dest);

        int width = source.cols();
        for (int i = 0; i < shifts.length; i++) {
            int s = inverse ? -shifts[i] : shifts[i];
            // Normalise dans [0, width). cut == 0 => ligne inchangée.
            int cut = ((s % width) + width) % width;
            if (cut == 0) continue;

            // dest[0, cut)        ← source[w-cut, w)   (queue ramenée à gauche)
            source.row(i).colRange(width - cut, width).copyTo(dest.row(i).colRange(0, cut));
            // dest[cut, w)        ← source[0, w-cut)   (tête poussée à droite)
            source.row(i).colRange(0, width - cut).copyTo(dest.row(i).colRange(cut, width));
        }
    }
}
