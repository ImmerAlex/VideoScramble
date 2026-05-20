package fr.aimmer.math.scoring;

import fr.aimmer.math.RowScoringFunction;

/**
 * Score basé sur la corrélation de Pearson entre deux lignes.
 * <pre>
 *   r = Σ((xi - x̄)(yi - ȳ)) / sqrt(Σ(xi - x̄)² · Σ(yi - ȳ)²)
 * </pre>
 * Retourne {@code 1 - r} pour respecter la convention "score bas = lignes proches".
 * <p>
 * Avantage vs Euclide : insensible aux décalages de luminosité globale
 * (dégradés verticaux, vignettage…).
 */
public class PearsonScoring implements RowScoringFunction
{
    @Override
    public double score(byte[] row1, byte[] row2)
    {
        return 1.0 - pearsonCorrelation(row1, row2);
    }

    /**
     * Coefficient de corrélation de Pearson dans [-1, 1].
     * 1 = signaux identiques (à un facteur près), 0 = décorrélés, -1 = opposés.
     * <p>
     * Implémentation en une seule passe avec 5 accumulateurs et formule fermée :
     * <pre>
     *   r = (n·Σxy − Σx·Σy) / sqrt((n·Σx² − (Σx)²)·(n·Σy² − (Σy)²))
     * </pre>
     * Si une des deux lignes est constante, le dénominateur est nul ; on
     * retourne 0 (corrélation neutre) pour éviter NaN.
     */
    static double pearsonCorrelation(byte[] row1, byte[] row2)
    {
        int n = row1.length;
        if (n == 0) return 0;

        double sumX  = 0;
        double sumY  = 0;
        double sumXX = 0;
        double sumYY = 0;
        double sumXY = 0;

        for (int i = 0; i < n; i++) {
            int x = row1[i] & 0xFF;
            int y = row2[i] & 0xFF;
            sumX  += x;
            sumY  += y;
            sumXX += (double) x * x;
            sumYY += (double) y * y;
            sumXY += (double) x * y;
        }

        double numerator = n * sumXY - sumX * sumY;
        // Math.max(0, ...) protège contre les erreurs d'arrondi flottant qui
        // pourraient rendre la variance très légèrement négative.
        double varX = Math.max(0, n * sumXX - sumX * sumX);
        double varY = Math.max(0, n * sumYY - sumY * sumY);
        double denom = Math.sqrt(varX * varY);

        if (denom == 0) return 0;
        return numerator / denom;
    }
}
