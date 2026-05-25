/**
 * VideoScramble — Résultat d'une attaque par force brute sur un chiffrement Nagravision.
 * <p>
 * Porte le fichier déchiffré produit et la clé retrouvée (offset, step).
 *
 * @param outputFile le fichier vidéo déchiffré
 * @param offset     l'offset retrouvé (0–255)
 * @param step       le step retrouvé (0–127)
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.math;

import java.io.File;

public record BruteForceResult(File outputFile, int offset, int step)
{
}
