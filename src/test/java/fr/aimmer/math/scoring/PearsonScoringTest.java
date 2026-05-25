/**
 * Tests unitaires pour {@link fr.aimmer.math.scoring.PearsonScoring}.
 * <p>
 * Vérifie le score (1 − corrélation) pour des lignes identiques, décalées en
 * luminosité, anti-corrélées, et le cas dégénéré d'une ligne constante.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.math.scoring;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PearsonScoringTest
{
    private static final double EPSILON = 1e-9;

    private final PearsonScoring scoring = new PearsonScoring();

    @Test
    void score_identicalRows_isZero()
    {
        byte[] a = { 10, 20, 30, 40 };
        byte[] b = { 10, 20, 30, 40 };
        assertEquals(0.0, scoring.score(a, b), EPSILON);
    }

    @Test
    void score_luminanceShiftedRows_isZero()
    {
        // Avantage clé de Pearson vs Euclide/L1 : insensible à un décalage de
        // luminosité constant. Ici b = a + 40 → corrélation = 1, score = 0.
        byte[] a = { 10, 20, 30, 40 };
        byte[] b = { 50, 60, 70, 80 };
        assertEquals(0.0, scoring.score(a, b), EPSILON);
    }

    @Test
    void score_antiCorrelatedRows_isTwo()
    {
        // Lignes opposées (décroissantes vs croissantes) → r = -1, score = 2
        byte[] a = { 10, 20, 30, 40 };
        byte[] b = { 40, 30, 20, 10 };
        assertEquals(2.0, scoring.score(a, b), EPSILON);
    }

    @Test
    void score_constantRow_doesNotProduceNaN()
    {
        // Cas dégénéré : une ligne constante a une variance nulle. On doit
        // retourner un score fini (pas NaN) — la corrélation est neutralisée à 0.
        byte[] constant = { 50, 50, 50, 50 };
        byte[] varying  = { 10, 20, 30, 40 };
        double s = scoring.score(constant, varying);
        assertFalse(Double.isNaN(s), "Le score ne doit jamais être NaN");
        assertEquals(1.0, s, EPSILON); // corrélation neutralisée à 0 → score = 1
    }

    @Test
    void pearsonCorrelation_perfectPositive_isOne()
    {
        // b = 2*a → corrélation parfaite = 1
        byte[] a = { 10, 20, 30, 40 };
        byte[] b = { 20, 40, 60, 80 };
        assertEquals(1.0, PearsonScoring.pearsonCorrelation(a, b), EPSILON);
    }
}
