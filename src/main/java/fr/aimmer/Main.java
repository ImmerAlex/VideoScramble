/**
 * VideoScramble — Chiffrement/Déchiffrement vidéo inspiré des systèmes analogiques.
 * <p>
 * Point d'entrée principal. Charge OpenCV et lance l'application JavaFX en mode GUI.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer;

import nu.pattern.OpenCV;

import java.io.File;

public class Main
{
	/** Largeur par défaut de la fenêtre JavaFX */
	public static final int WIDTH  = 1280;
	/** Hauteur par défaut de la fenêtre JavaFX */
	public static final int HEIGHT = 720;

	private static final String LOG_PREFIX = "[VideoScramble]";

	/**
	 * Point d'entrée. Démarre l'application en mode GUI.
	 * La vidéo source est choisie par l'utilisateur dans l'interface.
	 *
	 * @param args ignorés (mode GUI uniquement)
	 */
	public static void main(String[] args)
	{
		System.out.println(LOG_PREFIX + " Démarrage en mode GUI.");

		AppConfig config = new AppConfig(
				'C',
				null,
				new File(System.getProperty("user.dir")),
				42,
				13
		);

		// Affichage de la config pour debug
		System.out.println(LOG_PREFIX + " Config : mode=" + config.mode()
				+ ", input=" + (config.inputFile() != null ? config.inputFile().getAbsolutePath() : "(aucun)")
				+ (config.inputFile() != null
					? " (exists=" + config.inputFile().exists() + ", size=" + config.inputFile().length() + ")"
					: "")
				+ ", outputDir=" + config.outputDir().getAbsolutePath()
				+ " (exists=" + config.outputDir().exists() + ")"
				+ ", offset=" + config.offset()
				+ ", step=" + config.step());

		// Chargement OpenCV : on essaie d'abord la lib système (meilleur support codec)
		try
		{
			System.loadLibrary("opencv_java");
			System.out.println(LOG_PREFIX + " OpenCV système chargé avec succès (support codec complet).");
		}
		catch (UnsatisfiedLinkError e)
		{
			System.out.println(LOG_PREFIX + " OpenCV système non disponible, fallback sur la version embarquée (support codec limité).");
			try
			{
				OpenCV.loadLocally();
				System.out.println(LOG_PREFIX + " OpenCV embarqué chargé avec succès.");
			}
			catch (Exception e2)
			{
				System.err.println(
						"Erreur: Impossible de charger OpenCV.\n"
						+ "Vérifiez que le répertoire temporaire système est accessible en écriture\n"
						+ "et n'est pas monté avec l'option 'noexec'.\n"
						+ "Détail : " + e2.getMessage()
				);
				System.exit(1);
			}
		}
		App.application(config, args);
	}
}
