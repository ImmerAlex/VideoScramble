/**
 * VideoScramble — Chiffrement inspiré du système Discret 11 (Canal+, 1984).
 * <p>
 * Principe : chaque ligne de l'image est décalée horizontalement de l'un
 * de N valeurs possibles (0, 902 ns ou 1804 ns dans l'original analogique).
 * Ici on transpose en pixels : décalage de 0, +SHIFT_UNIT ou +2*SHIFT_UNIT pixels.
 * Le choix par ligne est piloté par une séquence pseudo-aléatoire dérivée
 * d'une graine (la clé).
 * <p>
 * La séquence de décalages est identique pour toutes les frames de la vidéo.
 * <p>
 * Algorithme symétrique : chiffrer puis déchiffrer avec la même graine restaure
 * l'image originale.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.math;

import org.opencv.core.Mat;

import java.util.Random;

public class Discret11Algorithm extends AbstractFramePermutation
{
    /** Pas du décalage horizontal (en pixels). 3 niveaux possibles : 0, +UNIT, +2*UNIT. */
    private static final int SHIFT_UNIT   = 40;
    private static final int SHIFT_LEVELS = 3;

    private final int seed;

    /**
     * @param seed la graine du PRNG (dérivée de offset*128+step dans l'UI)
     */
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
    }

    @Override
    protected void transformFrame(Mat source, Mat dest, boolean inverse)
    {
        int[] shifts = computeRowShifts(source.rows(), seed);
        applyRowShifts(source, dest, shifts, inverse);
    }

    /**
     * Calcule, pour chaque ligne, le décalage horizontal (en pixels) à appliquer.
     * Valeurs possibles : 0, SHIFT_UNIT, 2*SHIFT_UNIT (3 niveaux comme l'original).
     * <p>
     * La séquence est déterministe pour une même graine.
     *
     * @param height nombre de lignes
     * @param seed   la graine
     * @return tableau des décalages par ligne
     */
    // package-private pour les tests
    static int[] computeRowShifts(int height, int seed)
    {
        int[] shifts = new int[height];
        Random rng = new Random(seed);

        for (int i = 0; i < height; i++) {
            // On tire un niveau parmi {0, 1, 2} et on le multiplie par SHIFT_UNIT
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
     * En mode direct  : dest[i, x] = source[i, (x - shift) mod w]
     * En mode inverse : dest[i, x] = source[i, (x + shift) mod w]
     *
     * @param source  frame d'entrée
     * @param dest    frame de sortie
     * @param shifts  décalages par ligne
     * @param inverse {@code true} = mode déchiffrement (shift inversé)
     */
    private static void applyRowShifts(Mat source, Mat dest, int[] shifts, boolean inverse)
    {
        // copyTo : alloue dest si nécessaire et couvre les lignes à shift nul
        source.copyTo(dest);

        int width = source.cols();
        for (int i = 0; i < shifts.length; i++) {
            // Le signe du shift détermine le sens du wrap-around
            int s = inverse ? -shifts[i] : shifts[i];
            // Normalisation du cut dans [0, width)
            int cut = ((s % width) + width) % width;
            if (cut == 0) continue; // rien à faire pour cette ligne

            // dest[0, cut)        ← source[width-cut, width)   (queue ramenée à gauche)
            source.row(i).colRange(width - cut, width).copyTo(dest.row(i).colRange(0, cut));
            // dest[cut, width)    ← source[0, width-cut)       (tête poussée à droite)
            source.row(i).colRange(0, width - cut).copyTo(dest.row(i).colRange(cut, width));
        }
    }
}
