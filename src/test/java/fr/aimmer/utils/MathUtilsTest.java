package fr.aimmer.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MathUtilsTest
{
	@Test
	void largestPowerOfTwo_exactPowersOfTwo()
	{
		assertEquals(1, MathUtils.largestPowerOfTwo(1));
		assertEquals(2, MathUtils.largestPowerOfTwo(2));
		assertEquals(4, MathUtils.largestPowerOfTwo(4));
		assertEquals(8, MathUtils.largestPowerOfTwo(8));
		assertEquals(512, MathUtils.largestPowerOfTwo(512));
	}

	@Test
	void largestPowerOfTwo_nonPowersOfTwo()
	{
		assertEquals(2, MathUtils.largestPowerOfTwo(3));
		assertEquals(4, MathUtils.largestPowerOfTwo(5));
		assertEquals(4, MathUtils.largestPowerOfTwo(7));
		assertEquals(512, MathUtils.largestPowerOfTwo(720));
		assertEquals(512, MathUtils.largestPowerOfTwo(1023));
		assertEquals(1024, MathUtils.largestPowerOfTwo(1024));
	}

	@Test
	void largestPowerOfTwo_throwsOnZeroOrNegative()
	{
		assertThrows(IllegalArgumentException.class, () -> MathUtils.largestPowerOfTwo(0));
		assertThrows(IllegalArgumentException.class, () -> MathUtils.largestPowerOfTwo(-1));
		assertThrows(IllegalArgumentException.class, () -> MathUtils.largestPowerOfTwo(Integer.MIN_VALUE));
	}
}
