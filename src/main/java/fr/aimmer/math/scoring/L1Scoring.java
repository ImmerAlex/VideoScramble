package fr.aimmer.math.scoring;

import fr.aimmer.math.RowScoringFunction;

/**
 * Distance L1 (Manhattan / variation totale) entre deux lignes.
 * <pre>
 *   d(x, y) = Σ |xi - yi|
 * </pre>
 * Score bas = lignes très similaires.
 * <p>
 * Différence vs Euclide : on somme |Δ| au lieu de Δ². Plus robuste aux
 * outliers (pixels aberrants) et favorise les images "lisses" sans pénaliser
 * excessivement quelques fortes discontinuités. Aussi moins coûteuse
 * (pas de carré, pas de sqrt).
 */
public class L1Scoring implements RowScoringFunction
{
    @Override
    public double score(byte[] row1, byte[] row2)
    {
        int sum = 0;
        for (int i = 0; i < row1.length; i++) {
            sum += Math.abs((row1[i] & 0xFF) - (row2[i] & 0xFF));
        }
        return sum;
    }
}
