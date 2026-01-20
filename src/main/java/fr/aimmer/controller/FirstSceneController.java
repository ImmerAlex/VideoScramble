package fr.aimmer.controller;

import static fr.aimmer.Main.FILE;
import static fr.aimmer.Main.FILE_PATH;
import static fr.aimmer.Main.HEIGHT;
import static fr.aimmer.Main.WIDTH;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import fr.aimmer.view.GoHomeButton;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

public class FirstSceneController implements Controller
{

	@Override
	public Scene get()
	{
		MediaView mediaView = getMediaView();

		VBox root = new VBox();
		root.setSpacing(20);
		root.getChildren().addAll(
				new Label("Etape 1 : Rendu video"),
				mediaView,
				new GoHomeButton()
		);

		return new Scene(root, WIDTH, HEIGHT);
	}

	private MediaView getMediaView()
	{
		URL videoUrl = null;
		if (FILE_PATH != null){
			videoUrl = getClass().getResource(FILE_PATH);
		} else {
			try {
				videoUrl = Path.of(FILE.getAbsolutePath()).toUri().toURL();
			} catch (MalformedURLException e) {
				System.err.print(e);
				System.exit(126);
			}
		}

		if (videoUrl == null) {
			System.err.println("< " + FILE_PATH + " > not found, using default video for demo");
			videoUrl = getClass().getResource("/video/Pencil_Candle_1280x720.mp4");
		}

		Media media = new Media(videoUrl.toExternalForm());

		MediaPlayer mediaPlayer = new MediaPlayer(media);

		MediaView mediaView = new MediaView(mediaPlayer);

		mediaView.setFitWidth(WIDTH * 0.8);
		mediaView.preserveRatioProperty().set(true);
		mediaPlayer.play();

		return mediaView;
	}
}
