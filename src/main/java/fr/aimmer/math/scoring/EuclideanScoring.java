package fr.aimmer.math.scoring;

import fr.aimmer.math.RowScoringFunction;

/**
 * Distance euclidienne entre deux lignes (norme L2).
 * <pre>
 *   d(x, y) = sqrt(Σ (xi - yi)²)
 * </pre>
 * Score bas = lignes très similaires.
 */
public class EuclideanScoring implements RowScoringFunction
{
    @Override
    public double score(byte[] row1, byte[] row2)
    {
        double sum = 0;
        for (int i = 0; i < row1.length; i++) {
            double diff = (row1[i] & 0xFF) - (row2[i] & 0xFF);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}
