/**
 * VideoScramble — Configuration de session immuable.
 * <p>
 * Porte tous les paramètres d'une session : mode (chiffrement/déchiffrement),
 * fichier vidéo d'entrée, dossier de sortie, et la clé (offset, step).
 * Construite dans {@link fr.aimmer.Main#main} et via l'UI.
 * <p>
 * En mode GUI sans vidéo par défaut, {@code inputFile} peut être {@code null}.
 *
 * @param mode      'C' pour chiffrement, 'D' pour déchiffrement
 * @param inputFile la vidéo source (peut être {@code null} en GUI avant sélection)
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
