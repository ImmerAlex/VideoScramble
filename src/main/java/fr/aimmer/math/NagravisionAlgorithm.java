/**
 * VideoScramble — Chiffrement de type Nagravision : permutation des lignes par blocs de puissance de 2.
 * <p>
 * Pour chaque bloc de hauteur {@code blockSize} :
 * <pre>
 *   dst = base + (offset + (2*step+1)*i) % blockSize
 * </pre>
 * <p>
 * La permutation est identique pour toutes les frames de la vidéo.
 * <p>
 * Algorithme symétrique : la même opération avec les mêmes paramètres
 * chiffre puis déchiffre.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.math;

import org.opencv.core.Mat;

import static fr.aimmer.utils.MathUtils.largestPowerOfTwo;

public class NagravisionAlgorithm extends AbstractFramePermutation
{
    private final int offset;
    private final int step;

    /**
     * @param offset décalage r ∈ [0, 255]
     * @param step   pas s ∈ [0, 127]
     */
    public NagravisionAlgorithm(int offset, int step)
    {
        this.offset = offset;
        this.step = step;
    }

    @Override
    public String displayName()
    {
        return "Nagravision";
    }

    @Override
    protected String filePrefix(boolean inverse)
    {
        return inverse ? "decrypted_" : "encrypted_";
    }

    @Override
    protected void prepareForResolution(int width, int height)
    {
    }

    @Override
    protected void transformFrame(Mat source, Mat dest, boolean inverse)
    {
        int[] mapping = computeRowMapping(source.rows(), offset, step);
        if (inverse)
            applyInverseRowPermutation(source, dest, mapping);
        else
            applyRowPermutation(source, dest, mapping);
    }

    /**
     * Calcule le mapping des lignes pour une permutation Nagravision.
     * <p>
     * Décompose la hauteur en blocs de plus grande puissance de 2,
     * puis permute chaque bloc avec la formule de Nagravision.
     *
     * @param height hauteur de l'image en pixels
     * @param offset décalage
     * @param step   pas
     * @return tableau mapping[i] = position de la ligne i dans l'image chiffrée
     */
    // package-private : utilisé par les tests et NagravisionBruteForce
    static int[] computeRowMapping(int height, int offset, int step)
    {
        int[] mapping = new int[height];
        int base = 0;
        int remaining = height;
        int destIndex = 0;

        // On traite la hauteur par blocs de puissance de 2 décroissants
        while (remaining > 1) {
            int blockSize = largestPowerOfTwo(remaining);

            for (int i = 0; i < blockSize; i++) {
                int dst = base + ((offset + (2 * step + 1) * i) % blockSize);
                mapping[destIndex++] = dst;
            }

            base += blockSize;
            remaining -= blockSize;
        }

        // La dernière ligne (si hauteur impaire) reste à sa place
        if (remaining == 1) {
            mapping[destIndex] = base;
        }

        return mapping;
    }

    /**
     * Applique la permutation directe : dest[mapping[i]] = source[i].
     */
    private static void applyRowPermutation(Mat source, Mat dest, int[] mapping)
    {
        source.copyTo(dest);
        for (int i = 0; i < mapping.length; i++) {
            source.row(i).copyTo(dest.row(mapping[i]));
        }
    }

    /**
     * Applique la permutation inverse : dest[i] = source[mapping[i]].
     * Utilisée pour le déchiffrement.
     */
    private static void applyInverseRowPermutation(Mat source, Mat dest, int[] mapping)
    {
        source.copyTo(dest);
        for (int i = 0; i < mapping.length; i++) {
            source.row(mapping[i]).copyTo(dest.row(i));
        }
    }
}
