/**
 * VideoScramble — Distance euclidienne entre deux lignes (norme L2).
 * <pre>
 *   d(x, y) = sqrt(Σ (xi - yi)²)
 * </pre>
 * Score bas = lignes très similaires. Sensible aux décalages de luminosité.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.math.scoring;

import fr.aimmer.math.RowScoringFunction;

public class EuclideanScoring implements RowScoringFunction
{
    /**
     * Calcule la racine de la somme des carrés des différences.
     *
     * @param row1 première ligne (bytes non signés)
     * @param row2 seconde ligne
     * @return la distance euclidienne
     */
    @Override
    public double score(byte[] row1, byte[] row2)
    {
        double sum = 0;
        for (int i = 0; i < row1.length; i++) {
            // & 0xFF pour interpréter les bytes comme non signés (0-255)
            double diff = (row1[i] & 0xFF) - (row2[i] & 0xFF);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}
