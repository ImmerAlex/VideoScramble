package fr.aimmer;

import fr.aimmer.utils.ResourceUtils;
import nu.pattern.OpenCV;

import java.io.File;

public class Main
{
	public static final int WIDTH  = 1280;
	public static final int HEIGHT = 720;

	public static void main(String[] args)
	{
		AppConfig config;

		if (args.length == 0) {
			System.out.println("Aucun argument fourni. Démarrage en mode GUI avec les valeurs par défaut.");
			File inputFile = ResourceUtils.resolveVideo("video/Pencil_Candle_1280x720.mp4");
			File outputDir = ResourceUtils.resolveOutputDir(new File("src/main/resources"));
			config = new AppConfig(
					'C',
					inputFile,
					outputDir,
					42,
					13
			);
		} else {
			if (args.length == 1 && (args[0].equals("--help") || args[0].equals("-h"))) {
				printHelp();
				return;
			}

			config = parseArgs(args);
			if (config == null) return;
		}

		OpenCV.loadLocally();
		App.application(config, args);
	}

	private static AppConfig parseArgs(String[] args)
	{
		if (args.length < 3) {
			error("Nombre d'arguments insuffisant.");
			return null;
		}

		try {
			char mode = args[0].charAt(0);
			if (mode != 'C' && mode != 'D')
				error("Le mode doit être 'C' (chiffrement) ou 'D' (déchiffrement).");

			File inputFile = new File(args[1]);
			if (!inputFile.exists() || inputFile.isDirectory())
				error("Chemin de vidéo invalide : " + args[1]);

			File outputDir = new File(args[2]);
			if (!outputDir.exists() || !outputDir.isDirectory())
				error("Dossier de sortie invalide : " + args[2]);

			int offset = 42;
			int step   = 13;

			for (int i = 3; i < args.length; i += 2) {
				switch (args[i]) {
					case "--r" -> {
						offset = Integer.parseInt(args[i + 1]);
						if (offset < 0 || offset > 255)
							error("Le décalage r doit être compris entre 0 et 255.");
					}
					case "--s" -> {
						step = Integer.parseInt(args[i + 1]);
						if (step < 0 || step > 127)
							error("Le pas s doit être compris entre 0 et 127.");
					}
					default -> error("Option inconnue : " + args[i]);
				}
			}

			return new AppConfig(mode, inputFile, outputDir, offset, step);

		} catch (NumberFormatException e) {
			error("Format de nombre invalide dans les options.");
		} catch (ArrayIndexOutOfBoundsException e) {
			error("Valeur manquante pour une option.");
		} catch (Exception e) {
			error("Erreur lors de l'analyse des arguments : " + e.getMessage());
		}

		return null;
	}

	private static void error(String msg)
	{
		System.err.println("Erreur: " + msg + "\n");
		printHelp();
		System.exit(1);
	}

	private static void printHelp()
	{
		String help = """
				Chiffrement et déchiffrement de vidéo.

				Usage:
				    java -jar video-scramble.jar <C|D> <input_video> <output_dir> [--r <offset>] [--s <step>]
				    java -jar video-scramble.jar --help

				Arguments:
				    <C|D>          Type d'opération :
				                       C = chiffrement
				                       D = déchiffrement

				    <input_video>  Chemin vers le fichier vidéo d'entrée
				    <output_dir>   Chemin vers le dossier de sortie

				    --r <offset>   Décalage r (offset), codé sur 8 bits
				                   Valeur comprise entre 0 et 255 (défaut: 42)

				    --s <step>     Pas s (step), codé sur 7 bits
				                   Valeur comprise entre 0 et 127 (défaut: 13)

				Exemples:
				    java -jar video-scramble.jar C video.mp4 output/ --r 42 --s 13
				    java -jar video-scramble.jar D output/video.enc.mp4 output/ --r 42 --s 13
				""";

		System.out.println(help);
	}
}
