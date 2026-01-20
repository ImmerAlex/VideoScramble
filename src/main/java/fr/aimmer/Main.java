package fr.aimmer;

import java.io.File;

import nu.pattern.OpenCV;

public class Main
{
	public static int    WIDTH;
	public static int    HEIGHT;
	public static String FILE_PATH;
	public static File   FILE;

	public static void main(String[] args)
	{
		if (args.length == 0 || args[0].equals("--help") || args[0].equals("-h")) {
			printHelp();
			System.exit(0);
		}

		if (args[0].equals("--demo")) {
			OpenCV.loadLocally();
			Main.WIDTH = 1280;
			Main.HEIGHT = 720;
			Main.FILE_PATH = "/video/Pencil_Candle_1280x720.mp4";
			App.application(args);
			return;
		}

		if (args.length != 3) {
			System.out.println("Erreur: nombre d'arguments incorrect.");
			printHelp();
			System.exit(1);
		}

		try {
			Main.WIDTH = Integer.parseInt(args[0]);
			Main.HEIGHT = Integer.parseInt(args[1]);
			Main.FILE = new File(args[2]);
		} catch (NumberFormatException e) {
			System.out.println("Erreur: width et height doivent être des entiers.");
			printHelp();
			System.exit(1);
		}

		OpenCV.loadLocally();

		App.application(args);
		return;
	}

	private static void printHelp()
	{
		System.out.println("""
		Chiffrement et déchiffrement de vidéo.

		Usage:
			java -jar video-scramble.jar <width> <height> <file>
			java -jar video-scramble.jar --demo
			java -jar video-scramble.jar --help

		Arguments:
			<width>            Largeur de la vidéo en pixels (entier)
			<height>           Hauteur de la vidéo en pixels (entier)
			<file>             Chemin vers le fichier vidéo

		Options:
			--demo             Lance le mode démonstration
			--help, -h         Affiche ce message d'aide

		Exemples:
			java -jar video-scramble.jar 1920 1080 /path/to/video.mp4
			java -jar video-scramble.jar --demo""");
	}
}
