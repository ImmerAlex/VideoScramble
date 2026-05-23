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
	private static final Path TEMP_DIR = Path.of(
			System.getProperty("java.io.tmpdir"), "videoscramble"
	);

	private static final String[] BUNDLED_VIDEOS = {
			"video/Pencil_Candle_1280x720.mp4",
			"video/encrypted_Pencil_Candle_1280x720.mp4",
			"video/oreo_test.mp4"
	};

	private static final Map<String, File> extractedCache = new ConcurrentHashMap<>();

	private ResourceUtils()
	{
	}

	public static File resolveVideo(String classpathResource)
	{
		File devFile = new File("src/main/resources", classpathResource);
		if (devFile.isFile())
		{
			System.out.println(LOG_PREFIX + " Vidéo trouvée sur le filesystem (dev) : " + devFile.getAbsolutePath());
			return devFile;
		}

		System.out.println(LOG_PREFIX + " Vidéo absente du filesystem, extraction depuis le classpath : " + classpathResource);
		return extractResource(classpathResource);
	}

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

	public static List<File> findLocalVideos(String classpathDir, String filesystemDir)
	{
		List<File> videos = new ArrayList<>();

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
				}
			}
		}

		return videos;
	}

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
