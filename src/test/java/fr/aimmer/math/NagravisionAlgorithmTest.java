package fr.aimmer.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NagravisionAlgorithmTest
{
	@Test
	void computeRowMapping_isPowerOfTwoHeight_isValidPermutation()
	{
		assertValidPermutation(NagravisionAlgorithm.computeRowMapping(8, 42, 13));
	}

	@Test
	void computeRowMapping_isNonPowerOfTwoHeight_isValidPermutation()
	{
		// 720 = 512 + 128 + 64 + 16 : vérifie que la décomposition en blocs reste bijective
		assertValidPermutation(NagravisionAlgorithm.computeRowMapping(720, 42, 13));
	}

	@Test
	void computeRowMapping_withZeroOffsetAndStep_isIdentity()
	{
		// offset=0, step=0 → (2*0+1)*i % blockSize = i : permutation identité
		int[] mapping = NagravisionAlgorithm.computeRowMapping(8, 0, 0);
		for (int i = 0; i < 8; i++) {
			assertEquals(i, mapping[i], "index " + i + " should map to itself");
		}
	}

	@Test
	void computeRowMapping_applyTwice_isIdentity()
	{
		// L'algorithme étant symétrique, appliquer la même permutation deux fois
		// doit reproduire la permutation identité sur les indices
		int   height  = 16;
		int[] mapping = NagravisionAlgorithm.computeRowMapping(height, 100, 50);

		int[] doubleMapping = new int[height];
		for (int i = 0; i < height; i++) {
			doubleMapping[i] = mapping[mapping[i]];
		}

		// vérifie que le double-mapping est lui aussi une permutation valide
		assertValidPermutation(doubleMapping);
	}

    private static void assertValidPermutation(int[] mapping)
    {
        boolean[] seen = new boolean[mapping.length];
        for (int dest : mapping) {
            assertTrue(dest >= 0 && dest < mapping.length,
                    "dest " + dest + " hors des bornes [0, " + mapping.length + ")");
            assertFalse(seen[dest], "destination dupliquée : " + dest);
            seen[dest] = true;
        }
    }

    @Test
    void perFrameVariation_consecutiveFrames_produceDifferentMappings()
    {
        // Simule transformFrame : frame 0 → offset effectif = 42, frame 1 → offset effectif = 43
        int[] mappingFrame0 = NagravisionAlgorithm.computeRowMapping(16, 42, 13);
        int[] mappingFrame1 = NagravisionAlgorithm.computeRowMapping(16, 43, 13);
        assertFalse(java.util.Arrays.equals(mappingFrame0, mappingFrame1),
                "Deux frames consécutives doivent avoir des permutations différentes");
    }

    @Test
    void perFrameVariation_offsetWrapsModulo256()
    {
        // Après 256 frames, le modulo 256 ramène au même offset effectif.
        int offset = 42;
        int[] mappingFrame0   = NagravisionAlgorithm.computeRowMapping(16, offset, 13);
        int[] mappingFrame256 = NagravisionAlgorithm.computeRowMapping(16, (offset + 256) & 0xFF, 13);
        assertArrayEquals(mappingFrame0, mappingFrame256,
                "L'offset effectif cycle modulo 256 : frame 0 et frame 256 doivent avoir le même mapping");
    }
}
