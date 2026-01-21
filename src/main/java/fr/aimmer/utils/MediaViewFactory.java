package fr.aimmer.utils;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.io.File;

import static fr.aimmer.Main.WIDTH;

public class MediaViewFactory
{
	public static MediaView getMediaView(File file) {
		Media media = new Media(file.toURI().toString());
		MediaPlayer mediaPlayer = new MediaPlayer(media);
		MediaView mediaView = new MediaView(mediaPlayer);
		mediaView.preserveRatioProperty().set(true);
		mediaPlayer.play();

		return mediaView;
	}
}
