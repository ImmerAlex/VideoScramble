/**
 * Tests unitaires pour {@link fr.aimmer.math.Discret11Algorithm}.
 * <p>
 * Vérifie le déterminisme du PRNG (même graine → même séquence), la diversité
 * (graines différentes → séquences différentes) et les niveaux de shift autorisés
 * ({0, 4, 8}).
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.math;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class Discret11AlgorithmTest
{
    @Test
    void computeRowShifts_sameSeed_isDeterministic()
    {
        int[] a = Discret11Algorithm.computeRowShifts(720, 42);
        int[] b = Discret11Algorithm.computeRowShifts(720, 42);
        assertArrayEquals(a, b);
    }

    @Test
    void computeRowShifts_differentSeeds_produceDifferentSequences()
    {
        int[] a = Discret11Algorithm.computeRowShifts(720, 42);
        int[] b = Discret11Algorithm.computeRowShifts(720, 43);
        assertFalse(java.util.Arrays.equals(a, b),
                "Deux graines différentes ne devraient pas produire la même séquence");
    }

    @Test
    void computeRowShifts_onlyProducesAuthorizedLevels()
    {
        // Le système Discret 11 utilise 3 niveaux : 0, +UNIT, +2*UNIT.
        // Avec SHIFT_UNIT=4, les valeurs autorisées sont {0, 4, 8}.
        int[] shifts = Discret11Algorithm.computeRowShifts(720, 12345);
        Set<Integer> distinct = new HashSet<>();
        for (int s : shifts) distinct.add(s);

        for (int s : distinct) {
            assertTrue(s == 0 || s == 4 || s == 8,
                    "Shift hors des niveaux autorisés {0, 4, 8} : " + s);
        }
    }

    @Test
    void computeRowShifts_distributionCoversAllLevels()
    {
        // Sur 720 lignes avec 3 niveaux équiprobables, on s'attend statistiquement
        // à voir les 3 valeurs apparaître. Test anti-régression sur le PRNG.
        int[] shifts = Discret11Algorithm.computeRowShifts(720, 12345);
        Set<Integer> distinct = new HashSet<>();
        for (int s : shifts) distinct.add(s);

        assertEquals(3, distinct.size(),
                "Les 3 niveaux de shift devraient tous être représentés sur 720 lignes");
    }
}
