package fr.aimmer.controller;

import fr.aimmer.view.GoHomeButton;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.net.URL;

import static fr.aimmer.Main.*;

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
		URL videoUrl = getClass().getResource(FILE_PATH);

		if (videoUrl == null) throw new NullPointerException("< " + FILE_PATH + " > not found");

		Media media = new Media(videoUrl.toExternalForm());

		MediaPlayer mediaPlayer = new MediaPlayer(media);

		MediaView mediaView = new MediaView(mediaPlayer);

		mediaView.setFitWidth(WIDTH * 0.8);
		mediaView.preserveRatioProperty().set(true);
		mediaPlayer.play();

		return mediaView;
	}
}
