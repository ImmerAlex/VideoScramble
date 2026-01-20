package fr.aimmer;

import nu.pattern.OpenCV;

public class Main
{
	public static int    WIDTH;
	public static int    HEIGHT;
	public static String FILE_PATH;

	public static void main(String[] args)
	{
		if (args.length != 3) {
			System.out.println("""
			Chiffrement et déchiffrement de vidéo.

			Usage: 
				java -jar video-scramble.jar <width> <height> file""");
			System.exit(1);
		}

		Main.WIDTH = Integer.parseInt(args[0]);
		Main.HEIGHT = Integer.parseInt(args[1]);
		Main.FILE_PATH = args[2].replaceFirst("^(?!/)", "/");

		OpenCV.loadLocally();

		App.application(args);
	}
}
