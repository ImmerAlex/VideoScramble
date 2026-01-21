package fr.aimmer.controller;

import fr.aimmer.Main;
import fr.aimmer.math.VideoScramble;
import fr.aimmer.utils.MediaViewFactory;
import fr.aimmer.view.GoHomeButton;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.util.List;

import static fr.aimmer.Main.*;

public class FirstSceneController implements Controller
{

	@Override
	public Scene get()
	{
		VBox root = new VBox(20); // espacement vertical entre les éléments
		root.setAlignment(Pos.CENTER); // centre tout verticalement et horizontalement

		// Loader
		ProgressIndicator loader = new ProgressIndicator();
		root.getChildren().add(loader);

		Scene scene = new Scene(root, WIDTH, HEIGHT);

		Task<List<MediaView>> loadVideosTask = new Task<>()
		{
			@Override
			protected List<MediaView> call() throws Exception
			{
				File encryptedFile = VideoScramble.encrypt(FILE);
				MediaView originalVideo = MediaViewFactory.getMediaView(FILE);
				MediaView encryptedVideo = MediaViewFactory.getMediaView(encryptedFile);
				return List.of(originalVideo, encryptedVideo);
			}
		};

		loadVideosTask.setOnSucceeded(event -> {
			List<MediaView> videos = loadVideosTask.getValue();
			root.getChildren().clear();

			// OFFSET et STEP dans un HBox, centrés
			Label offsetLabel = new Label("OFFSET: " + Main.OFFSET);
			offsetLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
			Label stepLabel = new Label("STEP: " + Main.STEP);
			stepLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

			HBox topLabels = new HBox(20, offsetLabel, stepLabel);
			topLabels.setAlignment(Pos.CENTER); // centre horizontalement
			root.getChildren().add(topLabels);

			// HBox pour les vidéos
			HBox videoBox = new HBox(20, videos.get(0), videos.get(1));
			videoBox.setAlignment(Pos.CENTER);
			root.getChildren().add(videoBox);

			// Adapter la largeur des vidéos pour qu’elles prennent chacune la moitié
			videos.get(0).fitWidthProperty().bind(videoBox.widthProperty().subtract(videoBox.getSpacing()).divide(2));
			videos.get(1).fitWidthProperty().bind(videoBox.widthProperty().subtract(videoBox.getSpacing()).divide(2));
			videos.get(0).setPreserveRatio(true);
			videos.get(1).setPreserveRatio(true);

			// Bouton Back Home en bas, centré
			GoHomeButton goHomeButton = new GoHomeButton();
			root.getChildren().add(goHomeButton);
			VBox.setMargin(goHomeButton, new Insets(20, 0, 0, 0));
		});

		loadVideosTask.setOnFailed(event -> {
			root.getChildren().clear();
			Label errorLabel = new Label("Erreur lors du chargement des vidéos");
			root.getChildren().add(errorLabel);
		});

		new Thread(loadVideosTask).start();

		return scene;
	}
}
