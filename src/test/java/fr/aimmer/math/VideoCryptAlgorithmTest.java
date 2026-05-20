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
        // Cas standard 1280×720 : tous les cuts doivent être strictement dans (0, width)
        // sinon la ligne ne subirait aucune transformation.
        int[] cuts = VideoCryptAlgorithm.computeRowCutPoints(720, 1280, 12345);
        for (int c : cuts) {
            assertTrue(c > 0 && c < 1280, "cut hors bornes : " + c);
        }
    }

    @Test
    void computeRowCutPoints_smallWidth_stillProducesValidCuts()
    {
        // Edge case : largeur < CUT_POSITIONS. On doit quand même produire des cuts
        // strictement dans (0, width).
        int width  = 80;
        int[] cuts = VideoCryptAlgorithm.computeRowCutPoints(60, width, 7);
        for (int c : cuts) {
            assertTrue(c > 0 && c < width, "cut hors bornes pour width=" + width + " : " + c);
        }
    }
}
