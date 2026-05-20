package fr.aimmer.math;

import java.io.File;

/**
 * Résultat d'une attaque par force brute sur un chiffrement de type Nagravision.
 * Porte le fichier déchiffré produit et la clé retrouvée (offset, step).
 */
public record BruteForceResult(File outputFile, int offset, int step)
{
}
