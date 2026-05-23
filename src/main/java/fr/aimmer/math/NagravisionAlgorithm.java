package fr.aimmer.math;

import org.opencv.core.Mat;

import static fr.aimmer.utils.MathUtils.largestPowerOfTwo;

/**
 * Chiffrement de type Nagravision : permutation des lignes de chaque frame
 * par blocs de puissance de 2.
 * <p>
 * Pour chaque bloc de hauteur {@code blockSize} :
 * <pre>
 *   dst = base + (offset + (2*step+1)*i) % blockSize
 * </pre>
 * <p>
 * L'offset effectif varie à chaque frame : {@code offset + frameIndex mod 256}.
 * Ceci garantit un brouillage dynamique — la permutation n'est jamais la même
 * d'une frame à l'autre.
 * <p>
 * Algorithme symétrique : la même opération avec les mêmes paramètres
 * chiffre puis déchiffre (la variation par frame est déterministe).
 */
public class NagravisionAlgorithm extends AbstractFramePermutation
{
    private final int offset;
    private final int step;
    private int frameIndex;

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
        this.frameIndex = 0;
    }

    @Override
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

    // package-private pour les tests unitaires et NagravisionBruteForce
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

    // dest[mapping[i]] = source[i]
    private static void applyRowPermutation(Mat source, Mat dest, int[] mapping)
    {
        source.copyTo(dest);
        for (int i = 0; i < mapping.length; i++) {
            source.row(i).copyTo(dest.row(mapping[i]));
        }
    }

    // dest[i] = source[mapping[i]]  — opération inverse, utilisée pour le déchiffrement
    private static void applyInverseRowPermutation(Mat source, Mat dest, int[] mapping)
    {
        source.copyTo(dest);
        for (int i = 0; i < mapping.length; i++) {
            source.row(mapping[i]).copyTo(dest.row(i));
        }
    }
}
