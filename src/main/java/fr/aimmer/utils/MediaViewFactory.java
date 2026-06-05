/**
 * VideoScramble — Fabrique de {@link javafx.scene.media.MediaView} pour lecture vidéo.
 * <p>
 * Crée un MediaView à partir d'un fichier, avec gestion basique des erreurs
 * et logs de debug.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.utils;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.io.File;

public class MediaViewFactory
{
	/**
	 * Crée un {@link MediaView} prêt à être affiché, lecture lancée.
	 *
	 * @param file le fichier vidéo à lire
	 * @return le MediaView configuré
	 */
	public static MediaView getMediaView(File file)
	{
		System.out.println("[VideoScramble] MediaView : " + file.getAbsolutePath()
				+ " (exists=" + file.exists() + ", size=" + file.length() + ")");

		Media       media       = new Media(file.toURI().toString());
		MediaPlayer mediaPlayer = new MediaPlayer(media);
		mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);

		// Log des problèmes éventuels
		mediaPlayer.setOnError(() ->
				System.err.println("[VideoScramble] MediaPlayer erreur : "
						+ mediaPlayer.getError() + " — " + file.getName()));

		mediaPlayer.setOnReady(() ->
				System.out.println("[VideoScramble] MediaPlayer prêt : " + file.getName()));

		mediaPlayer.setOnStalled(() ->
				System.err.println("[VideoScramble] MediaPlayer stalled : " + file.getName()));

		MediaView   mediaView   = new MediaView(mediaPlayer);
		mediaView.preserveRatioProperty().set(true);
		mediaPlayer.play(); // lecture automatique

		return mediaView;
	}
}
