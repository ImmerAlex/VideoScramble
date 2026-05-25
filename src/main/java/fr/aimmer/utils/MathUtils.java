/**
 * VideoScramble — Utilitaires mathématiques.
 * <p>
 * Fonctions mathématiques utilisées par les algorithmes de chiffrement.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.utils;

import org.opencv.core.Mat;

public class MathUtils
{
	/**
	 * Retourne la plus grande puissance de 2 inférieure ou égale à n.
	 * <p>
	 * Utilisé par Nagravision pour découper la hauteur de l'image en blocs.
	 *
	 * @param n Entier supérieur ou égal à 1
	 * @return La plus grande puissance de 2 ≤ n
	 * @throws IllegalArgumentException Si n < 1
	 */
	public static int largestPowerOfTwo(int n)
	{
		if (n < 1)
			throw new IllegalArgumentException("[ n ] must be >= 1");

		// Méthode naïve mais suffisante : on double jusqu'à dépasser n
		int p = 1;
		while (p * 2 <= n) {
			p *= 2;
		}
		return p;
	}

	/**
	 * Calcule la distance euclidienne entre deux lignes de pixels (Mat OpenCV).
	 * <pre>
	 * d(x,y) = sqrt(Σ (xi - yi)²)
	 * </pre>
	 *
	 * @param ligne1 Première ligne (Mat 1×cols, type quelconque)
	 * @param ligne2 Seconde ligne (mêmes dimensions)
	 * @return Distance euclidienne entre les deux lignes
	 */
	public static double euclideanDistance(Mat ligne1, Mat ligne2)
	{
		double sum  = 0;
		int    cols = ligne1.cols();

		for (int j = 0; j < cols; j++) {
			double[] pixel1 = ligne1.get(0, j);
			double[] pixel2 = ligne2.get(0, j);

			// On itère sur les canaux (R, G, B)
			for (int c = 0; c < pixel1.length; c++) {
				double diff = pixel1[c] - pixel2[c];
				sum += diff * diff;
			}
		}

		return Math.sqrt(sum);
	}
}
