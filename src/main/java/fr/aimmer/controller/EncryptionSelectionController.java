package fr.aimmer.controller;

import fr.aimmer.AppConfig;
import fr.aimmer.Main;
import fr.aimmer.math.Discret11Algorithm;
import fr.aimmer.math.EncryptionMethod;
import fr.aimmer.math.NagravisionAlgorithm;
import fr.aimmer.math.VideoCryptAlgorithm;
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
import java.util.function.Function;

public class EncryptionSelectionController implements Controller
{
	// La graine pour les algorithmes basés sur PRNG (Discret 11, VideoCrypt) est
	// dérivée de (offset, step) → 256 × 128 = 32 768 graines distinctes, ce qui
	// aligne leur espace de clés sur celui de Nagravision pour la démo.
	private static final List<EncryptionType> TYPES = List.of(
			new EncryptionType("Nagravision",
					"Permutation des lignes par blocs de puissance de 2",
					c -> new NagravisionAlgorithm(c.offset(), c.step())),
			new EncryptionType("Discret 11",
					"Décalage horizontal pseudo-aléatoire des lignes (3 niveaux, wrap-around)",
					c -> new Discret11Algorithm(c.offset() * 128 + c.step())),
			new EncryptionType("VideoCrypt",
					"Cut-and-rotate : échange des deux moitiés à un point de coupe pseudo-aléatoire",
					c -> new VideoCryptAlgorithm(c.offset() * 128 + c.step()))
	);

	private final AppConfig config;

	public EncryptionSelectionController(AppConfig config)
	{
		this.config = config;
	}

	@Override
	public Scene get()
	{
		VBox root = new VBox(20);
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(40));

		Label title = new Label("Méthode de chiffrement");
		title.setFont(Font.font("System", FontWeight.BOLD, 18));
		root.getChildren().add(title);

		for (EncryptionType type : TYPES) {
			root.getChildren().add(buildCard(type));
		}

		root.getChildren().add(new GoHomeButton());

		return new Scene(root, Main.WIDTH, Main.HEIGHT);
	}

	private VBox buildCard(EncryptionType type)
	{
		Label nameLabel = new Label(type.label());
		nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

		Label descLabel = new Label(type.description());

		Button launchButton = new Button("Sélectionner →");
		launchButton.setOnAction(e ->
		{
			SceneManager sm = SceneManager.getInstance();
			sm.register("scene:encryption:video-selection",
					new VideoSelectionController(config, type.label(), type.algoFactory()));
			sm.switchTo("scene:encryption:video-selection");
		});

		VBox card = new VBox(6, nameLabel, descLabel, launchButton);
		card.setAlignment(Pos.CENTER_LEFT);
		card.setPadding(new Insets(12, 20, 12, 20));
		card.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 6; -fx-background-radius: 6;");
		card.setMaxWidth(600);

		return card;
	}

	// Ajouter ici les futurs algorithmes de chiffrement (1 ligne dans TYPES suffit)
	private record EncryptionType(
			String label,
			String description,
			Function<AppConfig, EncryptionMethod> algoFactory
	)
	{
	}
}
