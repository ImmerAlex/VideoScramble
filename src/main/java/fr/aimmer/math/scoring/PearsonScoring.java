/**
 * VideoScramble — Score basé sur la corrélation de Pearson entre deux lignes.
 * <pre>
 *   r = Σ((xi - x̄)(yi - ȳ)) / sqrt(Σ(xi - x̄)² · Σ(yi - ȳ)²)
 * </pre>
 * Retourne {@code 1 - r} pour respecter la convention "score bas = lignes proches".
 * <p>
 * Avantage vs Euclide : insensible aux décalages de luminosité globale
 * (dégradés verticaux, vignettage…). Le coefficient est normalisé entre -1 et 1.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.math.scoring;

import fr.aimmer.math.RowScoringFunction;

public class PearsonScoring implements RowScoringFunction
{
    /**
     * Score = 1 − corrélation. 0 pour des lignes parfaitement corrélées,
     * 2 pour des lignes anti-corrélées.
     *
     * @param row1 première ligne
     * @param row2 seconde ligne
     * @return 1 − coefficient de Pearson
     */
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
     * Si une des deux lignes est constante (variance nulle), le dénominateur
     * est nul ; on retourne 0 (corrélation neutre) pour éviter NaN.
     *
     * @param row1 première ligne
     * @param row2 seconde ligne
     * @return le coefficient de Pearson, ou 0 si variance nulle
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

        // Une seule passe pour accumuler les 5 sommes
        for (int i = 0; i < n; i++) {
            int x = row1[i] & 0xFF; // conversion non signée
            int y = row2[i] & 0xFF;
            sumX  += x;
            sumY  += y;
            sumXX += (double) x * x;
            sumYY += (double) y * y;
            sumXY += (double) x * y;
        }

        double numerator = n * sumXY - sumX * sumY;
        // Math.max(0, ...) protège contre les erreurs d'arrondi flottant
        double varX = Math.max(0, n * sumXX - sumX * sumX);
        double varY = Math.max(0, n * sumYY - sumY * sumY);
        double denom = Math.sqrt(varX * varY);

        if (denom == 0) return 0; // ligne constante → corrélation neutre
        return numerator / denom;
    }
}
