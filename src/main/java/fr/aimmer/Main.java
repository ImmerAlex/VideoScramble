package fr.aimmer;

import nu.pattern.OpenCV;

import java.io.File;

public class Main
{
	public static int WIDTH  = 1280;
	public static int HEIGHT = 720;

	public static char MODE;
	public static File FILE;
	public static File OUTPUT_DIR;
	public static File ENCYPTED_FILE;
	public static int  OFFSET; // 0..255
	public static int  STEP; // 0..127

	public static void main(String[] args)
	{
		if (args.length == 1 && ( args[0].equals("--help") || args[0].equals("--h") )) {
			printHelp();
			return;
		}

		if (args.length < 7) error("Nombre d'arguments insuffisant.");

		try {
			Main.MODE = args[0].charAt(0);
			if (Main.MODE != 'C' && Main.MODE != 'D')
				error("Le mode doit être 'C' (chiffrement) ou 'D' (déchiffrement).");

			File file = new File(args[1]);
			if (!file.exists() || file.isDirectory()) error("Chemin de video invalide : " + args[1]);
			Main.FILE = file;

			File outDir = new File(args[2]);
			if (!outDir.exists() || !outDir.isDirectory()) error("Dossier de sortie invalide : " + args[2]);
			Main.OUTPUT_DIR = outDir;

			for (int i = 3; i < args.length; i += 2) {
				switch (args[i]) {
					case "--r" -> {
						Main.OFFSET = Integer.parseInt(args[i + 1]);
						if (Main.OFFSET < 0 || Main.OFFSET > 255)
							error("Le décalage r doit être compris entre 0 et 255.");
					}
					case "--s" -> {
						Main.STEP = Integer.parseInt(args[i + 1]);
						if (Main.STEP < 0 || Main.STEP > 127)
							error("Le pas s doit être compris entre 0 et 127.");
					}
					default -> {
						error("Option inconnue : " + args[i]);
					}
				}
			}
		} catch (NumberFormatException e) {
			error("Format de nombre invalide dans les options.");
		} catch (ArrayIndexOutOfBoundsException e) {
			error("Valeur manquante pour une option.");
		} catch (Exception e) {
			error("Erreur lors de l'analyse des arguments : " + e.getMessage());
		}

		OpenCV.loadLocally();
		App.application(args);
	}

	private static void error(String msg)
	{
		System.err.println("Erreur: " + msg);
		System.err.println("Utilisez --help pour plus d'informations.");
		System.exit(1);
	}

	private static void printHelp()
	{
		String help = """
				Chiffrement et dechiffrement de video.
				
				Usage:
				    java -jar video-scramble.jar <C|D> <input_video> <output_dir> --r <offset> --s <step>
				    java -jar video-scramble.jar --help
				
				Arguments:
				    <C|D>          Type d'opération :
				                       C = chiffrement
				                       D = déchiffrement
				
				    <input_video>  Chemin vers le fichier vidéo d'entree
				    <output_dir>   Chemin vers le dossier de sortie
				
				    --r <offset>   Decalage r (offset), codé sur 8 bits
				                   Valeur comprise entre 0 et 255
				
				    --s <step>     Pas s (step), codé sur 7 bits
				                   Valeur comprise entre 0 et 127
				
				Exemples:
				    java -jar video-scramble.jar C video.mp4 output/ --r 42 --s 13
				    java -jar video-scramble.jar D output/video.enc.mp4 output/ --r 42 --s 13
				""";

		System.out.println(help);
	}
}
