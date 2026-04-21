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
	 * Calcule la distance euclidienne entre deux matrices.
	 * <p>
	 * d(x,y) = sqrt(sum((xi - yi)^2))
	 * </p>
	 *
	 * @param ligne1 La première matrice (ligne)
	 * @param ligne2 La seconde matrice (ligne)
	 * @return La distance euclidienne entre les deux matrices
	 */
	public static double euclideanDistance(Mat ligne1, Mat ligne2) {
		double sum = 0;
		int cols = ligne1.cols();

		for (int j = 0; j < cols; j++) {
			double[] pixel1 = ligne1.get(0, j);
			double[] pixel2 = ligne2.get(0, j);

			for (int c = 0; c < pixel1.length; c++) {
				double diff = pixel1[c] - pixel2[c];
				sum += diff * diff;
			}
		}

		return Math.sqrt(sum);
	}
}
