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
 * Algorithme symétrique : la même opération avec les mêmes paramètres
 * chiffre puis déchiffre.
 */
public class NagravisionAlgorithm extends AbstractFramePermutation
{
    private final int offset;
    private final int step;
    private int[] rowMapping;

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
        rowMapping = computeRowMapping(height, offset, step);
    }

    @Override
    protected void transformFrame(Mat source, Mat dest, boolean inverse)
    {
        if (inverse)
            applyInverseRowPermutation(source, dest, rowMapping);
        else
            applyRowPermutation(source, dest, rowMapping);
    }

    // package-private pour les tests unitaires
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
