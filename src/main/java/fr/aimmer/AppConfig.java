/**
 * VideoScramble — Configuration de session immuable.
 * <p>
 * Porte tous les paramètres d'une session : mode (chiffrement/déchiffrement),
 * fichier vidéo d'entrée, dossier de sortie, et la clé (offset, step).
 * Construite par {@link fr.aimmer.Main#parseArgs} ou via l'UI.
 *
 * @param mode      'C' pour chiffrement, 'D' pour déchiffrement
 * @param inputFile la vidéo source
 * @param outputDir le dossier où écrire les fichiers générés
 * @param offset    décalage r ∈ [0, 255]
 * @param step      pas s ∈ [0, 127]
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer;

import java.io.File;

public record AppConfig(
		char mode,
		File inputFile,
		File outputDir,
		int offset,
		int step
)
{
	/**
	 * Constructeur compact : valide les bornes des paramètres.
	 */
	public AppConfig
	{
		if (mode != 'C' && mode != 'D')
			throw new IllegalArgumentException("Le mode doit être 'C' ou 'D'.");
		if (offset < 0 || offset > 255)
			throw new IllegalArgumentException("L'offset doit être compris entre 0 et 255.");
		if (step < 0 || step > 127)
			throw new IllegalArgumentException("Le step doit être compris entre 0 et 127.");
	}
}
