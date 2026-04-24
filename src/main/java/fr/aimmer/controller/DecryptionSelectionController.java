package fr.aimmer.controller;

import fr.aimmer.AppConfig;
import fr.aimmer.Main;
import fr.aimmer.ui.scene.SceneManager;
import fr.aimmer.view.GoHomeButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class DecryptionSelectionController implements Controller
{
	// Ajouter ici les futurs algorithmes de déchiffrement
	private record DecryptionType(String label, String description, String sceneId) {}

	private static final List<DecryptionType> TYPES = List.of(
			new DecryptionType("Euclide",  "Force brute — distance euclidienne entre lignes", "scene:decryption:euclide"),
			new DecryptionType("Pearson",  "Force brute — corrélation de Pearson (TODO)",     "scene:decryption:pearson")
	);

	private final AppConfig config;

	public DecryptionSelectionController(AppConfig config)
	{
		this.config = config;
	}

	@Override
	public Scene get()
	{
		VBox root = new VBox(20);
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(40));

		Label title = new Label("Méthode de déchiffrement");
		title.setFont(Font.font("System", FontWeight.BOLD, 18));
		root.getChildren().add(title);

		for (DecryptionType type : TYPES) {
			VBox card = buildCard(type);
			root.getChildren().add(card);
		}

		root.getChildren().add(new GoHomeButton());

		return new Scene(root, Main.WIDTH, Main.HEIGHT);
	}

	private VBox buildCard(DecryptionType type)
	{
		Label nameLabel = new Label(type.label());
		nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

		Label descLabel = new Label(type.description());

		Button launchButton = new Button("Sélectionner →");
		launchButton.setOnAction(_ -> SceneManager.getInstance().switchTo(type.sceneId()));

		VBox card = new VBox(6, nameLabel, descLabel, launchButton);
		card.setAlignment(Pos.CENTER_LEFT);
		card.setPadding(new Insets(12, 20, 12, 20));
		card.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 6; -fx-background-radius: 6;");
		card.setMaxWidth(500);

		return card;
	}
}
