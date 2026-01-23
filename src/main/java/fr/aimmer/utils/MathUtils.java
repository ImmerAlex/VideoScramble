package fr.aimmer.utils;

import org.opencv.core.Mat;

public class MathUtils {
	/**
	 * Retourne la plus grande puissance de 2 inférieure à n-1
	 *
	 * @param n Entier supérieur ou égal à 1
	 *
	 * @return La plus grande puissance de 2 inférieure à n-1
	 *
	 * @throws IllegalArgumentException Si n < 1
	 */
	public static int largestPowerOfTwo(int n) {
		if (n < 1)
			throw new IllegalArgumentException("[ n ] must be >= 1");

		int p = 1;
		while (p * 2 <= n) {
			p *= 2;
		}
		return p;
	}

	/**
	 *  Retourne la distance euclidienne entre deux lignes
	 */
	public double euclideanDistance(Mat ligne1, Mat ligne2) {
		double result = 0d;

		// TODO: implemented method
		throw new UnsupportedOperationException();
	}
}
