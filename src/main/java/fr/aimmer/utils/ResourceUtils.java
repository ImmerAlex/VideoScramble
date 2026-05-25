/**
 * VideoScramble — Utilitaires de résolution de ressources (vidéos, dossiers).
 * <p>
 * Gère l'extraction des vidéos embarquées dans le JAR vers un dossier temporaire,
 * la résolution des chemins en mode développement (filesystem) et le scan des
 * vidéos locales disponibles.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourceUtils
{
	private static final String LOG_PREFIX = "[VideoScramble]";

	/** Dossier temporaire où extraire les ressources du JAR */
	private static final Path TEMP_DIR = Path.of(
			System.getProperty("java.io.tmpdir"), "videoscramble"
	);

	/** Vidéos embarquées dans le classpath (fallback si aucune vidéo locale) */
	private static final String[] BUNDLED_VIDEOS = {
			"video/Pencil_Candle_1280x720.mp4",
			"video/encrypted_Pencil_Candle_1280x720.mp4",
			"video/oreo_test.mp4"
	};

	/** Cache des ressources déjà extraites (évite les extractions multiples) */
	private static final Map<String, File> extractedCache = new ConcurrentHashMap<>();

	private ResourceUtils()
	{
		// Classe utilitaire, pas d'instanciation
	}

	/**
	 * Résout une vidéo : d'abord sur le filesystem (mode dev), puis depuis le JAR.
	 *
	 * @param classpathResource chemin relatif dans les ressources (ex: "video/foo.mp4")
	 * @return le fichier résolu (sur disque)
	 */
	public static File resolveVideo(String classpathResource)
	{
		// Mode développement : le fichier est directement sur le filesystem
		File devFile = new File("src/main/resources", classpathResource);
		if (devFile.isFile())
		{
			System.out.println(LOG_PREFIX + " Vidéo trouvée sur le filesystem (dev) : " + devFile.getAbsolutePath());
			return devFile;
		}

		// Mode JAR : on extrait la ressource vers le dossier temporaire
		System.out.println(LOG_PREFIX + " Vidéo absente du filesystem, extraction depuis le classpath : " + classpathResource);
		return extractResource(classpathResource);
	}

	/**
	 * Résout le dossier de sortie. Si le dossier préféré n'existe pas, fallback
	 * sur le dossier temporaire.
	 *
	 * @param preferred le dossier souhaité
	 * @return un dossier existant (le preferred ou le temp)
	 */
	public static File resolveOutputDir(File preferred)
	{
		if (preferred.isDirectory())
		{
			System.out.println(LOG_PREFIX + " Dossier de sortie (filesystem) : " + preferred.getAbsolutePath());
			return preferred;
		}

		TEMP_DIR.toFile().mkdirs();
		System.out.println(LOG_PREFIX + " Dossier de sortie (fallback temp) : " + TEMP_DIR.toAbsolutePath());
		return TEMP_DIR.toFile();
	}

	/**
	 * Cherche les vidéos disponibles : d'abord dans les ressources locales,
	 * puis en fallback les vidéos embarquées dans le JAR.
	 *
	 * @param classpathDir  sous-dossier dans src/main/resources (ex: "video")
	 * @param filesystemDir dossier additionnel sur le filesystem (peut être null)
	 * @return la liste des fichiers vidéo trouvés
	 */
	public static List<File> findLocalVideos(String classpathDir, String filesystemDir)
	{
		List<File> videos = new ArrayList<>();

		// Dossier de développement
		File devDir = new File("src/main/resources", classpathDir);
		if (devDir.isDirectory())
		{
			File[] found = devDir.listFiles((dir, name) -> {
				String lower = name.toLowerCase();
				return lower.endsWith(".mp4") || lower.endsWith(".avi")
						|| lower.endsWith(".mkv") || lower.endsWith(".mov")
						|| lower.endsWith(".wmv");
			});
			if (found != null)
			{
				Arrays.sort(found);
				videos.addAll(Arrays.asList(found));
			}
		}

		// Dossier additionnel (s'il est différent du dev)
		if (filesystemDir != null)
		{
			File fsDir = new File(filesystemDir);
			if (fsDir.isDirectory() && !fsDir.equals(devDir))
			{
				File[] found = fsDir.listFiles((dir, name) -> {
					String lower = name.toLowerCase();
					return lower.endsWith(".mp4") || lower.endsWith(".avi")
							|| lower.endsWith(".mkv") || lower.endsWith(".mov")
							|| lower.endsWith(".wmv");
				});
				if (found != null)
				{
					Arrays.sort(found);
					videos.addAll(Arrays.asList(found));
				}
			}
		}

		// Fallback : vidéos embarquées dans le JAR
		if (videos.isEmpty())
		{
			for (String resource : BUNDLED_VIDEOS)
			{
				try
				{
					videos.add(extractResource(resource));
				}
				catch (UncheckedIOException ignored)
				{
					// Ressource absente du classpath, on ignore
				}
			}
		}

		return videos;
	}

	/**
	 * Extrait une ressource du classpath vers le dossier temporaire.
	 * Les extractions sont cachées pour éviter les doublons.
	 *
	 * @param resourcePath chemin de la ressource dans le classpath
	 * @return le fichier extrait sur le disque
	 * @throws UncheckedIOException si l'extraction échoue
	 */
	private static File extractResource(String resourcePath)
	{
		return extractedCache.computeIfAbsent(resourcePath, path -> {
			try
			{
				TEMP_DIR.toFile().mkdirs();
				String fileName = path.substring(path.lastIndexOf('/') + 1);
				File tempFile = new File(TEMP_DIR.toFile(), fileName);

				if (!tempFile.exists())
				{
					try (InputStream in = ResourceUtils.class.getClassLoader()
							.getResourceAsStream(path))
					{
						if (in == null)
							throw new FileNotFoundException(
									"Ressource introuvable dans le classpath : " + path);
						Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						System.out.println(LOG_PREFIX + " Ressource extraite : " + path
								+ " -> " + tempFile.getAbsolutePath()
								+ " (" + tempFile.length() + " octets)");
					}
				}
				return tempFile;
			}
			catch (IOException e)
			{
				throw new UncheckedIOException(
						"Impossible d'extraire la ressource : " + path, e);
			}
		});
	}
}
