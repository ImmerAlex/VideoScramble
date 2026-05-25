/**
 * VideoScramble — Métrique de dissimilarité entre deux lignes de pixels.
 * <p>
 * Convention : <strong>plus le score est bas, plus les lignes sont proches</strong>.
 * Les implémentations dont la formule naturelle est "plus haut = mieux"
 * (ex. corrélation de Pearson) doivent retourner {@code 1 - corrélation}
 * (ou similaire) pour respecter cette convention.
 * <p>
 * Permet à {@link NagravisionBruteForce} d'être parametré par une métrique
 * sans avoir à dupliquer la boucle d'exploration de l'espace de clés.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.math;

@FunctionalInterface
public interface RowScoringFunction
{
    /**
     * Calcule un score de dissimilarité entre deux lignes.
     *
     * @param row1 première ligne de pixels (valeurs brutes, byte signé)
     * @param row2 seconde  ligne de pixels
     * @return un score positif ou nul. 0 = lignes identiques.
     */
    double score(byte[] row1, byte[] row2);
}
