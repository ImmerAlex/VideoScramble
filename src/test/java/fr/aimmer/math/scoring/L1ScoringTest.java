/**
 * Tests unitaires pour {@link fr.aimmer.math.scoring.L1Scoring}.
 * <p>
 * Vérifie le score (somme des |Δ|) pour des lignes identiques, avec une
 * différence connue, et le traitement correct des bytes signés en non signé.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.math.scoring;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class L1ScoringTest
{
    private final L1Scoring scoring = new L1Scoring();

    @Test
    void score_identicalRows_isZero()
    {
        byte[] a = { 10, 20, 30, 40 };
        byte[] b = { 10, 20, 30, 40 };
        assertEquals(0.0, scoring.score(a, b));
    }

    @Test
    void score_knownDifference_returnsExpectedSum()
    {
        // |0-3| + |0-4| = 7
        byte[] a = { 0, 0 };
        byte[] b = { 3, 4 };
        assertEquals(7.0, scoring.score(a, b));
    }

    @Test
    void score_handlesUnsignedBytes()
    {
        // 200 stocké en byte signé = -56 ; le scoring doit interpréter en non-signé
        // → |200 - 100| = 100 (et non |-56 - 100| = 156)
        byte[] a = { (byte) 200 };
        byte[] b = { 100 };
        assertEquals(100.0, scoring.score(a, b));
    }
}
