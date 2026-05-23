package fr.aimmer.controller;

import fr.aimmer.AppConfig;
import fr.aimmer.Main;
import fr.aimmer.math.EncryptionMethod;
import fr.aimmer.ui.scene.SceneManager;
import fr.aimmer.utils.ResourceUtils;
import fr.aimmer.view.GoHomeButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.function.Function;

public class VideoSelectionController implements Controller
{
	private final AppConfig config;
	private final String algoLabel;
	private final Function<AppConfig, EncryptionMethod> algoFactory;

	public VideoSelectionController(AppConfig config, String algoLabel,
	                                Function<AppConfig, EncryptionMethod> algoFactory)
	{
		this.config = config;
		this.algoLabel = algoLabel;
		this.algoFactory = algoFactory;
	}

	@Override
	public Scene get()
	{
		VBox root = new VBox(20);
		root.setAlignment(Pos.TOP_CENTER);
		root.setPadding(new Insets(30, 60, 30, 60));

		Label title = new Label("Chiffrement : " + algoLabel);
		title.setFont(Font.font("System", FontWeight.BOLD, 16));

		// --- Sélection via explorateur de fichiers ---
		Label browseTitle = new Label("Fichier sélectionné :");
		browseTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

		Label selectedFileLabel = new Label(config.inputFile().getName());

		Button browseButton = new Button("Parcourir…");

		HBox browseBox = new HBox(12, browseButton, selectedFileLabel);
		browseBox.setAlignment(Pos.CENTER_LEFT);

		// --- Liste des vidéos disponibles localement ---
		Label listTitle = new Label("Vidéos disponibles dans le projet :");
		listTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

		ListView<File> videoList = new ListView<>();
		videoList.setPrefHeight(260);
		videoList.getItems().addAll(findLocalVideos());
		videoList.setCellFactory(_ -> new ListCell<>()
		{
			@Override
			protected void updateItem(File item, boolean empty)
			{
				super.updateItem(item, empty);
				setText(empty || item == null ? null : item.getPath());
			}
		});

		// Fichier retenu (tableau à un élément pour mutation dans les lambdas)
		File[] selectedFile = { config.inputFile() };

		// --- Bouton de lancement ---
		Button launchButton = new Button("Chiffrer →");
		launchButton.setFont(Font.font("System", FontWeight.BOLD, 14));
		launchButton.setPadding(new Insets(10, 40, 10, 40));

		// Parcourir : ouvre un FileChooser
		browseButton.setOnAction(_ ->
		{
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Sélectionner une vidéo");
			fileChooser.getExtensionFilters().add(
					new FileChooser.ExtensionFilter("Fichiers vidéo", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.wmv")
			);
			Stage stage = (Stage) browseButton.getScene().getWindow();
			File file = fileChooser.showOpenDialog(stage);
			if (file != null) {
				selectedFile[0] = file;
				selectedFileLabel.setText(file.getName());
				videoList.getSelectionModel().clearSelection();
			}
		});

		// Clic dans la liste
		videoList.getSelectionModel().selectedItemProperty().addListener((_, _, newVal) ->
		{
			if (newVal != null) {
				selectedFile[0] = newVal;
				selectedFileLabel.setText(newVal.getName());
			}
		});

		// Lancement du chiffrement avec le fichier retenu et l'algo sélectionné
		launchButton.setOnAction(_ ->
		{
			AppConfig newConfig = new AppConfig(
					'C',
					selectedFile[0],
					config.outputDir(),
					config.offset(),
					config.step()
			);
			EncryptionMethod algo = algoFactory.apply(newConfig);
			SceneManager sm = SceneManager.getInstance();
			sm.register("scene:encryption", new EncryptionSceneController(newConfig, algo));
			sm.switchTo("scene:encryption");
		});

		root.getChildren().addAll(title, browseTitle, browseBox, listTitle, videoList, launchButton, new GoHomeButton());

		return new Scene(root, Main.WIDTH, Main.HEIGHT);
	}

	private List<File> findLocalVideos()
	{
		return ResourceUtils.findLocalVideos("video", null);
	}
}
