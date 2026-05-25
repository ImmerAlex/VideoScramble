/**
 * Tests unitaires pour {@link fr.aimmer.math.VideoCryptAlgorithm}.
 * <p>
 * Vérifie le déterminisme, la diversité des séquences, et la validité des
 * points de coupe (strictement dans (0, width)).
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VideoCryptAlgorithmTest
{
    @Test
    void computeRowCutPoints_sameSeed_isDeterministic()
    {
        int[] a = VideoCryptAlgorithm.computeRowCutPoints(720, 1280, 42);
        int[] b = VideoCryptAlgorithm.computeRowCutPoints(720, 1280, 42);
        assertArrayEquals(a, b);
    }

    @Test
    void computeRowCutPoints_differentSeeds_produceDifferentSequences()
    {
        int[] a = VideoCryptAlgorithm.computeRowCutPoints(720, 1280, 42);
        int[] b = VideoCryptAlgorithm.computeRowCutPoints(720, 1280, 43);
        assertFalse(java.util.Arrays.equals(a, b),
                "Deux graines différentes ne devraient pas produire la même séquence");
    }

    @Test
    void computeRowCutPoints_allCutsAreInsideFrame()
    {
        // Cas standard 1280×720 : tous les cuts doivent être dans (0, width)
        int[] cuts = VideoCryptAlgorithm.computeRowCutPoints(720, 1280, 12345);
        for (int c : cuts) {
            assertTrue(c > 0 && c < 1280, "cut hors bornes : " + c);
        }
    }

    @Test
    void computeRowCutPoints_smallWidth_stillProducesValidCuts()
    {
        // Cas limite : largeur < CUT_POSITIONS (256). On doit quand même produire
        // des cuts strictement dans (0, width).
        int width  = 80;
        int[] cuts = VideoCryptAlgorithm.computeRowCutPoints(60, width, 7);
        for (int c : cuts) {
            assertTrue(c > 0 && c < width, "cut hors bornes pour width=" + width + " : " + c);
        }
    }
}
